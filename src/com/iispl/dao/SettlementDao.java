package com.iispl.dao;

import com.iispl.entity.SettlementResult;
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
}