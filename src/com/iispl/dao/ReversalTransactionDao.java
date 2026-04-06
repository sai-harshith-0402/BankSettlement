package com.iispl.dao;

import com.iispl.entity.ReversalTransaction;
import com.iispl.enums.TransactionStatus;

import java.time.LocalDateTime;
import java.util.List;

public interface ReversalTransactionDao {

    // Persist a new reversal transaction; returns the entity with generated id set
    ReversalTransaction save(ReversalTransaction transaction);

    // Fetch all reversal transactions
    List<ReversalTransaction> findAll();

    // Fetch by primary key; returns null if not found
    ReversalTransaction findById(long id);

    // Assign or update the settlement batch id on a transaction
    void updateBatchId(long id, String settlementBatchId);

    // Fetch all reversals that reference a given original transaction
    List<ReversalTransaction> findByOriginalTransactionId(long originalTransactionId);

    // Fetch all reversal transactions with a specific status
    List<ReversalTransaction> findByStatus(TransactionStatus status);

    // Fetch all reversal transactions whose txn_date falls within the given range
    List<ReversalTransaction> findByDateRange(LocalDateTime from, LocalDateTime to);

    // Update the status of a specific reversal transaction
    void updateStatus(long id, TransactionStatus newStatus);
}