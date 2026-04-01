package com.iispl.threading;

import com.iispl.entity.IncomingTransaction;
import com.iispl.entity.SourceSystem;
import com.iispl.service.BatchService;
import com.iispl.service.SettlementService;

import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * PipelineOrchestrator
 *
 * Wires the full producer-consumer pipeline:
 *   Producers: one IngestionWorker per active SourceSystem
 *   Consumers: N SettlementProcessors that drain the queue,
 *              group by date+channel, and settle each batch
 */
public class PipelineOrchestrator {

    private static final int QUEUE_CAPACITY        = 500;
    private static final int CONSUMER_THREAD_COUNT = 4;
    private static final int SHUTDOWN_TIMEOUT_SEC  = 60;

    private final BatchService      batchService;
    private final SettlementService settlementService;

    public PipelineOrchestrator(BatchService batchService,
                                SettlementService settlementService) {
        this.batchService      = batchService;
        this.settlementService = settlementService;
    }

    public void startPipeline(List<SourceSystem> activeSources) {

        System.out.println("[Orchestrator] Starting pipeline for "
                + activeSources.size() + " source(s).");

        // 1. Shared bounded queue — back-pressure on producers
        BlockingQueue<IncomingTransaction> queue =
                new ArrayBlockingQueue<>(QUEUE_CAPACITY);

        // 2. Shutdown signal — consumers watch this
        AtomicBoolean producersRunning = new AtomicBoolean(true);

        // 3. Producer pool — one thread per source
        ExecutorService producerPool =
                Executors.newFixedThreadPool(activeSources.size());

        // 4. Consumer pool — fixed thread count
        ExecutorService consumerPool =
                Executors.newFixedThreadPool(CONSUMER_THREAD_COUNT);

        // Submit consumers BEFORE producers — ready to receive immediately
        for (int i = 0; i < CONSUMER_THREAD_COUNT; i++) {
            consumerPool.submit(new SettlementProcessor(
                    batchService, settlementService, queue, producersRunning));
        }

        // Submit one producer per active source
        for (SourceSystem source : activeSources) {
            producerPool.submit(
                    new IngestionWorker(source, batchService, queue));
        }

        // 5. Wait for all producers to finish
        producerPool.shutdown();
        try {
            boolean done = producerPool.awaitTermination(
                    SHUTDOWN_TIMEOUT_SEC, TimeUnit.SECONDS);
            if (!done) {
                System.err.println("[Orchestrator] Producers timed out.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("[Orchestrator] Interrupted waiting for producers.");
        }

        // 6. Signal consumers — no more items coming
        producersRunning.set(false);
        System.out.println("[Orchestrator] All producers done. Consumers draining queue...");

        // 7. Wait for consumers to process all batches
        consumerPool.shutdown();
        try {
            boolean done = consumerPool.awaitTermination(
                    SHUTDOWN_TIMEOUT_SEC, TimeUnit.SECONDS);
            if (done) {
                System.out.println("[Orchestrator] Pipeline complete. "
                        + "All batches settled and files written.");
            } else {
                System.err.println("[Orchestrator] Consumers timed out — forcing shutdown.");
                consumerPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            consumerPool.shutdownNow();
        }
    }
}