package com.iispl.threading;

import com.iispl.entity.Batch;
import com.iispl.entity.NettingPosition;
import com.iispl.entity.ReconciliationEntry;
import com.iispl.entity.SettlementResult;
import com.iispl.service.NettingService;
import com.iispl.service.ReconciliationService;
import com.iispl.service.SettlementService;

import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

/**
 * SettlementProcessor — Consumer thread (Stage 2).
 *
 * Consumes Batches from batchQueue.
 * For each Batch runs the full settlement pipeline:
 *   1. settlementService.settle(batch)         → SettlementResult
 *   2. nettingService.computeNetting(batch)    → List<NettingPosition>
 *   3. reconciliationService.reconcile(...)    → List<ReconciliationEntry>
 *
 * Stops when it receives POISON_BATCH from BatchProcessor.
 *
 * No ReentrantLock needed here — single consumer thread, no shared
 * mutable state beyond what the services manage internally.
 */
public class SettlementProcessor implements Runnable {

    private static final Logger logger = Logger.getLogger(SettlementProcessor.class.getName());

    private final LinkedBlockingQueue<Batch> batchQueue;
    private final SettlementService          settlementService;
    private final NettingService             nettingService;
    private final ReconciliationService      reconciliationService;

    public SettlementProcessor(
            LinkedBlockingQueue<Batch> batchQueue,
            SettlementService          settlementService,
            NettingService             nettingService,
            ReconciliationService      reconciliationService) {

        this.batchQueue            = batchQueue;
        this.settlementService     = settlementService;
        this.nettingService        = nettingService;
        this.reconciliationService = reconciliationService;
    }

    // =========================================================================
    // RUN
    // =========================================================================

    @Override
    public void run() {
        String threadName = Thread.currentThread().getName();
        logger.info("[SettlementProcessor:" + threadName + "] Started — waiting for batches.");

        try {
            while (true) {
                // Blocks until a batch is available
                Batch batch = batchQueue.take();

                // ── Poison pill — all batches processed ───────────────────────
                if (batch == PipelineOrchestrator.POISON_BATCH) {
                    logger.info("[SettlementProcessor:" + threadName
                            + "] POISON_BATCH received — shutting down.");
                    break;
                }

                // ── Process the batch ─────────────────────────────────────────
                processBatch(batch, threadName);
            }

        } catch (InterruptedException e) {
            logger.severe("[SettlementProcessor:" + threadName
                    + "] Interrupted: " + e.getMessage());
            Thread.currentThread().interrupt();
        }

        logger.info("[SettlementProcessor:" + threadName + "] Shut down cleanly.");
    }

    // =========================================================================
    // PROCESS BATCH
    // Full pipeline: Settle → Net → Reconcile
    // =========================================================================

    private void processBatch(Batch batch, String threadName) {
        String batchId = batch.getBatchId();

        logger.info("[SettlementProcessor:" + threadName + "] Processing batch: " + batchId);

        try {
            // ── Step 1: Settlement ─────────────────────────────────────────────
            logger.info("[SettlementProcessor:" + threadName
                    + "] Step 1 — Settlement | BatchId: " + batchId);

            SettlementResult result = settlementService.settle(batch);

            logger.info("[SettlementProcessor:" + threadName + "] Settlement done"
                    + " | BatchId: "       + result.getBatchId()
                    + " | Status: "        + result.getStatus()
                    + " | Settled: "       + result.getSettledCount()
                    + " | Failed: "        + result.getFailedCount()
                    + " | TotalAmount: "   + result.getTotalSettledAmount()
                    + " | ProcessedAt: "   + result.getProcessedAt());

            // ── Step 2: Netting ────────────────────────────────────────────────
            logger.info("[SettlementProcessor:" + threadName
                    + "] Step 2 — Netting | BatchId: " + batchId);

            List<NettingPosition> positions = nettingService.computeNetting(batch);

            logger.info("[SettlementProcessor:" + threadName + "] Netting done"
                    + " | Positions: " + positions.size());

            for (NettingPosition pos : positions) {
                logger.info("[SettlementProcessor:" + threadName + "] NettingPosition"
                        + " | BankId: "       + pos.getCounterpartyBankId()
                        + " | GrossDebit: "   + pos.getGrossDebitAmount()
                        + " | GrossCredit: "  + pos.getGrossCreditAmount()
                        + " | Net: "          + pos.getNetAmount()
                        + " | Date: "         + pos.getPositionDate());
            }

            // ── Step 3: Reconciliation ─────────────────────────────────────────
            logger.info("[SettlementProcessor:" + threadName
                    + "] Step 3 — Reconciliation | BatchId: " + batchId);

            List<ReconciliationEntry> entries =
                    reconciliationService.reconcile(batch, positions);

            logger.info("[SettlementProcessor:" + threadName + "] Reconciliation done"
                    + " | Entries: " + entries.size());

            for (ReconciliationEntry entry : entries) {
                logger.info("[SettlementProcessor:" + threadName + "] ReconciliationEntry"
                        + " | EntryId: "       + entry.getEntryId()
                        + " | AccountId: "     + entry.getAccountId()
                        + " | Expected: "      + entry.getExpectedAmount()
                        + " | Actual: "        + entry.getActualAmount()
                        + " | Variance: "      + entry.getVariance()
                        + " | Status: "        + entry.getReconStatus()
                        + " | Date: "          + entry.getReconciliationDate());
            }

            logger.info("[SettlementProcessor:" + threadName
                    + "] Batch fully processed | BatchId: " + batchId);

        } catch (Exception e) {
            logger.severe("[SettlementProcessor:" + threadName
                    + "] Error processing batch [" + batchId + "]: " + e.getMessage());
        }
    }
}