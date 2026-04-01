package com.iispl.dao;

import com.iispl.entity.CreditTransaction;
import com.iispl.entity.DebitTransaction;
import com.iispl.entity.IncomingTransaction;
import com.iispl.entity.InterBankTransaction;
import com.iispl.entity.ReversalTransaction;
import com.iispl.entity.Transaction;
import com.iispl.enums.TransactionStatus;

import java.sql.Connection;
import java.util.List;
import java.util.Optional;

public interface TransactionDao {

    /**
     * Persists a raw incoming transaction (before mapping/settlement).
     * Returns the generated DB id.
     */
    long saveIncoming(IncomingTransaction txn, Connection conn);

    /**
     * Updates the processing_status of an IncomingTransaction row.
     */
    void updateIncomingStatus(long incomingId, String processingStatus, Connection conn);

    // ---- concrete subtypes ----

    long saveCreditTransaction(CreditTransaction txn, Connection conn);

    long saveDebitTransaction(DebitTransaction txn, Connection conn);

    long saveInterBankTransaction(InterBankTransaction txn, Connection conn);

    long saveReversalTransaction(ReversalTransaction txn, Connection conn);

    /**
     * Updates the status column on the transaction base table.
     */
    void updateTransactionStatus(long transactionId, TransactionStatus status, Connection conn);

    /**
     * Fetches any transaction by its base-table id.
     * Returns empty if not found.
     */
    Optional<Transaction> findById(long transactionId, Connection conn);

    /**
     * Returns all transactions belonging to a settlement batch.
     */
    List<Transaction> findByBatchId(long batchId, Connection conn);
}