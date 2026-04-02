package com.iispl.threading;

import com.iispl.entity.IncomingTransaction;
import com.iispl.entity.SourceSystem;
import com.iispl.service.BatchService;
import com.iispl.service.SettlementService;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class PipelineOrchestrator {

    private static final int QUEUE_CAPACITY        = 500;
    private static final int CONSUMER_THREAD_COUNT = 4;
    private static final int SHUTDOWN_TIMEOUT_SEC  = 60;

    private final BatchService batchService;
    private final SettlementService settlementService;

    public PipelineOrchestrator(BatchService batchService,
                                SettlementService settlementService) {
        this.batchService = batchService;
        this.settlementService = settlementService;
    }

    public void startPipeline(List<SourceSystem> activeSources) {

        System.out.println("[Orchestrator] Starting pipeline for "
                + activeSources.size() + " source(s).");

        // =========================
        // 1. DATA QUEUE (PIPELINE)
        // =========================
        BlockingQueue<IncomingTransaction> queue =
                new ArrayBlockingQueue<>(QUEUE_CAPACITY);

        AtomicBoolean producersRunning = new AtomicBoolean(true);

        // =========================
        // 2. THREAD FACTORIES 
        // =========================
        ThreadFactory producerFactory = r -> {
            Thread t = new Thread(r);
            t.setName("producer-" + t.getId());
            return t;
        };

        ThreadFactory consumerFactory = r -> {
            Thread t = new Thread(r);
            t.setName("consumer-" + t.getId());
            return t;
        };

        // =========================
        // 3. PRODUCER POOL
        // =========================
        ThreadPoolExecutor producerPool = new ThreadPoolExecutor(
                activeSources.size(),
                activeSources.size(),
                0L,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(activeSources.size() * 2),
                producerFactory,
                new ThreadPoolExecutor.CallerRunsPolicy()
        );

        // =========================
        // 4. CONSUMER POOL
        // =========================
        ThreadPoolExecutor consumerPool = new ThreadPoolExecutor(
                CONSUMER_THREAD_COUNT,
                CONSUMER_THREAD_COUNT,
                0L,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(100),
                consumerFactory,
                new ThreadPoolExecutor.CallerRunsPolicy()
        );

        // =========================
        // 5. START CONSUMERS FIRST
        // =========================
        for (int i = 0; i < CONSUMER_THREAD_COUNT; i++) {
            consumerPool.submit(
                    new SettlementProcessor(
                            batchService,
                            settlementService,
                            queue,
                            producersRunning
                    )
            );
        }

        // =========================
        // 6. START PRODUCERS
        // =========================
        for (SourceSystem source : activeSources) {
            producerPool.submit(
                    new IngestionWorker(
                            source,
                            batchService,
                            queue
                    )
            );
        }

        // =========================
        // 7. SHUTDOWN PRODUCERS
        // =========================
        producerPool.shutdown();
        try {
            if (!producerPool.awaitTermination(SHUTDOWN_TIMEOUT_SEC, TimeUnit.SECONDS)) {
                System.err.println("[Orchestrator] Producers timed out. Forcing shutdown...");
                producerPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            producerPool.shutdownNow();
        }

        // =========================
        // 8. SIGNAL CONSUMERS
        // =========================
        producersRunning.set(false);
        System.out.println("[Orchestrator] Producers done. Consumers draining queue...");

        // =========================
        // 9. SHUTDOWN CONSUMERS
        // =========================
        consumerPool.shutdown();
        try {
            if (!consumerPool.awaitTermination(SHUTDOWN_TIMEOUT_SEC, TimeUnit.SECONDS)) {
                System.err.println("[Orchestrator] Consumers timed out. Forcing shutdown...");
                consumerPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            consumerPool.shutdownNow();
        }

        System.out.println("Producer Completed Tasks: " + producerPool.getCompletedTaskCount());
        System.out.println("Consumer Completed Tasks: " + consumerPool.getCompletedTaskCount());

        System.out.println("[Orchestrator] Pipeline completed successfully.");
    }
}