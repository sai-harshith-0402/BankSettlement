package com.iispl.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

import com.iispl.dao.ReconciliationEntryDao;
import com.iispl.dao.ReconciliationEntryDaoImpl;
import com.iispl.entity.Batch;
import com.iispl.entity.NPCIBank;
import com.iispl.entity.NettingPosition;
import com.iispl.entity.ReconciliationEntry;
import com.iispl.enums.ReconStatus;

public class ReconciliationServiceImpl implements ReconciliationService {

    private static final Logger logger = Logger.getLogger(ReconciliationServiceImpl.class.getName());
    private final ReconciliationEntryDao reconDao = new ReconciliationEntryDaoImpl();
    private static final BigDecimal VARIANCE_TOLERANCE = new BigDecimal("0.01");
    private static final AtomicLong entryIdSequence = new AtomicLong(1);

    private final NPCIService npciService;

    public ReconciliationServiceImpl(NPCIService npciService) {
        this.npciService = npciService;
    }

    @Override
    public List<ReconciliationEntry> reconcile(Batch batch, List<NettingPosition> positions) {

        List<ReconciliationEntry> entries = new ArrayList<>();

        for (NettingPosition pos : positions) {

            NPCIBank bank   = findBankById(pos.getCounterpartyBankId());
            BigDecimal expected = pos.getNetAmount();
            BigDecimal actual   = (bank != null) ? bank.getBalanceAmount() : BigDecimal.ZERO;
            BigDecimal variance = actual.subtract(expected);

            ReconStatus status;
            if (bank == null) {
                // Bank not registered in npci_bank — cannot verify balance
                status = ReconStatus.EXCEPTION;
            } else {
                status = determineReconStatus(variance.abs());
            }

            ReconciliationEntry entry = new ReconciliationEntry(
                    entryIdSequence.getAndIncrement(),
                    batch.getBatchDate(),
                    pos.getCounterpartyBankId(),
                    expected,
                    actual,
                    variance,
                    status
            );

            entries.add(entry);

            // FIX: ONE save here inside the service — the caller (BankSettlementUtility)
            // must NOT call saveReconciliationEntry again. Removed the duplicate save
            // in opsRunReconciliation() that was causing primary-key violations.
            try {
                reconDao.saveReconciliationEntry(entry);
                logger.info("[ReconciliationService] Saved entry | BatchId: " + batch.getBatchId()
                        + " | BankId: "  + pos.getCounterpartyBankId()
                        + " | Expected: " + expected
                        + " | Actual: "   + actual
                        + " | Variance: " + variance
                        + " | Status: "   + status);
            } catch (Exception e) {
                logger.severe("[ReconciliationService] Failed to save reconciliation entry ["
                        + entry.getEntryId() + "]: " + e.getMessage());
            }
        }

        return entries;
    }

    private ReconStatus determineReconStatus(BigDecimal absVariance) {
        if (absVariance.compareTo(BigDecimal.ZERO) == 0)
            return ReconStatus.MATCHED;
        else if (absVariance.compareTo(VARIANCE_TOLERANCE) <= 0)
            return ReconStatus.PARTIALLY_MATCHED;
        else
            return ReconStatus.UNMATCHED;
    }

    private NPCIBank findBankById(long bankId) {
        for (NPCIBank bank : npciService.getAllBanks()) {
            if (bank.getBankId() == bankId)
                return bank;
        }
        return null;
    }

    private long countByStatus(List<ReconciliationEntry> entries, ReconStatus status) {
        long count = 0;
        for (ReconciliationEntry e : entries)
            if (e.getReconStatus() == status) count++;
        return count;
    }
}