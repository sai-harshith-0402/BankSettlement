package com.iispl.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

import com.iispl.dao.NettingPositionDao;
import com.iispl.dao.NettingPositionDaoImpl;
import com.iispl.entity.Batch;
import com.iispl.entity.IncomingTransaction;
import com.iispl.entity.NPCIBank;
import com.iispl.entity.NettingPosition;
import com.iispl.enums.TransactionType;

public class NettingServiceImpl implements NettingService {

    private static final Logger logger = Logger.getLogger(NettingServiceImpl.class.getName());
    private final NettingPositionDao nettingDao = new NettingPositionDaoImpl();
    private static final AtomicLong positionIdSequence = new AtomicLong(1);

    private final NPCIService npciService;

    public NettingServiceImpl(NPCIService npciService) {
        this.npciService = npciService;
    }

    @Override
    public List<NettingPosition> computeNetting(Batch batch) {

        String batchId = batch.getBatchId();
        Map<String, BigDecimal[]> bankTotals = new HashMap<>();

        for (IncomingTransaction txn : batch.getTransactionList()) {
            // Only CREDIT and DEBIT participate; skip REVERSAL and INTRABANK
            if (txn.getTransactionType() != TransactionType.CREDIT &&
                txn.getTransactionType() != TransactionType.DEBIT) {
                continue;
            }

            String fromBank   = txn.getFromBankName();
            String toBank     = txn.getToBankName();
            BigDecimal amount = txn.getAmount();

            bankTotals.computeIfAbsent(fromBank, k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            bankTotals.get(fromBank)[0] = bankTotals.get(fromBank)[0].add(amount); // gross debit

            bankTotals.computeIfAbsent(toBank, k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            bankTotals.get(toBank)[1] = bankTotals.get(toBank)[1].add(amount);     // gross credit
        }

        List<NettingPosition> positions = new ArrayList<>();

        for (Map.Entry<String, BigDecimal[]> entry : bankTotals.entrySet()) {

            String bankName       = entry.getKey();
            BigDecimal grossDebit  = entry.getValue()[0];
            BigDecimal grossCredit = entry.getValue()[1];
            BigDecimal netAmount   = grossCredit.subtract(grossDebit);

            // NPCIBank lookup is OPTIONAL enrichment only — we NEVER skip a bank just
            // because it is not registered in npci_bank. bankId = 0 if not found.
            long bankId = 0L;
            NPCIBank bank = npciService.findByBankName(bankName);
            if (bank != null) {
                bankId = bank.getBankId();
            }

            NettingPosition pos = new NettingPosition(
                    positionIdSequence.getAndIncrement(),
                    batchId,    // NEW param
                    bankName,   // NEW param
                    bankId,
                    grossDebit,
                    grossCredit,
                    netAmount,
                    LocalDateTime.now()
            );

            positions.add(pos);

            try {
                nettingDao.saveNettingPosition(pos);
                logger.info("[NettingService] Saved | BatchId: " + batchId
                        + " | Bank: " + bankName
                        + " | GrossDebit: "  + grossDebit
                        + " | GrossCredit: " + grossCredit
                        + " | Net: "         + netAmount);
            } catch (Exception e) {
                logger.severe("[NettingService] Save failed for bank [" + bankName
                        + "] batch [" + batchId + "]: " + e.getMessage());
            }
        }

        logger.info("[NettingService] Complete | BatchId: " + batchId
                + " | Positions: " + positions.size());
        return positions;
    }
}