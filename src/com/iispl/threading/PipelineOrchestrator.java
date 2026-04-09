package com.iispl.threading;

import com.iispl.entity.Batch;
import com.iispl.entity.IncomingTransaction;
import com.iispl.entity.SourceSystem;
import com.iispl.service.BatchService;
import com.iispl.service.NettingService;
import com.iispl.service.NPCIService;
import com.iispl.service.ReconciliationService;
import com.iispl.service.SettlementService;
import com.iispl.ingestion.AdapterRegistry;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * PipelineOrchestrator — wires the full two-stage pipeline:
 *
 * Stage 1: Ingestion
 *   N × IngestionWorker (one per SourceSystem) → BlockingQueue<IncomingTransaction>
 *   1 × BatchProcessor  (consumer + producer)  → BlockingQueue<Batch>
 *
 * Stage 2: Settlement
 *   1 × SettlementProcessor (consumer) → logs SettlementResult, NettingPositions,
 *                                         ReconciliationEntries
 *
 * Shutdown:
 *   IngestionWorkers push POISON_TRANSACTION after each file completes.
 *   BatchProcessor counts poison pills — when it sees N pills (one per source),
 *   it flushes remaining transactions, then pushes POISON_BATCH.
 *   SettlementProcessor stops when it receives POISON_BATCH.
 */
public class PipelineOrchestrator {

    private static final Logger logger = Logger.getLogger(PipelineOrchestrator.class.getName());

    // ── Queue capacities ───────────────────────────────────────────────────────
    private static final int TRANSACTION_QUEUE_CAPACITY = 1000;
    private static final int BATCH_QUEUE_CAPACITY       = 100;

    // ── Poison pills ───────────────────────────────────────────────────────────
    // Sentinel objects — signal consumers to shut down
    static final IncomingTransaction POISON_TRANSACTION = new IncomingTransaction(
            -1L, null, -1L, null, null, "POISON", "POISON",
            null, null, null, "POISON"
    );
    static final Batch POISON_BATCH = new Batch(
            "POISON", null, null, -1L, null, null
    );

    // ── Dependencies ───────────────────────────────────────────────────────────
    private final List<SourceSystem>    sourceSystems;
    private final AdapterRegistry       adapterRegistry;
    private final BatchService          batchService;
    private final SettlementService     settlementService;
    private final NettingService        nettingService;
    private final ReconciliationService reconciliationService;
    private final NPCIService           npciService;

    public PipelineOrchestrator(
            List<SourceSystem>    sourceSystems,
            AdapterRegistry       adapterRegistry,
            BatchService          batchService,
            SettlementService     settlementService,
            NettingService        nettingService,
            ReconciliationService reconciliationService,
            NPCIService           npciService) {

        this.sourceSystems        = sourceSystems;
        this.adapterRegistry      = adapterRegistry;
        this.batchService         = batchService;
        this.settlementService    = settlementService;
        this.nettingService       = nettingService;
        this.reconciliationService= reconciliationService;
        this.npciService          = npciService;
    }

    // =========================================================================
    // START — entry point called by Main
    // =========================================================================

    public void start() {
        logger.info("[Orchestrator] Pipeline starting | SourceSystems: " + sourceSystems.size());

        // ── Shared queues ──────────────────────────────────────────────────────
        LinkedBlockingQueue<IncomingTransaction> transactionQueue =
                new LinkedBlockingQueue<>(TRANSACTION_QUEUE_CAPACITY);

        LinkedBlockingQueue<Batch> batchQueue =
                new LinkedBlockingQueue<>(BATCH_QUEUE_CAPACITY);

        int sourceCount = sourceSystems.size();

        // ── Stage 1: Ingestion thread pool ─────────────────────────────────────
        // One IngestionWorker per SourceSystem, all running in parallel
        ExecutorService ingestionPool = Executors.newFixedThreadPool(sourceCount);

        for (SourceSystem sourceSystem : sourceSystems) {
            ingestionPool.submit(new IngestionWorker(
                    sourceSystem,
                    adapterRegistry,
                    transactionQueue,
                    sourceCount
            ));
        }

        // ── Stage 1: BatchProcessor ────────────────────────────────────────────
        // Single thread — accumulates transactions, creates batches, feeds Stage 2
        Thread batchProcessorThread = new Thread(new BatchProcessor(
                transactionQueue,
                batchQueue,
                batchService,
                sourceCount
        ), "BatchProcessor-Thread");

        // ── Stage 2: SettlementProcessor ──────────────────────────────────────
        // Single thread — consumes batches, settles, nets, reconciles
        Thread settlementProcessorThread = new Thread(new SettlementProcessor(
                batchQueue,
                settlementService,
                nettingService,
                reconciliationService
        ), "SettlementProcessor-Thread");

        // ── Start all threads ──────────────────────────────────────────────────
        settlementProcessorThread.start();
        batchProcessorThread.start();

        logger.info("[Orchestrator] All threads started.");

        // ── Shutdown ingestion pool after all workers complete ─────────────────
        ingestionPool.shutdown();
        try {
            // Wait up to 10 minutes for all ingestion workers to finish
            if (!ingestionPool.awaitTermination(10, TimeUnit.MINUTES)) {
                logger.warning("[Orchestrator] Ingestion pool did not finish in time — forcing shutdown.");
                ingestionPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            logger.severe("[Orchestrator] Interrupted while waiting for ingestion pool: "
                    + e.getMessage());
            ingestionPool.shutdownNow();
            Thread.currentThread().interrupt();
        }

        // ── Wait for BatchProcessor and SettlementProcessor to finish ──────────
        try {
            batchProcessorThread.join();
            settlementProcessorThread.join();
        } catch (InterruptedException e) {
            logger.severe("[Orchestrator] Interrupted while waiting for processors: "
                    + e.getMessage());
            Thread.currentThread().interrupt();
        }

        logger.info("[Orchestrator] Pipeline completed successfully.");
    }
}