package com.iispl.dao;

import com.iispl.entity.InterBankTransaction;
import com.iispl.enums.TransactionStatus;

import java.time.LocalDateTime;
import java.util.List;

public interface InterBankTransactionDao {

    // Persist a new interbank transaction; returns the entity with generated id set
    InterBankTransaction save(InterBankTransaction transaction);

    // Fetch all interbank transactions
    List<InterBankTransaction> findAll();

    // Fetch by primary key; returns null if not found
    InterBankTransaction findById(long id);

    // Assign or update the settlement batch id on a transaction
    void updateBatchId(long id, String settlementBatchId);

    // Fetch all interbank transactions linked to a specific nostro account
    List<InterBankTransaction> findByNostroAccountId(long nostroAccountId);

    // Fetch all transactions with a specific status
    List<InterBankTransaction> findByStatus(TransactionStatus status);

    // Fetch all transactions whose txn_date falls within the given range
    List<InterBankTransaction> findByDateRange(LocalDateTime from, LocalDateTime to);

    // Update the status of a specific transaction
    void updateStatus(long id, TransactionStatus newStatus);
}