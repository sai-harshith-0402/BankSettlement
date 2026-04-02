package com.iispl.dao;

import com.iispl.entity.SettlementResult;
import com.iispl.enums.BatchStatus;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class SettlementDaoImpl implements SettlementDao {

    // =========================================================================
    // SAVE
    // =========================================================================

    // FIX: Table was "settlement_batch" — corrected to "settlement_result"
    //      to match the schema and entity class name.
    @Override
    public long save(SettlementResult result, Connection conn) {
        String sql = "INSERT INTO settlement_result " +
                     "(batch_id, batch_date, batch_status, total_transactions, " +
                     " settled_count, failed_count, total_amount, settled_amount, " +
                     " net_amount, exported_file_path, processed_at, created_at, updated_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())";

        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, result.getBatchId());
            ps.setDate(2, Date.valueOf(result.getBatchDate()));
            ps.setString(3, result.getBatchStatus() != null
                    ? result.getBatchStatus().name() : BatchStatus.RUNNING.name());
            ps.setInt(4, result.getTotalTransactions());
            ps.setInt(5, result.getSettledCount());
            ps.setInt(6, result.getFailedCount());
            ps.setBigDecimal(7, result.getTotalAmount());
            ps.setBigDecimal(8, result.getSettledAmount());
            ps.setBigDecimal(9, result.getNetAmount());
            ps.setString(10, result.getExportedFilePath());
            ps.setTimestamp(11, result.getProcessedAt() != null
                    ? Timestamp.valueOf(result.getProcessedAt()) : null);
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    long id = rs.getLong(1);
                    result.setId(id);
                    return id;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save SettlementResult batchId="
                    + result.getBatchId() + ": " + e.getMessage(), e);
        }
        throw new RuntimeException("save SettlementResult: no generated key returned");
    }

    // =========================================================================
    // UPDATE
    // =========================================================================

    @Override
    public void update(SettlementResult result, Connection conn) {
        String sql = "UPDATE settlement_result SET " +
                     "batch_status = ?, total_transactions = ?, settled_count = ?, " +
                     "failed_count = ?, total_amount = ?, settled_amount = ?, " +
                     "net_amount = ?, exported_file_path = ?, processed_at = ?, updated_at = NOW() " +
                     "WHERE id = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, result.getBatchStatus() != null ? result.getBatchStatus().name() : null);
            ps.setInt(2, result.getTotalTransactions());
            ps.setInt(3, result.getSettledCount());
            ps.setInt(4, result.getFailedCount());
            ps.setBigDecimal(5, result.getTotalAmount());
            ps.setBigDecimal(6, result.getSettledAmount());
            ps.setBigDecimal(7, result.getNetAmount());
            ps.setString(8, result.getExportedFilePath());
            ps.setTimestamp(9, result.getProcessedAt() != null
                    ? Timestamp.valueOf(result.getProcessedAt()) : null);
            ps.setLong(10, result.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update SettlementResult id="
                    + result.getId() + ": " + e.getMessage(), e);
        }
    }

    // =========================================================================
    // UPDATE STATUS only
    // =========================================================================

    @Override
    public void updateStatus(long settlementId, BatchStatus status, Connection conn) {
        String sql = "UPDATE settlement_result SET batch_status = ?, updated_at = NOW() WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setLong(2, settlementId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update SettlementResult status id="
                    + settlementId, e);
        }
    }

    // =========================================================================
    // FIND BY ID
    // =========================================================================

    @Override
    public SettlementResult findById(long settlementId, Connection conn) {
        String sql = "SELECT id, batch_id, batch_date, batch_status, total_transactions, " +
                     "settled_count, failed_count, total_amount, settled_amount, " +
                     "net_amount, exported_file_path, processed_at, created_at, updated_at " +
                     "FROM settlement_result WHERE id = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, settlementId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find SettlementResult id=" + settlementId, e);
        }
        return null;
    }

    // =========================================================================
    // FIND BY DATE
    // =========================================================================

    @Override
    public List<SettlementResult> findByDate(LocalDate batchDate, Connection conn) {
        String sql = "SELECT id, batch_id, batch_date, batch_status, total_transactions, " +
                     "settled_count, failed_count, total_amount, settled_amount, " +
                     "net_amount, exported_file_path, processed_at, created_at, updated_at " +
                     "FROM settlement_result WHERE batch_date = ? ORDER BY id";

        List<SettlementResult> results = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(batchDate));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find SettlementResult for date=" + batchDate, e);
        }
        return results;
    }

    // =========================================================================
    // FIND BY STATUS
    // =========================================================================

    @Override
    public List<SettlementResult> findByStatus(BatchStatus status, Connection conn) {
        String sql = "SELECT id, batch_id, batch_date, batch_status, total_transactions, " +
                     "settled_count, failed_count, total_amount, settled_amount, " +
                     "net_amount, exported_file_path, processed_at, created_at, updated_at " +
                     "FROM settlement_result WHERE batch_status = ? ORDER BY batch_date DESC";

        List<SettlementResult> results = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find SettlementResult status=" + status, e);
        }
        return results;
    }

    // =========================================================================
    // PRIVATE HELPER
    // =========================================================================

    // FIX: Old code called new SettlementResult(batchId, batchDate) — 2-arg constructor
    //      that no longer exists. SettlementResult now requires the full
    //      (Long id, LocalDateTime createdAt, LocalDateTime updatedAt, String batchId,
    //       LocalDate batchDate, BatchStatus batchStatus, List<Transaction> transactions,
    //       int totalTransactions, int settledCount, int failedCount,
    //       BigDecimal totalAmount, BigDecimal settledAmount, BigDecimal netAmount,
    //       String exportedFilePath, LocalDateTime processedAt).
    //      Also added created_at, updated_at to every SELECT.
    //      transactions list is not loaded here — loaded separately when needed.
    private SettlementResult mapRow(ResultSet rs) throws SQLException {
        Timestamp createdAt   = rs.getTimestamp("created_at");
        Timestamp updatedAt   = rs.getTimestamp("updated_at");
        Timestamp processedAt = rs.getTimestamp("processed_at");

        return new SettlementResult(
                rs.getLong("id"),
                createdAt   != null ? createdAt.toLocalDateTime()   : null,
                updatedAt   != null ? updatedAt.toLocalDateTime()   : null,
                rs.getString("batch_id"),
                rs.getDate("batch_date").toLocalDate(),
                BatchStatus.valueOf(rs.getString("batch_status")),
                null,   // transactions — loaded separately via settlement_transaction join table
                rs.getInt("total_transactions"),
                rs.getInt("settled_count"),
                rs.getInt("failed_count"),
                rs.getBigDecimal("total_amount"),
                rs.getBigDecimal("settled_amount"),
                rs.getBigDecimal("net_amount"),
                rs.getString("exported_file_path"),
                processedAt != null ? processedAt.toLocalDateTime() : null
        );
    }
}