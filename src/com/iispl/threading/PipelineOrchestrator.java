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
 * Wires the entire producer-consumer pipeline together.
 *
 * Responsibilities:
 *   1. Create the shared BlockingQueue
 *   2. Submit one IngestionWorker (producer) per active SourceSystem
 *   3. Submit N SettlementProcessors (consumers) on a separate thread pool
 *   4. Wait for all producers to finish, then signal consumers to shut down
 *   5. Await clean consumer shutdown with a timeout
 *
 * Designed to be called once at application startup from main().
 */
public class PipelineOrchestrator {

    private static final int QUEUE_CAPACITY        = 500;
    private static final int CONSUMER_THREAD_COUNT = 4;
    private static final int SHUTDOWN_TIMEOUT_SEC  = 30;

    private final BatchService      batchService;
    private final SettlementService settlementService;

    public PipelineOrchestrator(BatchService batchService,
                                SettlementService settlementService) {
        this.batchService      = batchService;
        this.settlementService = settlementService;
    }

    public void startPipeline(List<SourceSystem> activeSources) {

        // 1. Shared queue — bounded, gives back-pressure on producers
        BlockingQueue<IncomingTransaction> queue =
                new ArrayBlockingQueue<>(QUEUE_CAPACITY);

        // 2. Shared flag — consumers watch this to know when to stop
        AtomicBoolean producersRunning = new AtomicBoolean(true);

        // 3. Producer pool — one thread per source system
        ExecutorService producerPool =
                Executors.newFixedThreadPool(activeSources.size());

        // 4. Consumer pool — fixed 4 threads
        ExecutorService consumerPool =
                Executors.newFixedThreadPool(CONSUMER_THREAD_COUNT);

        // Submit consumers FIRST — ready before producers enqueue anything
        for (int i = 0; i < CONSUMER_THREAD_COUNT; i++) {
            consumerPool.submit(
                    new SettlementProcessor(settlementService, queue, producersRunning));
        }

        // Submit one producer per active source
        for (SourceSystem source : activeSources) {
            producerPool.submit(
                    new IngestionWorker(source, batchService, queue));
        }

        // 5. Wait for all producers to finish enqueuing
        producerPool.shutdown();
        try {
            boolean producersDone =
                    producerPool.awaitTermination(SHUTDOWN_TIMEOUT_SEC, TimeUnit.SECONDS);
            if (!producersDone) {
                System.err.println("[Orchestrator] Producers did not finish within timeout.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("[Orchestrator] Interrupted while waiting for producers.");
        }

        // 6. Signal consumers — no more items are coming
        producersRunning.set(false);

        // 7. Wait for consumers to drain the queue and exit cleanly
        consumerPool.shutdown();
        try {
            boolean consumersDone =
                    consumerPool.awaitTermination(SHUTDOWN_TIMEOUT_SEC, TimeUnit.SECONDS);
            if (consumersDone) {
                System.out.println("[Orchestrator] Pipeline completed. All transactions processed.");
            } else {
                System.err.println("[Orchestrator] Consumers timed out — forcing shutdown.");
                consumerPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("[Orchestrator] Interrupted while waiting for consumers.");
            consumerPool.shutdownNow();
        }
    }
}