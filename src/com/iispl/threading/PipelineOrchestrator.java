package com.iispl.threading;

import com.iispl.entity.IncomingTransaction;
import com.iispl.entity.IncomingTransaction;
import com.iispl.entity.SourceSystem;
import com.iispl.service.BatchService;
import com.iispl.service.SettlementService;

import java.util.List;
import java.util.concurrent.*;

public class PipelineOrchestrator {

    private static final int QUEUE_CAPACITY = 1000;

    private final BatchService batchService;
    private final SettlementService settlementService;

    public PipelineOrchestrator(BatchService batchService,
                                SettlementService settlementService) {
        this.batchService = batchService;
        this.settlementService = settlementService;
    }

    public void startPipeline(List<SourceSystem> activeSources) {

        System.out.println("[Orchestrator] Starting NPCI-style pipeline...");

        // =========================
        // 1. SHARED QUEUE (NPCI BUFFER)
        // =========================
        BlockingQueue<IncomingTransaction> queue =
                new ArrayBlockingQueue<>(QUEUE_CAPACITY);

        // =========================
        // 2. PRODUCER POOL (INGESTION)
        // =========================
        ThreadPoolExecutor producerPool = new ThreadPoolExecutor(
                activeSources.size(),
                activeSources.size(),
                0L,
                TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(),        // unbounded — no RejectedExecutionException
                new ThreadPoolExecutor.CallerRunsPolicy()
        );

        // =========================
        // 3. SCHEDULER (BATCH ENGINE)
        // =========================
        Thread schedulerThread = new Thread(
                new BatchScheduler(queue, batchService)  // SettlementService removed — BatchService owns it
        );
        schedulerThread.setName("batch-scheduler");
        schedulerThread.start();

        // =========================
        // 4. START PRODUCERS
        // =========================
        for (SourceSystem source : activeSources) {
            producerPool.submit(
                    new IngestionWorker(source, batchService, queue)
            );
        }

        // =========================
        // 5. KEEP SYSTEM RUNNING (SIMULATION)
        // =========================
        producerPool.shutdown();

        try {
            producerPool.awaitTermination(5, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        System.out.println("[Orchestrator] Producers finished (system still running for batches).");

        // NOTE:
        // In real system → producers NEVER stop
        // Scheduler keeps running forever
    }
}