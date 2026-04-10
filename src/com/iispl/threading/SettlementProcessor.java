package com.iispl.threading;

import com.iispl.dao.BatchDao;
import com.iispl.dao.NettingPositionDao;
import com.iispl.dao.ReconciliationEntryDao;
import com.iispl.dao.SettlementDao;
import com.iispl.entity.Batch;
import com.iispl.entity.NettingPosition;
import com.iispl.entity.ReconciliationEntry;
import com.iispl.entity.SettlementResult;
import com.iispl.enums.BatchStatus;
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
 *   1. settlementService.settle()          → SettlementResult  → saved to DB
 *   2. batchDao.updateBatchStatus()        → RUNNING → COMPLETED in DB
 *   3. nettingService.computeNetting()     → List<NettingPosition> → saved to DB
 *   4. reconciliationService.reconcile()  → List<ReconciliationEntry> → saved to DB
 *
 * Stops when POISON_BATCH received.
 */
public class SettlementProcessor implements Runnable {

    private static final Logger logger = Logger.getLogger(SettlementProcessor.class.getName());

    private final LinkedBlockingQueue<Batch> batchQueue;
    private final SettlementService          settlementService;
    private final NettingService             nettingService;
    private final ReconciliationService      reconciliationService;

    // DAOs for persistence
    private final BatchDao              batchDao;
    private final SettlementDao         settlementDao;
    private final NettingPositionDao    nettingPositionDao;
    private final ReconciliationEntryDao reconciliationEntryDao;

    public SettlementProcessor(
            LinkedBlockingQueue<Batch> batchQueue,
            SettlementService          settlementService,
            NettingService             nettingService,
            ReconciliationService      reconciliationService,
            BatchDao                   batchDao,
            SettlementDao              settlementDao,
            NettingPositionDao         nettingPositionDao,
            ReconciliationEntryDao     reconciliationEntryDao) {

        this.batchQueue             = batchQueue;
        this.settlementService      = settlementService;
        this.nettingService         = nettingService;
        this.reconciliationService  = reconciliationService;
        this.batchDao               = batchDao;
        this.settlementDao          = settlementDao;
        this.nettingPositionDao     = nettingPositionDao;
        this.reconciliationEntryDao = reconciliationEntryDao;
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
                Batch batch = batchQueue.take();

                if (batch == PipelineOrchestrator.POISON_BATCH) {
                    logger.info("[SettlementProcessor:" + threadName
                            + "] POISON_BATCH received — shutting down.");
                    break;
                }

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
    // PROCESS BATCH — Settle → Save → Net → Save → Reconcile → Save
    // =========================================================================

    private void processBatch(Batch batch, String threadName) {
        String batchId = batch.getBatchId();
        logger.info("[SettlementProcessor:" + threadName + "] Processing: " + batchId);

        try {
            // ── Step 1: Mark batch RUNNING in DB ──────────────────────────────
            batchDao.updateBatchStatus(batchId, BatchStatus.RUNNING);
            logger.info("[SettlementProcessor:" + threadName
                    + "] Batch status → RUNNING | BatchId: " + batchId);

            // ── Step 2: Settle ─────────────────────────────────────────────────
            // NPCIServiceImpl.creditBalance/debitBalance already persists to DB
            SettlementResult result = settlementService.settle(batch);

            // ── Step 3: Save SettlementResult to DB ───────────────────────────
            try {
                settlementDao.saveSettlement(result);
                logger.info("[SettlementProcessor:" + threadName
                        + "] SettlementResult saved | BatchId: " + batchId
                        + " | Status: "      + result.getStatus()
                        + " | Settled: "     + result.getSettledCount()
                        + " | Failed: "      + result.getFailedCount()
                        + " | Amount: "      + result.getTotalSettledAmount()
                        + " | ProcessedAt: " + result.getProcessedAt());
            } catch (Exception e) {
                logger.severe("[SettlementProcessor:" + threadName
                        + "] Failed to save SettlementResult: " + e.getMessage());
            }

            // ── Step 4: Update Batch status in DB (COMPLETED / PARTIAL / FAILED)
            try {
                batchDao.updateBatchStatus(batchId, batch.getBatchStatus());
                logger.info("[SettlementProcessor:" + threadName
                        + "] Batch status → " + batch.getBatchStatus()
                        + " | BatchId: " + batchId);
            } catch (Exception e) {
                logger.severe("[SettlementProcessor:" + threadName
                        + "] Failed to update batch status: " + e.getMessage());
            }

            // ── Step 5: Netting ────────────────────────────────────────────────
            List<NettingPosition> positions = nettingService.computeNetting(batch);

            // ── Step 6: Save NettingPositions to DB ───────────────────────────
            for (NettingPosition position : positions) {
                try {
                    nettingPositionDao.saveNettingPosition(position);
                    logger.info("[SettlementProcessor:" + threadName + "] NettingPosition saved"
                            + " | BankId: "      + position.getCounterpartyBankId()
                            + " | GrossDebit: "  + position.getGrossDebitAmount()
                            + " | GrossCredit: " + position.getGrossCreditAmount()
                            + " | Net: "         + position.getNetAmount());
                } catch (Exception e) {
                    logger.severe("[SettlementProcessor:" + threadName
                            + "] Failed to save NettingPosition id="
                            + position.getPositionId() + ": " + e.getMessage());
                }
            }

            // ── Step 7: Reconciliation ─────────────────────────────────────────
            List<ReconciliationEntry> entries =
                    reconciliationService.reconcile(batch, positions);

            // ── Step 8: Save ReconciliationEntries to DB ───────────────────────
            for (ReconciliationEntry entry : entries) {
                try {
                    reconciliationEntryDao.saveReconciliationEntry(entry);
                    logger.info("[SettlementProcessor:" + threadName
                            + "] ReconciliationEntry saved"
                            + " | EntryId: "   + entry.getEntryId()
                            + " | AccountId: " + entry.getAccountId()
                            + " | Expected: "  + entry.getExpectedAmount()
                            + " | Actual: "    + entry.getActualAmount()
                            + " | Variance: "  + entry.getVariance()
                            + " | Status: "    + entry.getReconStatus());
                } catch (Exception e) {
                    logger.severe("[SettlementProcessor:" + threadName
                            + "] Failed to save ReconciliationEntry id="
                            + entry.getEntryId() + ": " + e.getMessage());
                }
            }

            logger.info("[SettlementProcessor:" + threadName
                    + "] Batch fully processed and persisted | BatchId: " + batchId);

        } catch (Exception e) {
            logger.severe("[SettlementProcessor:" + threadName
                    + "] Error processing batch [" + batchId + "]: " + e.getMessage());

            // Mark batch FAILED in DB on unexpected error
            try {
                batchDao.updateBatchStatus(batchId, BatchStatus.FAILED);
            } catch (Exception ex) {
                logger.severe("[SettlementProcessor:" + threadName
                        + "] Also failed to mark batch FAILED: " + ex.getMessage());
            }
        }
    }
}