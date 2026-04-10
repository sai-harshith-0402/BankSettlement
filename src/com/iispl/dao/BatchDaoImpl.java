package com.iispl.dao;

import com.iispl.connectionpool.ConnectionPool;
import com.iispl.entity.Batch;
import com.iispl.entity.IncomingTransaction;
import com.iispl.entity.SourceSystem;
import com.iispl.enums.BatchStatus;
import com.iispl.enums.ChannelType;
import com.iispl.enums.ProcessingStatus;
import com.iispl.enums.SourceType;
import com.iispl.enums.TransactionType;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class BatchDaoImpl implements BatchDao {

    // -------------------- WRITE --------------------

    @Override
    public void saveBatch(Batch batch) {
        String sql = "INSERT INTO batch (batch_id, batch_date, batch_status, total_transactions, total_amount) VALUES (?, ?, ?, ?, ?)";

        try (Connection con = ConnectionPool.getDataSource().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, batch.getBatchId());
            ps.setObject(2, batch.getBatchDate());
            ps.setString(3, batch.getBatchStatus().name());
            ps.setLong(4, batch.getTotalTransactions());
            ps.setBigDecimal(5, batch.getTotalAmount());

            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Failed to save batch [" + batch.getBatchId() + "]", e);
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
            throw new RuntimeException("Failed to delete batch [" + batchId + "]", e);
        }
    }

    // -------------------- READ --------------------

    @Override
    public List<Batch> findAllBatches() {
        String sql = "SELECT * FROM batch";
        List<Batch> list = new ArrayList<>();

        try (Connection con = ConnectionPool.getDataSource().getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) list.add(mapRow(rs));

        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch batches", e);
        }
        return list;
    }

    @Override
    public Batch findBatchById(String batchId) {
        String sql = "SELECT * FROM batch WHERE batch_id = ?";

        try (Connection con = ConnectionPool.getDataSource().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, batchId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch batch", e);
        }
        return null;
    }

    @Override
    public List<Batch> findBatchesByStatus(BatchStatus status) {
        String sql = "SELECT * FROM batch WHERE batch_status = ?";
        List<Batch> list = new ArrayList<>();

        try (Connection con = ConnectionPool.getDataSource().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, status.name());

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch batches by status", e);
        }
        return list;
    }

    @Override
    public List<Batch> findBatchesByDate(LocalDate batchDate) {
        String sql = "SELECT * FROM batch WHERE batch_date = ?";
        List<Batch> list = new ArrayList<>();

        try (Connection con = ConnectionPool.getDataSource().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setObject(1, batchDate);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch batches by date", e);
        }
        return list;
    }

    @Override
    public List<Batch> findBatchesBySourceSystem(long sourceSystemId) {

        String sql = "SELECT DISTINCT b.* "
                   + "FROM batch b "
                   + "JOIN incoming_transaction t ON t.batch_id = b.batch_id "
                   + "WHERE t.source_system_id = ?";

        List<Batch> list = new ArrayList<>();

        try (Connection con = ConnectionPool.getDataSource().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, sourceSystemId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch batches by source system", e);
        }

        return list;
    }

    // -------------------- UPDATE --------------------

    @Override
    public void updateBatchStatus(String batchId, BatchStatus status) {
        String sql = "UPDATE batch SET batch_status = ? WHERE batch_id = ?";

        try (Connection con = ConnectionPool.getDataSource().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, status.name());
            ps.setString(2, batchId);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Failed to update batch status", e);
        }
    }

    // -------------------- MAPPER --------------------

    private Batch mapRow(ResultSet rs) throws SQLException {

        String batchId = rs.getString("batch_id");

        List<IncomingTransaction> transactions = findTransactionsForBatch(batchId);

        return new Batch(
                batchId,
                rs.getObject("batch_date", LocalDate.class),
                BatchStatus.valueOf(rs.getString("batch_status")),
                rs.getLong("total_transactions"),
                rs.getBigDecimal("total_amount"),
                transactions
        );
    }

    // -------------------- FIXED CORE QUERY --------------------

    private List<IncomingTransaction> findTransactionsForBatch(String batchId) {

        String sql = "SELECT t.*, s.file_path "
                   + "FROM incoming_transaction t "
                   + "JOIN source_system s ON t.source_system_id = s.id "
                   + "WHERE t.batch_id = ?";

        List<IncomingTransaction> list = new ArrayList<>();

        try (Connection con = ConnectionPool.getDataSource().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, batchId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapTransactionRow(rs));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException(
                "Failed to load transactions for batch [" + batchId + "]", e);
        }

        return list;
    }

    // -------------------- TRANSACTION MAPPER --------------------

    private IncomingTransaction mapTransactionRow(ResultSet rs) throws SQLException {

        SourceSystem sourceSystem = new SourceSystem(
                rs.getLong("source_system_id"),
                SourceType.valueOf(rs.getString("source_type")),
                rs.getString("file_path")
        );

        Timestamp ts = rs.getTimestamp("ingestion_time_stamp");

        return new IncomingTransaction(
                rs.getLong("incoming_tnx_id"),
                sourceSystem,
                rs.getLong("source_system_id"),
                TransactionType.valueOf(rs.getString("transaction_type")),
                ChannelType.valueOf(rs.getString("channel_type")),
                rs.getString("from_bank_name"),
                rs.getString("to_bank_name"),
                rs.getBigDecimal("amount"),
                ProcessingStatus.valueOf(rs.getString("processing_status")),
                ts != null ? ts.toLocalDateTime() : null,
                rs.getString("batch_id")
        );
    }
}