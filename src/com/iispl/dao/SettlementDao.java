package com.iispl.dao;

import com.iispl.entity.SettlementResult;
<<<<<<< Updated upstream
import com.iispl.enums.BatchStatus;

import java.sql.Connection;
import java.time.LocalDate;
import java.util.List;

public interface SettlementDao {

    /**
     * Inserts a new SettlementResult (batch record) at the start of processing.
     * Returns the generated DB id.
     */
    long save(SettlementResult result, Connection conn);

    /**
     * Updates counters, amounts, status, file path, and processed_at
     * after the batch has finished.
     */
    void update(SettlementResult result, Connection conn);

    /**
     * Updates only the batch_status column — used for quick status transitions
     * (e.g. RUNNING → COMPLETED / FAILED).
     */
    void updateStatus(long settlementId, BatchStatus status, Connection conn);

    /**
     * Finds a SettlementResult by its generated DB id.
     */
    SettlementResult findById(long settlementId, Connection conn);

    /**
     * Returns all settlement batches for a given date.
     */
    List<SettlementResult> findByDate(LocalDate batchDate, Connection conn);

    /**
     * Returns all settlement batches with a given status.
     */
    List<SettlementResult> findByStatus(BatchStatus status, Connection conn);
=======
import com.iispl.enums.SettlementStatus;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Data Access contract for SettlementResult entities.
 */
public interface SettlementDao {

    // -----------------------------------------------------------------------
    // Write operations
    // -----------------------------------------------------------------------

    /** Persists a new SettlementResult record. */
    void saveSettlement(SettlementResult settlementResult);

    /** Removes a SettlementResult record by its batchId. */
    void deleteSettlement(String batchId);

    // -----------------------------------------------------------------------
    // Read operations
    // -----------------------------------------------------------------------

    /** Returns every SettlementResult record in the table. */
    List<SettlementResult> findAllSettlements();

    /** Returns the SettlementResult matching the given batchId, or null if not found. */
    SettlementResult findSettlementById(String batchId);

    /** Returns all SettlementResults with the given SettlementStatus. */
    List<SettlementResult> findSettlementsByStatus(SettlementStatus status);

    /**
     * Returns all SettlementResults processed between the two timestamps (inclusive).
     * Useful for end-of-day reports and audit queries.
     */
    List<SettlementResult> findSettlementsByDateRange(LocalDateTime from, LocalDateTime to);

    // -----------------------------------------------------------------------
    // Update operations
    // -----------------------------------------------------------------------

    /** Updates the status of an existing SettlementResult. */
    void updateSettlementStatus(String batchId, SettlementStatus newStatus);
>>>>>>> Stashed changes
}