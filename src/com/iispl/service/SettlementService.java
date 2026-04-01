package com.iispl.service;

import com.iispl.entity.IncomingTransaction;
import com.iispl.entity.SettlementResult;
import com.iispl.entity.Transaction;
import com.iispl.enums.ChannelType;

import java.time.LocalDate;
import java.util.List;

public interface SettlementService {

    /**
     * Called by BatchService.processBatch().
     * Orchestrates validation → mapping → settlement logic → netting → file export
     * for a single date+channel batch.
     *
     * Populates the provided SettlementResult in-place
     * (settledCount, failedCount, amounts, filePath).
     */
    void settle(String batchId,
                LocalDate settlementDate,
                ChannelType channel,
                List<IncomingTransaction> transactions,
                SettlementResult result);

    /**
     * Validates a single IncomingTransaction.
     * Returns null if valid; returns an error message string if invalid.
     */
    String validate(IncomingTransaction txn);

    /**
     * Maps an IncomingTransaction to a concrete Transaction subtype
     * (CreditTransaction, DebitTransaction, InterBankTransaction, etc.).
     */
    Transaction mapToTransaction(IncomingTransaction txn);

    /**
     * Applies netting: collapses offsetting credits and debits between the
     * same bank pair within a batch, reducing gross settlement volume.
     * Returns the net amount (credits minus debits).
     */
    java.math.BigDecimal applyNetting(List<Transaction> transactions);

    /**
     * Writes the SettlementResult to a file on disk.
     * File path pattern: settlements/<batchId>.csv
     * Returns the absolute path of the created file.
     */
    String exportToFile(SettlementResult result);

    /**
     * Single-transaction entry point used directly by SettlementProcessor.
     * Wraps the single txn as a one-item batch and delegates to settle().
     */
    SettlementResult process(IncomingTransaction txn);
}