package com.iispl.dao;

import com.iispl.entity.ReconciliationEntry;
import com.iispl.enums.ReconStatus;

import java.time.LocalDate;
import java.util.List;

/**
 * Data Access contract for ReconciliationEntry entities.
 */
public interface ReconciliationEntryDao {

    // -----------------------------------------------------------------------
    // Write operations
    // -----------------------------------------------------------------------

    /** Persists a new ReconciliationEntry record. */
    void saveReconciliationEntry(ReconciliationEntry entry);

    /** Removes a ReconciliationEntry by its entryId. */
    void deleteReconciliationEntry(long entryId);

    // -----------------------------------------------------------------------
    // Read operations
    // -----------------------------------------------------------------------

    /** Returns every ReconciliationEntry record in the table. */
    List<ReconciliationEntry> findAllReconciliationEntries();

    /** Returns the ReconciliationEntry matching the given entryId, or null if not found. */
    ReconciliationEntry findReconciliationEntryById(long entryId);

    /** Returns all ReconciliationEntries for a specific account. */
    List<ReconciliationEntry> findEntriesByAccountId(long accountId);

    /** Returns all ReconciliationEntries reconciled on the given date. */
    List<ReconciliationEntry> findEntriesByDate(LocalDate reconciliationDate);

    /**
     * Returns all ReconciliationEntries with the given ReconStatus.
     * Useful for finding all UNMATCHED or PENDING entries that need attention.
     */
    List<ReconciliationEntry> findEntriesByStatus(ReconStatus reconStatus);

    /**
     * Returns all ReconciliationEntries for a specific account on a specific date.
     * Used during the daily reconciliation run to check one account's position.
     */
    List<ReconciliationEntry> findEntriesByAccountAndDate(long accountId, LocalDate reconciliationDate);

    // -----------------------------------------------------------------------
    // Update operations
    // -----------------------------------------------------------------------

    /** Updates the ReconStatus of an existing entry. */
    void updateReconStatus(long entryId, ReconStatus newStatus);
}