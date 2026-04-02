package com.iispl.threading;

import com.iispl.entity.IncomingTransaction;
import com.iispl.service.BatchService;
import com.iispl.service.SettlementService;

import java.util.*;
import java.util.concurrent.BlockingQueue;

public class BatchScheduler implements Runnable {

    private final BlockingQueue<IncomingTransaction> queue;
    private final BatchService batchService;
    private final SettlementService settlementService;

    // Batch window (simulate UPI cycles)
    private static final int BATCH_INTERVAL_SECONDS = 30;

    public BatchScheduler(BlockingQueue<IncomingTransaction> queue,
                          BatchService batchService,
                          SettlementService settlementService) {
        this.queue = queue;
        this.batchService = batchService;
        this.settlementService = settlementService;
    }

    @Override
    public void run() {
        System.out.println("[Scheduler] Batch scheduler started...");

        while (true) {
            try {
                Thread.sleep(BATCH_INTERVAL_SECONDS * 1000L);

                List<IncomingTransaction> batch = new ArrayList<>();

                // Drain queue snapshot (important 🔥)
                queue.drainTo(batch);

                if (batch.isEmpty()) {
                    System.out.println("[Scheduler] No transactions in this cycle.");
                    continue;
                }

                System.out.println("[Scheduler] Processing batch of size: " + batch.size());

                // Group into batches (date + channel)
                Map<String, List<IncomingTransaction>> grouped =
                        batchService.groupByDateAndChannel(batch);

                // Process each batch
                for (Map.Entry<String, List<IncomingTransaction>> entry : grouped.entrySet()) {
                    batchService.processBatch(entry.getKey(), entry.getValue());
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("[Scheduler] Stopped.");
                break;
            } catch (Exception e) {
                System.err.println("[Scheduler] Error: " + e.getMessage());
            }
        }
    }
}