package com.iispl.dao;

import com.iispl.entity.Batch;
import com.iispl.enums.BatchStatus;

import java.time.LocalDate;
import java.util.List;

/**
 * Data Access contract for Batch entities.
 */
public interface BatchDao {

    // -----------------------------------------------------------------------
    // Write operations
    // -----------------------------------------------------------------------

    /** Persists a new Batch record. */
    void saveBatch(Batch batch);

    /** Removes a Batch record by its batchId. */
    void deleteBatch(String batchId);

    // -----------------------------------------------------------------------
    // Read operations
    // -----------------------------------------------------------------------

    /** Returns every Batch record in the table. */
    List<Batch> findAllBatches();

    /** Returns the Batch matching the given batchId, or null if not found. */
    Batch findBatchById(String batchId);

    /** Returns all Batches with the given BatchStatus (e.g. PENDING, COMPLETED). */
    List<Batch> findBatchesByStatus(BatchStatus status);

    /** Returns all Batches whose batchDate falls on the given date. */
    List<Batch> findBatchesByDate(LocalDate batchDate);

    /** Returns all Batches belonging to the given sourceSystemId. */
    List<Batch> findBatchesBySourceSystem(long sourceSystemId);

    // -----------------------------------------------------------------------
    // Update operations
    // -----------------------------------------------------------------------

    /** Updates the status of an existing Batch. */
    void updateBatchStatus(String batchId, BatchStatus newStatus);
}