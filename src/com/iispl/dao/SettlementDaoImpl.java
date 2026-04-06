package com.iispl.dao;

<<<<<<< Updated upstream
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
=======
import com.iispl.connectionpool.ConnectionPool;
import com.iispl.dao.SettlementDao;
import com.iispl.entity.SettlementResult;
import com.iispl.enums.SettlementStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
>>>>>>> Stashed changes
import java.util.ArrayList;
import java.util.List;

public class SettlementDaoImpl implements SettlementDao {

<<<<<<< Updated upstream
    // =========================================================================
    // SAVE
    // =========================================================================

    @Override
    public long save(SettlementResult result, Connection conn) {
        String sql = "INSERT INTO settlement_batch " +
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
        String sql = "UPDATE settlement_batch SET " +
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
    // UPDATE STATUS ONLY
    // =========================================================================

    @Override
    public void updateStatus(long settlementId, BatchStatus status, Connection conn) {
        String sql = "UPDATE settlement_batch SET batch_status = ?, updated_at = NOW() WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setLong(2, settlementId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update SettlementResult status id=" + settlementId, e);
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
                     "FROM settlement_batch WHERE id = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, settlementId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find SettlementResult id=" + settlementId, e);
=======
    // -----------------------------------------------------------------------
    // Write operations
    // -----------------------------------------------------------------------

    @Override
    public void saveSettlement(SettlementResult settlementResult) {
        String sql = "INSERT INTO settlement_result "
                   + "(batch_id, status, settled_count, failed_count, total_settled_amount, processed_at) "
                   + "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection con = ConnectionPool.getDataSource().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, settlementResult.getBatchId());
            ps.setString(2, settlementResult.getStatus().name());
            ps.setInt(3, settlementResult.getSettledCount());
            ps.setInt(4, settlementResult.getFailedCount());
            ps.setBigDecimal(5, settlementResult.getTotalSettledAmount());
            ps.setObject(6, settlementResult.getProcessedAt());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save settlement [" + settlementResult.getBatchId() + "]: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteSettlement(String batchId) {
        String sql = "DELETE FROM settlement_result WHERE batch_id = ?";
        try (Connection con = ConnectionPool.getDataSource().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, batchId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete settlement [" + batchId + "]: " + e.getMessage(), e);
        }
    }

    // -----------------------------------------------------------------------
    // Read operations
    // -----------------------------------------------------------------------

    @Override
    public List<SettlementResult> findAllSettlements() {
        String sql = "SELECT batch_id, status, settled_count, failed_count, total_settled_amount, processed_at "
                   + "FROM settlement_result";
        List<SettlementResult> results = new ArrayList<>();
        try (Connection con = ConnectionPool.getDataSource().getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) results.add(mapRow(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch all settlements: " + e.getMessage(), e);
        }
        return results;
    }

    @Override
    public SettlementResult findSettlementById(String batchId) {
        String sql = "SELECT batch_id, status, settled_count, failed_count, total_settled_amount, processed_at "
                   + "FROM settlement_result WHERE batch_id = ?";
        try (Connection con = ConnectionPool.getDataSource().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, batchId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find settlement [" + batchId + "]: " + e.getMessage(), e);
>>>>>>> Stashed changes
        }
        return null;
    }

<<<<<<< Updated upstream
    // =========================================================================
    // FIND BY DATE
    // =========================================================================

    @Override
    public List<SettlementResult> findByDate(LocalDate batchDate, Connection conn) {
        String sql = "SELECT id, batch_id, batch_date, batch_status, total_transactions, " +
                     "settled_count, failed_count, total_amount, settled_amount, " +
                     "net_amount, exported_file_path, processed_at, created_at, updated_at " +
                     "FROM settlement_batch WHERE batch_date = ? ORDER BY id";

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
=======
    @Override
    public List<SettlementResult> findSettlementsByStatus(SettlementStatus status) {
        String sql = "SELECT batch_id, status, settled_count, failed_count, total_settled_amount, processed_at "
                   + "FROM settlement_result WHERE status = ?";
        List<SettlementResult> results = new ArrayList<>();
        try (Connection con = ConnectionPool.getDataSource().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, status.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) results.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find settlements by status [" + status + "]: " + e.getMessage(), e);
>>>>>>> Stashed changes
        }
        return results;
    }

<<<<<<< Updated upstream
    // =========================================================================
    // FIND BY STATUS
    // =========================================================================

    @Override
    public List<SettlementResult> findByStatus(BatchStatus status, Connection conn) {
        String sql = "SELECT id, batch_id, batch_date, batch_status, total_transactions, " +
                     "settled_count, failed_count, total_amount, settled_amount, " +
                     "net_amount, exported_file_path, processed_at, created_at, updated_at " +
                     "FROM settlement_batch WHERE batch_status = ? ORDER BY batch_date DESC";

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
=======
    @Override
    public List<SettlementResult> findSettlementsByDateRange(LocalDateTime from, LocalDateTime to) {
        String sql = "SELECT batch_id, status, settled_count, failed_count, total_settled_amount, processed_at "
                   + "FROM settlement_result WHERE processed_at BETWEEN ? AND ?";
        List<SettlementResult> results = new ArrayList<>();
        try (Connection con = ConnectionPool.getDataSource().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setObject(1, from);
            ps.setObject(2, to);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) results.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find settlements by date range: " + e.getMessage(), e);
>>>>>>> Stashed changes
        }
        return results;
    }

<<<<<<< Updated upstream
    // =========================================================================
    // PRIVATE HELPER
    // Uses the full 15-arg constructor that matches the SettlementResult entity.
    // The transactions list is not loaded here — fetched separately via
    // TransactionDao if needed.
    // =========================================================================

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
                new ArrayList<>(),                                          // transactions loaded separately
                rs.getInt("total_transactions"),
                rs.getInt("settled_count"),
                rs.getInt("failed_count"),
                rs.getBigDecimal("total_amount"),
                rs.getBigDecimal("settled_amount"),
                rs.getBigDecimal("net_amount"),
                rs.getString("exported_file_path"),
                processedAt != null ? processedAt.toLocalDateTime() : null
=======
    // -----------------------------------------------------------------------
    // Update operations
    // -----------------------------------------------------------------------

    @Override
    public void updateSettlementStatus(String batchId, SettlementStatus newStatus) {
        String sql = "UPDATE settlement_result SET status = ? WHERE batch_id = ?";
        try (Connection con = ConnectionPool.getDataSource().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, newStatus.name());
            ps.setString(2, batchId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update settlement status [" + batchId + "]: " + e.getMessage(), e);
        }
    }

    // -----------------------------------------------------------------------
    // Row mapper
    // -----------------------------------------------------------------------

    private SettlementResult mapRow(ResultSet rs) throws SQLException {
        return new SettlementResult(
            rs.getString("batch_id"),
            SettlementStatus.valueOf(rs.getString("status")),
            rs.getInt("settled_count"),
            rs.getInt("failed_count"),
            rs.getBigDecimal("total_settled_amount"),
            rs.getObject("processed_at", LocalDateTime.class)
>>>>>>> Stashed changes
        );
    }
}