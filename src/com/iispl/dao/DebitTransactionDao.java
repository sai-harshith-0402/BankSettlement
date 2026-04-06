package com.iispl.dao;

import com.iispl.entity.DebitTransaction;
import com.iispl.enums.TransactionStatus;

import java.time.LocalDateTime;
import java.util.List;

public interface DebitTransactionDao {

    // Persist a new debit transaction record
    DebitTransaction save(DebitTransaction debitTransaction);

    // Fetch all debit transactions in the system
    List<DebitTransaction> findAllTransactions();

    // Fetch a single debit transaction by its primary key; returns null if not found
    DebitTransaction findById(Long id);

    // Assign or update the settlement batch ID on a transaction
    void updateBatchId(Long transactionId, String settlementBatchId);

    // Fetch all debit transactions belonging to a specific debit account
    List<DebitTransaction> findByDebitAccountId(Long debitAccountId);

    // Fetch all debit transactions with a given settlement batch ID
    List<DebitTransaction> findBySettlementBatchId(String settlementBatchId);

    // Fetch all debit transactions with a specific status (PENDING, SETTLED, FAILED, etc.)
    List<DebitTransaction> findByStatus(TransactionStatus status);

    // Fetch all debit transactions falling within a date range
    List<DebitTransaction> findByDateRange(LocalDateTime from, LocalDateTime to);

    // Update the status of a specific transaction
    void updateStatus(Long transactionId, TransactionStatus newStatus);
}