package com.iispl.service;

import com.iispl.entity.Batch;
import com.iispl.entity.NPCIBank;
import com.iispl.entity.NettingPosition;
import com.iispl.entity.ReconciliationEntry;
import com.iispl.enums.ReconStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

public class ReconciliationServiceImpl implements ReconciliationService {

    private static final Logger logger = Logger.getLogger(ReconciliationServiceImpl.class.getName());

    // Tolerance for floating point variance (0.01 = 1 paisa tolerance)
    private static final BigDecimal VARIANCE_TOLERANCE = new BigDecimal("0.01");

    // Simple ID generator — threading layer can replace with DB sequence
    private static final AtomicLong entryIdSequence = new AtomicLong(1);

    private final NPCIService npciService;

    public ReconciliationServiceImpl(NPCIService npciService) {
        this.npciService = npciService;
    }

    // =========================================================================
    // RECONCILE
    //
    // For each NettingPosition:
    //   expectedAmount = netAmount from netting (what should have moved)
    //   actualAmount   = current NPCIBank.balanceAmount (what actually moved)
    //   variance       = actualAmount - expectedAmount
    //   reconStatus:
    //     variance == 0               → MATCHED
    //     |variance| <= tolerance     → PARTIALLY_MATCHED
    //     variance != 0 & > tolerance → UNMATCHED
    //     bank not found in NPCI      → EXCEPTION
    // =========================================================================

    @Override
    public List<ReconciliationEntry> reconcile(Batch batch,
            List<NettingPosition> nettingPositions) {

        String    batchId   = batch.getBatchId();
        LocalDate reconDate = batch.getBatchDate();

        logger.info("[ReconciliationService] Starting reconciliation | BatchId: " + batchId
                + " | Netting positions: " + nettingPositions.size());

        List<ReconciliationEntry> entries = new ArrayList<>();

        for (NettingPosition position : nettingPositions) {

            long       counterpartyBankId = position.getCounterpartyBankId();
            BigDecimal expectedAmount     = position.getNetAmount();

            // Look up the bank by ID to get its current balance
            NPCIBank bank = findBankById(counterpartyBankId);

            if (bank == null) {
                logger.severe("[ReconciliationService] Bank not found for id="
                        + counterpartyBankId + " — marking as EXCEPTION.");

                ReconciliationEntry exceptionEntry = new ReconciliationEntry(
                        entryIdSequence.getAndIncrement(),
                        reconDate,
                        counterpartyBankId,
                        expectedAmount,
                        BigDecimal.ZERO,
                        expectedAmount.negate(),
                        ReconStatus.EXCEPTION
                );
                entries.add(exceptionEntry);
                continue;
            }

            BigDecimal actualAmount = bank.getBalanceAmount();
            BigDecimal variance     = actualAmount.subtract(expectedAmount);
            BigDecimal absVariance  = variance.abs();

            ReconStatus reconStatus = determineReconStatus(absVariance);

            ReconciliationEntry entry = new ReconciliationEntry(
                    entryIdSequence.getAndIncrement(),
                    reconDate,
                    counterpartyBankId,
                    expectedAmount,
                    actualAmount,
                    variance,
                    reconStatus
            );

            entries.add(entry);

            logger.info("[ReconciliationService] Entry | Bank: " + bank.getBankName()
                    + " | Expected: " + expectedAmount
                    + " | Actual: " + actualAmount
                    + " | Variance: " + variance
                    + " | Status: " + reconStatus);
        }

        // ── Summary log ────────────────────────────────────────────────────────
        long matched          = countByStatus(entries, ReconStatus.MATCHED);
        long partiallyMatched = countByStatus(entries, ReconStatus.PARTIALLY_MATCHED);
        long unmatched        = countByStatus(entries, ReconStatus.UNMATCHED);
        long exceptions       = countByStatus(entries, ReconStatus.EXCEPTION);

        logger.info("[ReconciliationService] Reconciliation complete | BatchId: " + batchId
                + " | Matched: "          + matched
                + " | PartiallyMatched: " + partiallyMatched
                + " | Unmatched: "        + unmatched
                + " | Exceptions: "       + exceptions);

        return entries;
    }

    // =========================================================================
    // DETERMINE RECON STATUS
    // =========================================================================

    private ReconStatus determineReconStatus(BigDecimal absVariance) {
        if (absVariance.compareTo(BigDecimal.ZERO) == 0) {
            return ReconStatus.MATCHED;
        } else if (absVariance.compareTo(VARIANCE_TOLERANCE) <= 0) {
            return ReconStatus.PARTIALLY_MATCHED;
        } else {
            return ReconStatus.UNMATCHED;
        }
    }

    // =========================================================================
    // FIND BANK BY ID
    // =========================================================================

    private NPCIBank findBankById(long bankId) {
        for (NPCIBank bank : npciService.getAllBanks()) {
            if (bank.getBankId() == bankId) {
                return bank;
            }
        }
        return null;
    }

    // =========================================================================
    // COUNT BY STATUS
    // =========================================================================

    private long countByStatus(List<ReconciliationEntry> entries, ReconStatus status) {
        long count = 0;
        for (ReconciliationEntry entry : entries) {
            if (entry.getReconStatus() == status) count++;
        }
        return count;
    }
}