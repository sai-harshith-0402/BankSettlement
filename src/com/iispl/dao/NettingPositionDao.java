package com.iispl.dao;

import com.iispl.entity.NettingPosition;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Data Access contract for NettingPosition entities.
 */
public interface NettingPositionDao {

    // -----------------------------------------------------------------------
    // Write operations
    // -----------------------------------------------------------------------

    /** Persists a new NettingPosition record. */
    void saveNettingPosition(NettingPosition nettingPosition);

    /** Removes a NettingPosition by its positionId. */
    void deleteNettingPosition(long positionId);

    // -----------------------------------------------------------------------
    // Read operations
    // -----------------------------------------------------------------------

    /** Returns every NettingPosition record in the table. */
    List<NettingPosition> findAllNettingPositions();

    /** Returns the NettingPosition matching the given positionId, or null if not found. */
    NettingPosition findNettingPositionById(long positionId);

    /**
     * Returns all NettingPositions for a specific counterparty bank.
     * Used to compute the running net exposure against a given bank.
     */
    List<NettingPosition> findPositionsByCounterpartyBank(long counterpartyBankId);

    /**
     * Returns all NettingPositions whose positionDate falls between
     * the two timestamps (inclusive). Used for end-of-day netting runs.
     */
    List<NettingPosition> findPositionsByDateRange(LocalDateTime from, LocalDateTime to);

    /**
     * Returns the single NettingPosition for a given counterparty bank
     * on a specific settlement date, or null if not found.
     * Used to check whether a netting position already exists before creating one.
     */
    NettingPosition findPositionByCounterpartyAndDate(long counterpartyBankId, LocalDateTime positionDate);

    // -----------------------------------------------------------------------
    // Update operations
    // -----------------------------------------------------------------------

    /**
     * Updates the gross debit, gross credit, and net amounts on an existing
     * NettingPosition. Called when new transactions are netted into a position
     * that was already opened for the day.
     */
    void updateNettingAmounts(long positionId, java.math.BigDecimal grossDebit,
                              java.math.BigDecimal grossCredit, java.math.BigDecimal netAmount);
}