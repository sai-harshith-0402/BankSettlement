package com.iispl.dao;

import com.iispl.entity.CreditTransaction;
import com.iispl.enums.TransactionStatus;

import java.time.LocalDateTime;
import java.util.List;

public interface CreditTransactionDao {

    // Persist a new credit transaction record; returns the generated DB id
    CreditTransaction save(CreditTransaction creditTransaction);

    // Fetch all credit transactions in the system
    List<CreditTransaction> findAll();

    // Fetch a single credit transaction by its primary key; returns null if not found
    CreditTransaction findById(Long id);

    // Assign or update the settlement batch ID on a transaction
    void updateBatchId(Long transactionId, String settlementBatchId);

    // Fetch all credit transactions belonging to a specific credit account
    List<CreditTransaction> findByCreditAccountId(Long creditAccountId);

    // Fetch all credit transactions that belong to a given settlement batch
    List<CreditTransaction> findBySettlementBatchId(String settlementBatchId);

    // Fetch all credit transactions with a specific status (PENDING, SETTLED, FAILED, etc.)
    List<CreditTransaction> findByStatus(TransactionStatus status);

    // Fetch all credit transactions whose txn_date falls within the given range
    List<CreditTransaction> findByDateRange(LocalDateTime from, LocalDateTime to);

    // Update the status of a specific transaction
    void updateStatus(Long transactionId, TransactionStatus newStatus);
}