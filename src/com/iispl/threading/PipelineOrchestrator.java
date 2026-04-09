package com.iispl.threading;

import com.iispl.dao.BatchDao;
import com.iispl.dao.CreditTransactionDao;
import com.iispl.dao.DebitTransactionDao;
import com.iispl.dao.InterBankTransactionDao;
import com.iispl.dao.NettingPositionDao;
import com.iispl.dao.ReconciliationEntryDao;
import com.iispl.dao.ReversalTransactionDao;
import com.iispl.dao.SettlementDao;
import com.iispl.dao.TransactionDao;
import com.iispl.entity.Batch;
import com.iispl.entity.IncomingTransaction;
import com.iispl.entity.SourceSystem;
import com.iispl.ingestion.AdapterRegistry;
import com.iispl.service.NettingService;
import com.iispl.service.ReconciliationService;
import com.iispl.service.SettlementService;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * PipelineOrchestrator — wires the full two-stage pipeline.
 *
 * Stage 1: Ingestion
 *   N × IngestionWorker  (one per SourceSystem, parallel)
 *                        → BlockingQueue<IncomingTransaction>
 *   1 × BatchProcessor   (accumulates, saves txns + batches to DB)
 *                        → BlockingQueue<Batch>
 *
 * Stage 2: Settlement
 *   1 × SettlementProcessor (settles, nets, reconciles — all persisted to DB)
 *
 * Shutdown via Poison Pill pattern:
 *   Each IngestionWorker pushes POISON_TRANSACTION when done.
 *   BatchProcessor counts pills; on last one pushes POISON_BATCH.
 *   SettlementProcessor stops on POISON_BATCH.
 */
public class PipelineOrchestrator {

    private static final Logger logger = Logger.getLogger(PipelineOrchestrator.class.getName());

    private static final int TRANSACTION_QUEUE_CAPACITY = 1000;
    private static final int BATCH_QUEUE_CAPACITY       = 100;

    // Poison pills — checked by reference (==), not value
    static final IncomingTransaction POISON_TRANSACTION = new IncomingTransaction(
            -1L, null, -1L, null, null, "POISON", "POISON", null, null, null, "POISON"
    );
    static final Batch POISON_BATCH = new Batch(
            "POISON", null, null, -1L, null, null
    );

    // ── Services ──────────────────────────────────────────────────────────────
    private final List<SourceSystem>    sourceSystems;
    private final AdapterRegistry       adapterRegistry;
    private final SettlementService     settlementService;
    private final NettingService        nettingService;
    private final ReconciliationService reconciliationService;

    // ── DAOs ──────────────────────────────────────────────────────────────────
    private final BatchDao               batchDao;
    private final TransactionDao         transactionDao;
    private final CreditTransactionDao   creditTransactionDao;
    private final DebitTransactionDao    debitTransactionDao;
    private final ReversalTransactionDao reversalTransactionDao;
    private final InterBankTransactionDao interBankTransactionDao;
    private final SettlementDao          settlementDao;
    private final NettingPositionDao     nettingPositionDao;
    private final ReconciliationEntryDao reconciliationEntryDao;

    public PipelineOrchestrator(
            List<SourceSystem>    sourceSystems,
            AdapterRegistry       adapterRegistry,
            SettlementService     settlementService,
            NettingService        nettingService,
            ReconciliationService reconciliationService,
            BatchDao              batchDao,
            TransactionDao        transactionDao,
            CreditTransactionDao  creditTransactionDao,
            DebitTransactionDao   debitTransactionDao,
            ReversalTransactionDao  reversalTransactionDao,
            InterBankTransactionDao interBankTransactionDao,
            SettlementDao           settlementDao,
            NettingPositionDao      nettingPositionDao,
            ReconciliationEntryDao  reconciliationEntryDao) {

        this.sourceSystems           = sourceSystems;
        this.adapterRegistry         = adapterRegistry;
        this.settlementService       = settlementService;
        this.nettingService          = nettingService;
        this.reconciliationService   = reconciliationService;
        this.batchDao                = batchDao;
        this.transactionDao          = transactionDao;
        this.creditTransactionDao    = creditTransactionDao;
        this.debitTransactionDao     = debitTransactionDao;
        this.reversalTransactionDao  = reversalTransactionDao;
        this.interBankTransactionDao = interBankTransactionDao;
        this.settlementDao           = settlementDao;
        this.nettingPositionDao      = nettingPositionDao;
        this.reconciliationEntryDao  = reconciliationEntryDao;
    }

    // =========================================================================
    // START
    // =========================================================================

    public void start() {
        int sourceCount = sourceSystems.size();
        logger.info("[Orchestrator] Pipeline starting | Sources: " + sourceCount);

        // ── Shared queues ──────────────────────────────────────────────────────
        LinkedBlockingQueue<IncomingTransaction> transactionQueue =
                new LinkedBlockingQueue<>(TRANSACTION_QUEUE_CAPACITY);
        LinkedBlockingQueue<Batch> batchQueue =
                new LinkedBlockingQueue<>(BATCH_QUEUE_CAPACITY);

        // ── Stage 1: One IngestionWorker per SourceSystem (parallel) ──────────
        ExecutorService ingestionPool = Executors.newFixedThreadPool(sourceCount);
        for (SourceSystem sourceSystem : sourceSystems) {
            ingestionPool.submit(new IngestionWorker(
                    sourceSystem, adapterRegistry, transactionQueue, sourceCount));
        }

        // ── Stage 1: BatchProcessor (accumulates + saves txns + creates batches)
        Thread batchProcessorThread = new Thread(new BatchProcessor(
                transactionQueue,
                batchQueue,
                sourceCount,
                batchDao,
                transactionDao,
                creditTransactionDao,
                debitTransactionDao,
                reversalTransactionDao,
                interBankTransactionDao
        ), "BatchProcessor-Thread");

        // ── Stage 2: SettlementProcessor (settles + nets + reconciles + saves) ─
        Thread settlementProcessorThread = new Thread(new SettlementProcessor(
                batchQueue,
                settlementService,
                nettingService,
                reconciliationService,
                batchDao,
                settlementDao,
                nettingPositionDao,
                reconciliationEntryDao
        ), "SettlementProcessor-Thread");

        // ── Start threads ──────────────────────────────────────────────────────
        settlementProcessorThread.start();
        batchProcessorThread.start();
        logger.info("[Orchestrator] All threads started.");

        // ── Wait for ingestion pool ────────────────────────────────────────────
        ingestionPool.shutdown();
        try {
            if (!ingestionPool.awaitTermination(10, TimeUnit.MINUTES)) {
                logger.warning("[Orchestrator] Ingestion pool timeout — forcing shutdown.");
                ingestionPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            logger.severe("[Orchestrator] Interrupted waiting for ingestion: " + e.getMessage());
            ingestionPool.shutdownNow();
            Thread.currentThread().interrupt();
        }

        // ── Wait for processors ────────────────────────────────────────────────
        try {
            batchProcessorThread.join();
            settlementProcessorThread.join();
        } catch (InterruptedException e) {
            logger.severe("[Orchestrator] Interrupted waiting for processors: " + e.getMessage());
            Thread.currentThread().interrupt();
        }

        logger.info("[Orchestrator] Pipeline completed successfully.");
    }
}