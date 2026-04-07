package com.iispl.dao;

import com.iispl.connectionpool.ConnectionPool;
import com.iispl.dao.SettlementDao;
import com.iispl.entity.SettlementResult;
import com.iispl.enums.SettlementStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class SettlementDaoImpl implements SettlementDao {

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
        }
        return null;
    }

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
        }
        return results;
    }

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
        }
        return results;
    }

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
        );
    }
}