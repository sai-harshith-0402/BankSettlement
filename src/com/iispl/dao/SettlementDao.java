package com.iispl.dao;

import com.iispl.entity.SettlementResult;
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
}