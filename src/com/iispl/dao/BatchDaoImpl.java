package com.iispl.dao;

import com.iispl.connectionpool.ConnectionPool;
import com.iispl.dao.BatchDao;
import com.iispl.entity.Batch;
import com.iispl.enums.BatchStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class BatchDaoImpl implements BatchDao {

    // -----------------------------------------------------------------------
    // Write operations
    // -----------------------------------------------------------------------

    @Override
    public void saveBatch(Batch batch) {
        String sql = "INSERT INTO batch (batch_id, batch_date, batch_status, total_transactions, total_amount) "
                   + "VALUES (?, ?, ?, ?, ?)";
        try (Connection con = ConnectionPool.getDataSource().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, batch.getBatchId());
            ps.setObject(2, batch.getBatchDate());
            ps.setString(3, batch.getBatchStatus().name());
            ps.setLong(4, batch.getTotalTransactions());
            ps.setBigDecimal(5, batch.getTotalAmount());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save batch [" + batch.getBatchId() + "]: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteBatch(String batchId) {
        String sql = "DELETE FROM batch WHERE batch_id = ?";
        try (Connection con = ConnectionPool.getDataSource().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, batchId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete batch [" + batchId + "]: " + e.getMessage(), e);
        }
    }

    // -----------------------------------------------------------------------
    // Read operations
    // -----------------------------------------------------------------------

    @Override
    public List<Batch> findAllBatches() {
        String sql = "SELECT batch_id, batch_date, batch_status, total_transactions, total_amount FROM batch";
        List<Batch> batches = new ArrayList<>();
        try (Connection con = ConnectionPool.getDataSource().getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) batches.add(mapRow(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch all batches: " + e.getMessage(), e);
        }
        return batches;
    }

    @Override
    public Batch findBatchById(String batchId) {
        String sql = "SELECT batch_id, batch_date, batch_status, total_transactions, total_amount "
                   + "FROM batch WHERE batch_id = ?";
        try (Connection con = ConnectionPool.getDataSource().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, batchId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find batch [" + batchId + "]: " + e.getMessage(), e);
        }
        return null;
    }

    @Override
    public List<Batch> findBatchesByStatus(BatchStatus status) {
        String sql = "SELECT batch_id, batch_date, batch_status, total_transactions, total_amount "
                   + "FROM batch WHERE batch_status = ?";
        List<Batch> batches = new ArrayList<>();
        try (Connection con = ConnectionPool.getDataSource().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, status.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) batches.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find batches by status [" + status + "]: " + e.getMessage(), e);
        }
        return batches;
    }

    @Override
    public List<Batch> findBatchesByDate(LocalDate batchDate) {
        String sql = "SELECT batch_id, batch_date, batch_status, total_transactions, total_amount "
                   + "FROM batch WHERE batch_date = ?";
        List<Batch> batches = new ArrayList<>();
        try (Connection con = ConnectionPool.getDataSource().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setObject(1, batchDate);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) batches.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find batches by date [" + batchDate + "]: " + e.getMessage(), e);
        }
        return batches;
    }

    @Override
    public List<Batch> findBatchesBySourceSystem(long sourceSystemId) {
        String sql = "SELECT DISTINCT b.batch_id, b.batch_date, b.batch_status, "
                   + "b.total_transactions, b.total_amount "
                   + "FROM batch b "
                   + "JOIN incoming_transaction t ON t.batch_id = b.batch_id "
                   + "WHERE t.source_system_id = ?";
        List<Batch> batches = new ArrayList<>();
        try (Connection con = ConnectionPool.getDataSource().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, sourceSystemId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) batches.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find batches by source system [" + sourceSystemId + "]: " + e.getMessage(), e);
        }
        return batches;
    }

    // -----------------------------------------------------------------------
    // Update operations
    // -----------------------------------------------------------------------

    @Override
    public void updateBatchStatus(String batchId, BatchStatus newStatus) {
        String sql = "UPDATE batch SET batch_status = ? WHERE batch_id = ?";
        try (Connection con = ConnectionPool.getDataSource().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, newStatus.name());
            ps.setString(2, batchId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update status for batch [" + batchId + "]: " + e.getMessage(), e);
        }
    }

    // -----------------------------------------------------------------------
    // Row mapper
    // -----------------------------------------------------------------------

    private Batch mapRow(ResultSet rs) throws SQLException {
        return new Batch(
            rs.getString("batch_id"),
            rs.getObject("batch_date", LocalDate.class),
            BatchStatus.valueOf(rs.getString("batch_status")),
            rs.getLong("total_transactions"),
            rs.getBigDecimal("total_amount"),
            new ArrayList<>()
        );
    }
}