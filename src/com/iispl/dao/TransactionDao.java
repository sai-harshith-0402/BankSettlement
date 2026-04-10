package com.iispl.dao;

import com.iispl.entity.IncomingTransaction;
import com.iispl.enums.ProcessingStatus;

import java.util.List;

public interface TransactionDao {

    // Persist a new incoming transaction; returns the entity with generated id set
    IncomingTransaction save(IncomingTransaction transaction);

    // Persist a transaction, overriding its batchId with the supplied value.
    // Use during batch creation to ensure the FK constraint is satisfied.
    IncomingTransaction save(IncomingTransaction transaction, String batchId);

    // Fetch all incoming transactions
    List<IncomingTransaction> findAll();

    // Fetch by primary key; returns null if not found
    IncomingTransaction findById(long id);

    // Assign or update the batch id on a transaction
    void updateBatchId(long id, String batchId);

    // Fetch all transactions from a specific source system
    List<IncomingTransaction> findBySourceSystemId(long sourceSystemId);

    // Fetch all transactions with a given processing status
    List<IncomingTransaction> findByProcessingStatus(ProcessingStatus status);
}