package com.iispl.threading;

import com.iispl.entity.IncomingTransaction;
import com.iispl.entity.SettlementResult;
import com.iispl.enums.ProcessingStatus;
import com.iispl.service.SettlementService;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * CONSUMER
 *
 * Multiple SettlementProcessors run in parallel, each polling the
 * shared BlockingQueue and processing whatever IncomingTransaction
 * it picks up next.
 *
 * Thread lifecycle:
 *   Runs in a loop until PipelineOrchestrator signals shutdown
 *   via the shared 'running' flag, AND the queue is empty.
 *   Uses poll() with a timeout (not take()) so it can check the
 *   shutdown flag regularly instead of blocking forever.
 */
public class SettlementProcessor implements Runnable {

    private static final int POLL_TIMEOUT_SECONDS = 3;

    private final SettlementService settlementService;
    private final BlockingQueue<IncomingTransaction> queue;
    private final AtomicBoolean running;   // shared flag — set to false by orchestrator

    public SettlementProcessor(SettlementService settlementService,
                               BlockingQueue<IncomingTransaction> queue,
                               AtomicBoolean running) {
        this.settlementService = settlementService;
        this.queue             = queue;
        this.running           = running;
    }

    @Override
    public void run() {
        String threadName = Thread.currentThread().getName();
        System.out.println("[" + threadName + "] SettlementProcessor started.");

        while (running.get() || !queue.isEmpty()) {
            try {
                // poll with timeout — avoids blocking forever when queue drains
                IncomingTransaction txn = queue.poll(POLL_TIMEOUT_SECONDS, TimeUnit.SECONDS);

                if (txn == null) {
                    // timed out — loop back and re-check running flag
                    continue;
                }

                txn.setProcessingStatus(ProcessingStatus.PROCESSING);
                System.out.println("[" + threadName + "] Processing txn id: " + txn.getId()
                        + " | source: " + txn.getSourceSystem().getSystemCode());

                SettlementResult result = settlementService.process(txn);

                txn.setProcessingStatus(ProcessingStatus.PROCESSED);
                System.out.println("[" + threadName + "] Settled txn id: " + txn.getId()
                        + " | batch: " + result.getBatchId()
                        + " | status: " + result.getBatchStatus());

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();   // restore interrupt flag
                System.err.println("[" + threadName + "] SettlementProcessor interrupted.");
                break;
            } catch (Exception e) {
                System.err.println("[" + threadName + "] Failed to process txn: " + e.getMessage());
                // log and continue — one bad txn should not kill the consumer thread
            }
        }

        System.out.println("[" + threadName + "] SettlementProcessor shut down cleanly.");
    }
}