package com.iispl.service;

import com.iispl.entity.Batch;
import com.iispl.entity.CreditTransaction;
import com.iispl.entity.DebitTransaction;
import com.iispl.entity.IncomingTransaction;
import com.iispl.entity.SettlementResult;
import com.iispl.enums.BatchStatus;
import com.iispl.enums.SettlementStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.logging.Logger;

public class SettlementServiceImpl implements SettlementService {

    private static final Logger logger = Logger.getLogger(SettlementServiceImpl.class.getName());

    private final NPCIService npciService;

    public SettlementServiceImpl(NPCIService npciService) {
        this.npciService = npciService;
    }

    // =========================================================================
    // SETTLE
    // For each transaction in the batch:
    //   CREDIT   → credit toBankName's balance
    //   DEBIT    → debit fromBankName's balance
    //   REVERSAL → reverse the original: credit fromBankName, debit toBankName
    //   INTRABANK→ internal move: debit fromBank, credit toBank
    // Produces a SettlementResult summarising the batch.
    // =========================================================================

    @Override
    public SettlementResult settle(Batch batch) {
        String batchId = batch.getBatchId();
        logger.info("[SettlementService] Starting settlement | BatchId: " + batchId
                + " | Transactions: " + batch.getTotalTransactions());

        batch.setBatchStatus(BatchStatus.RUNNING);

        int        settledCount       = 0;
        BigDecimal totalSettledAmount = BigDecimal.ZERO;

        for (IncomingTransaction txn : batch.getTransactionList()) {
            try {
                settleTransaction(txn);
                settledCount++;
                totalSettledAmount = totalSettledAmount.add(txn.getAmount());

                logger.info("[SettlementService] Settled txn [id=" + txn.getIncomingTnxId()
                        + "] | Type: " + txn.getTransactionType()
                        + " | Amount: " + txn.getAmount()
                        + " | From: " + txn.getFromBankName()
                        + " | To: " + txn.getToBankName());

            } catch (Exception e) {
                // No failed transactions in this system — log and continue
                logger.severe("[SettlementService] Unexpected error settling txn [id="
                        + txn.getIncomingTnxId() + "]: " + e.getMessage());
            }
        }

        // ── Determine final batch status ───────────────────────────────────────
        BatchStatus finalBatchStatus;
        SettlementStatus settlementStatus;

        if (settledCount == batch.getTotalTransactions()) {
            finalBatchStatus  = BatchStatus.COMPLETED;
            settlementStatus  = SettlementStatus.SETTLED;
        } else if (settledCount > 0) {
            finalBatchStatus  = BatchStatus.PARTIAL;
            settlementStatus  = SettlementStatus.PARTIALLY_SETTLED;
        } else {
            finalBatchStatus  = BatchStatus.FAILED;
            settlementStatus  = SettlementStatus.FAILED;
        }

        batch.setBatchStatus(finalBatchStatus);

        SettlementResult result = new SettlementResult(
                batchId,
                settlementStatus,
                settledCount,
                (int) batch.getTotalTransactions() - settledCount,
                totalSettledAmount,
                LocalDateTime.now()
        );

        logger.info("[SettlementService] Settlement complete | BatchId: " + batchId
                + " | Status: " + settlementStatus
                + " | Settled: " + settledCount
                + " | TotalSettledAmount: " + totalSettledAmount);

        return result;
    }

    // =========================================================================
    // SETTLE SINGLE TRANSACTION
    // =========================================================================

    private void settleTransaction(IncomingTransaction txn) {
        String fromBank = txn.getFromBankName();
        String toBank   = txn.getToBankName();
        BigDecimal amount = txn.getAmount();

        switch (txn.getTransactionType()) {

            case CREDIT:
                // Money flows into toBank
                npciService.creditBalance(toBank, amount);
                break;

            case DEBIT:
                // Money flows out of fromBank
                npciService.debitBalance(fromBank, amount);
                break;

            case REVERSAL:
                // Reverse the original flow:
                // Original was fromBank → toBank, so reversal is toBank → fromBank
                npciService.creditBalance(fromBank, amount);
                npciService.debitBalance(toBank, amount);
                logger.info("[SettlementService] REVERSAL applied | fromBank credited: "
                        + fromBank + " | toBank debited: " + toBank);
                break;

            case INTRABANK:
                // Internal interbank move: debit sender, credit receiver
                npciService.debitBalance(fromBank, amount);
                npciService.creditBalance(toBank, amount);
                break;

            default:
                logger.warning("[SettlementService] Unknown transactionType: "
                        + txn.getTransactionType() + " for txn id=" + txn.getIncomingTnxId());
        }
    }
}