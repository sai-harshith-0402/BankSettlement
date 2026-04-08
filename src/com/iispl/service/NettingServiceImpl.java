package com.iispl.service;

import com.iispl.entity.Batch;
import com.iispl.entity.IncomingTransaction;
import com.iispl.entity.NPCIBank;
import com.iispl.entity.NettingPosition;
import com.iispl.enums.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

public class NettingServiceImpl implements NettingService {

    private static final Logger logger = Logger.getLogger(NettingServiceImpl.class.getName());

    // Simple ID generator for NettingPosition — threading layer can replace with DB sequence
    private static final AtomicLong positionIdSequence = new AtomicLong(1);

    private final NPCIService npciService;

    public NettingServiceImpl(NPCIService npciService) {
        this.npciService = npciService;
    }

    // =========================================================================
    // COMPUTE NETTING
    //
    // Multilateral netting per counterparty bank:
    //   For each unique counterpartyBankName in the batch:
    //     grossDebitAmount  = sum of amounts where fromBankName = counterparty
    //     grossCreditAmount = sum of amounts where toBankName   = counterparty
    //     netAmount         = grossCreditAmount - grossDebitAmount
    //       (positive = counterparty owes us, negative = we owe counterparty)
    //
    // Only CREDIT and DEBIT transactions participate in netting.
    // REVERSAL and INTRABANK are excluded (handled separately in settlement).
    // =========================================================================

    @Override
    public List<NettingPosition> computeNetting(Batch batch) {
        String batchId = batch.getBatchId();
        logger.info("[NettingService] Computing netting positions | BatchId: " + batchId);

        // Map: bankName → [grossDebit, grossCredit]
        Map<String, BigDecimal[]> bankTotals = new HashMap<>();

        int processed = 0;
        int skipped   = 0;

        for (IncomingTransaction txn : batch.getTransactionList()) {

            // Only CREDIT and DEBIT participate in netting
            if (txn.getTransactionType() != TransactionType.CREDIT
                    && txn.getTransactionType() != TransactionType.DEBIT) {
                skipped++;
                continue;
            }

            String fromBank = txn.getFromBankName();
            String toBank   = txn.getToBankName();
            BigDecimal amount = txn.getAmount();

            // fromBank sent money → gross debit for fromBank
            bankTotals.computeIfAbsent(fromBank, k -> new BigDecimal[]{
                    BigDecimal.ZERO, BigDecimal.ZERO});
            bankTotals.get(fromBank)[0] = bankTotals.get(fromBank)[0].add(amount);

            // toBank received money → gross credit for toBank
            bankTotals.computeIfAbsent(toBank, k -> new BigDecimal[]{
                    BigDecimal.ZERO, BigDecimal.ZERO});
            bankTotals.get(toBank)[1] = bankTotals.get(toBank)[1].add(amount);

            processed++;
        }

        logger.info("[NettingService] Processed " + processed
                + " transactions for netting | Skipped (non-nettable): " + skipped);

        // ── Build NettingPosition list ─────────────────────────────────────────
        List<NettingPosition> positions = new ArrayList<>();
        LocalDateTime positionDate = LocalDateTime.now();

        for (Map.Entry<String, BigDecimal[]> entry : bankTotals.entrySet()) {
            String bankName       = entry.getKey();
            BigDecimal grossDebit = entry.getValue()[0];
            BigDecimal grossCredit= entry.getValue()[1];
            BigDecimal netAmount  = grossCredit.subtract(grossDebit);

            // Look up bankId from NPCI registry
            NPCIBank npciBank = npciService.findByBankName(bankName);
            if (npciBank == null) {
                logger.warning("[NettingService] Bank not found in NPCI registry: "
                        + bankName + " — skipping netting position.");
                continue;
            }

            long counterpartyBankId = npciBank.getBankId();

            NettingPosition position = new NettingPosition(
                    positionIdSequence.getAndIncrement(),
                    counterpartyBankId,
                    grossDebit,
                    grossCredit,
                    netAmount,
                    positionDate
            );

            positions.add(position);

            logger.info("[NettingService] Position | Bank: " + bankName
                    + " | GrossDebit: " + grossDebit
                    + " | GrossCredit: " + grossCredit
                    + " | Net: " + netAmount);
        }

        logger.info("[NettingService] Netting complete | BatchId: " + batchId
                + " | Positions computed: " + positions.size());

        return positions;
    }
}