package com.iispl.threading;

import com.iispl.entity.IncomingTransaction;
import com.iispl.entity.SettlementResult;
import com.iispl.enums.ProcessingStatus;
import com.iispl.service.BatchService;
import com.iispl.service.SettlementService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * CONSUMER
 *
 * Drains the shared queue into a local accumulator list.
 * When producers are done (running=false) and the queue is empty,
 * it groups accumulated transactions by (settlementDate + channel)
 * and fires processBatch() for each group.
 *
 * This gives true batch semantics: netting and file export operate
 * on the full set of transactions per date/channel, not one-by-one.
 */
public class SettlementProcessor implements Runnable {

    private static final int POLL_TIMEOUT_SECONDS = 3;

    private final BatchService      batchService;
    private final SettlementService settlementService;
    private final BlockingQueue<IncomingTransaction> queue;
    private final AtomicBoolean running;

    public SettlementProcessor(BatchService batchService,
                               SettlementService settlementService,
                               BlockingQueue<IncomingTransaction> queue,
                               AtomicBoolean running) {
        this.batchService      = batchService;
        this.settlementService = settlementService;
        this.queue             = queue;
        this.running           = running;
    }

    @Override
    public void run() {
        String threadName = Thread.currentThread().getName();
        System.out.println("[" + threadName + "] SettlementProcessor started.");

        List<IncomingTransaction> accumulated = new ArrayList<>();

        // ---- DRAIN PHASE: collect all transactions from the queue ----
        while (running.get() || !queue.isEmpty()) {
            try {
                IncomingTransaction txn =
                        queue.poll(POLL_TIMEOUT_SECONDS, TimeUnit.SECONDS);

                if (txn == null) continue;  // timeout — re-check running flag

                txn.setProcessingStatus(ProcessingStatus.QUEUED);
                accumulated.add(txn);

                System.out.println("[" + threadName + "] Accumulated txn id: "
                        + txn.getId()
                        + " | source: " + txn.getSourceSystem().getSystemCode()
                        + " | total so far: " + accumulated.size());

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("[" + threadName + "] Interrupted during drain.");
                break;
            }
        }

        if (accumulated.isEmpty()) {
            System.out.println("[" + threadName + "] No transactions to process. Exiting.");
            return;
        }

        System.out.println("[" + threadName + "] Drain complete. "
                + accumulated.size() + " transactions accumulated. "
                + "Grouping by date + channel...");

        // ---- GROUP PHASE: date + channel → sub-lists ----
        Map<String, List<IncomingTransaction>> batches =
                batchService.groupByDateAndChannel(accumulated);

        // ---- PROCESS PHASE: one batch per group ----
        for (Map.Entry<String, List<IncomingTransaction>> entry : batches.entrySet()) {
            String batchKey = entry.getKey();
            List<IncomingTransaction> batchTxns = entry.getValue();

            System.out.println("[" + threadName + "] Processing batch key: "
                    + batchKey + " | txns: " + batchTxns.size());

            try {
                SettlementResult result =
                        batchService.processBatch(batchKey, batchTxns);

                System.out.println("[" + threadName + "] Batch complete: "
                        + result.getBatchId()
                        + " | status: "   + result.getBatchStatus()
                        + " | settled: "  + result.getSettledCount()
                        + " | failed: "   + result.getFailedCount()
                        + " | net: "      + result.getNetAmount()
                        + " | file: "     + result.getExportedFilePath());

            } catch (Exception e) {
                System.err.println("[" + threadName + "] Batch " + batchKey
                        + " threw exception: " + e.getMessage());
            }
        }

        System.out.println("[" + threadName + "] SettlementProcessor shut down cleanly. "
                + "Processed " + batches.size() + " batches.");
    }
}