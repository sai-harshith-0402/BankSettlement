package com.iispl.threading;

import com.iispl.dao.BatchDao;
import com.iispl.dao.CreditTransactionDao;
import com.iispl.dao.DebitTransactionDao;
import com.iispl.dao.InterBankTransactionDao;
import com.iispl.dao.ReversalTransactionDao;
import com.iispl.dao.TransactionDao;
import com.iispl.entity.Batch;
import com.iispl.entity.CreditTransaction;
import com.iispl.entity.DebitTransaction;
import com.iispl.entity.IncomingTransaction;
import com.iispl.entity.InterBankTransaction;
import com.iispl.entity.ReversalTransaction;
import com.iispl.enums.BatchStatus;
import com.iispl.enums.ChannelType;

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
 * Saves each transaction to the correct subtype table via DAO.
 * Accumulates them in channel buckets (Map<ChannelType, List<IncomingTransaction>>).
 * On poison pill → flush buckets → create Batches → save each Batch to DB.
 * When all N poison pills received → push POISON_BATCH → shut down.
 *
 * ReentrantLock guards channelBuckets during accumulate and flush
 * to ensure thread-safe critical section discipline.
 */
public class BatchProcessor implements Runnable {

    private static final Logger logger = Logger.getLogger(BatchProcessor.class.getName());

    private final LinkedBlockingQueue<IncomingTransaction> transactionQueue;
    private final LinkedBlockingQueue<Batch>               batchQueue;
    private final int                                      totalSourceCount;

    // DAOs for persistence
    private final BatchDao                 batchDao;
    private final TransactionDao           transactionDao;
    private final CreditTransactionDao     creditTransactionDao;
    private final DebitTransactionDao      debitTransactionDao;
    private final ReversalTransactionDao   reversalTransactionDao;
    private final InterBankTransactionDao  interBankTransactionDao;

    // ReentrantLock guards channelBuckets map
    private final ReentrantLock                                        bucketLock     = new ReentrantLock();
    private final Map<ChannelType, List<IncomingTransaction>>          channelBuckets = new HashMap<>();

    private int poisonPillsReceived = 0;

    public BatchProcessor(
            LinkedBlockingQueue<IncomingTransaction> transactionQueue,
            LinkedBlockingQueue<Batch>               batchQueue,
            int                                      totalSourceCount,
            BatchDao                                 batchDao,
            TransactionDao                           transactionDao,
            CreditTransactionDao                     creditTransactionDao,
            DebitTransactionDao                      debitTransactionDao,
            ReversalTransactionDao                   reversalTransactionDao,
            InterBankTransactionDao                  interBankTransactionDao) {

        this.transactionQueue        = transactionQueue;
        this.batchQueue              = batchQueue;
        this.totalSourceCount        = totalSourceCount;
        this.batchDao                = batchDao;
        this.transactionDao          = transactionDao;
        this.creditTransactionDao    = creditTransactionDao;
        this.debitTransactionDao     = debitTransactionDao;
        this.reversalTransactionDao  = reversalTransactionDao;
        this.interBankTransactionDao = interBankTransactionDao;
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
                IncomingTransaction txn = transactionQueue.take();

                // ── Poison pill received ───────────────────────────────────────
                if (txn == PipelineOrchestrator.POISON_TRANSACTION) {
                    poisonPillsReceived++;
                    logger.info("[BatchProcessor:" + threadName + "] Poison pill ("
                            + poisonPillsReceived + "/" + totalSourceCount + ")");

                    // One source file fully ingested — flush into Batches
                    flushBuckets(threadName);

                    if (poisonPillsReceived == totalSourceCount) {
                        logger.info("[BatchProcessor:" + threadName
                                + "] All sources done — pushing POISON_BATCH.");
                        batchQueue.put(PipelineOrchestrator.POISON_BATCH);
                        break;
                    }
                    continue;
                }

                // ── Save transaction to DB ─────────────────────────────────────
                saveTransaction(txn, threadName);

                // ── Accumulate into channel bucket ─────────────────────────────
                accumulateTransaction(txn);
            }

        } catch (InterruptedException e) {
            logger.severe("[BatchProcessor:" + threadName + "] Interrupted: " + e.getMessage());
            Thread.currentThread().interrupt();
        }

        logger.info("[BatchProcessor:" + threadName + "] Shut down cleanly.");
    }

    // =========================================================================
    // SAVE TRANSACTION TO DB
    // Routes to the correct subtype DAO based on instanceof check.
    // Also saves to the base incoming_transaction table via TransactionDao.
    // =========================================================================

    private void saveTransaction(IncomingTransaction txn, String threadName) {
        try {
            // Save to base table first
            transactionDao.save(txn);

            // Save to subtype table
            if (txn instanceof CreditTransaction) {
                creditTransactionDao.save((CreditTransaction) txn);
                logger.info("[BatchProcessor:" + threadName + "] Saved CreditTransaction id="
                        + txn.getIncomingTnxId());

            } else if (txn instanceof DebitTransaction) {
                debitTransactionDao.save((DebitTransaction) txn);
                logger.info("[BatchProcessor:" + threadName + "] Saved DebitTransaction id="
                        + txn.getIncomingTnxId());

            } else if (txn instanceof ReversalTransaction) {
                reversalTransactionDao.save((ReversalTransaction) txn);
                logger.info("[BatchProcessor:" + threadName + "] Saved ReversalTransaction id="
                        + txn.getIncomingTnxId());

            } else if (txn instanceof InterBankTransaction) {
                interBankTransactionDao.save((InterBankTransaction) txn);
                logger.info("[BatchProcessor:" + threadName + "] Saved InterBankTransaction id="
                        + txn.getIncomingTnxId());

            } else {
                logger.warning("[BatchProcessor:" + threadName + "] Unknown transaction subtype for id="
                        + txn.getIncomingTnxId() + " — saved to base table only.");
            }

        } catch (Exception e) {
            logger.severe("[BatchProcessor:" + threadName + "] Failed to save transaction id="
                    + txn.getIncomingTnxId() + ": " + e.getMessage());
        }
    }

    // =========================================================================
    // ACCUMULATE TRANSACTION
    // Guarded by ReentrantLock.
    // =========================================================================

    private void accumulateTransaction(IncomingTransaction txn) {
        bucketLock.lock();
        try {
            channelBuckets
                    .computeIfAbsent(txn.getChannelType(), k -> new ArrayList<>())
                    .add(txn);
        } finally {
            bucketLock.unlock();
        }
    }

    // =========================================================================
    // FLUSH BUCKETS
    // Creates one Batch per channel, saves each to DB, pushes to batchQueue.
    // Guarded by ReentrantLock.
    // =========================================================================

    private void flushBuckets(String threadName) throws InterruptedException {
        bucketLock.lock();
        try {
            if (channelBuckets.isEmpty()) {
                logger.info("[BatchProcessor:" + threadName + "] No transactions to flush.");
                return;
            }

            LocalDate today = LocalDate.now();

            for (Map.Entry<ChannelType, List<IncomingTransaction>> entry
                    : channelBuckets.entrySet()) {

                ChannelType channel = entry.getKey();
                List<IncomingTransaction> txns = new ArrayList<>(entry.getValue());

                if (txns.isEmpty()) continue;

                String sourceTypeName = txns.get(0).getSourceSystem() != null
                        ? txns.get(0).getSourceSystem().getSourceType().name()
                        : "UNKNOWN";

                String batchId = sourceTypeName + "-" + channel.name() + "-" + today;

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

                // ── Persist Batch to DB ────────────────────────────────────────
                try {
                    batchDao.saveBatch(batch);
                    logger.info("[BatchProcessor:" + threadName + "] Batch saved to DB: " + batchId);
                } catch (Exception e) {
                    logger.severe("[BatchProcessor:" + threadName
                            + "] Failed to save batch to DB [" + batchId + "]: " + e.getMessage());
                }

                // ── Update batchId on each transaction in DB ───────────────────
                for (IncomingTransaction txn : txns) {
                    try {
                        transactionDao.updateBatchId(txn.getIncomingTnxId(), batchId);
                    } catch (Exception e) {
                        logger.warning("[BatchProcessor:" + threadName
                                + "] Failed to update batchId for txn id="
                                + txn.getIncomingTnxId() + ": " + e.getMessage());
                    }
                }

                // ── Push batch to batchQueue for SettlementProcessor ───────────
                batchQueue.put(batch);

                logger.info("[BatchProcessor:" + threadName + "] Batch flushed → batchQueue"
                        + " | BatchId: " + batchId
                        + " | Channel: " + channel
                        + " | Transactions: " + txns.size()
                        + " | TotalAmount: " + totalAmount);
            }

            channelBuckets.clear();

        } finally {
            bucketLock.unlock();
        }
    }
}