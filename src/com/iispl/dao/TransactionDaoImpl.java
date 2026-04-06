package com.iispl.dao;

import com.iispl.entity.IncomingTransaction;
import com.iispl.entity.SourceSystem;
import com.iispl.enums.ProcessingStatus;
import com.iispl.enums.SourceType;
import com.iispl.enums.TransactionType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class TransactionDaoImpl implements TransactionDao {

    private final Connection connection;

    public TransactionDaoImpl(Connection connection) {
        this.connection = connection;
    }

    // SQL constants
    private static final String INSERT =
            "INSERT INTO incoming_transaction " +
            "(source_system_id, txn_type, amount, ingest_timestamp, processing_status, batch_id, created_at, updated_at) " +
            "VALUES (?, ?, ?, ?, ?, ?, NOW(), NOW())";

    private static final String SELECT_COLS =
            "SELECT t.id, t.source_system_id, t.txn_type, t.amount, t.ingest_timestamp, " +
            "       t.processing_status, t.batch_id, t.created_at, t.updated_at, " +
            "       s.id AS ss_id, s.system_code, s.file_path, s.is_active, " +
            "       s.created_at AS ss_created_at, s.updated_at AS ss_updated_at " +
            "FROM   incoming_transaction t " +
            "JOIN   source_system s ON s.id = t.source_system_id";

    private static final String SELECT_ALL = SELECT_COLS;

    private static final String SELECT_BY_ID = SELECT_COLS +
            " WHERE t.id = ?";

    private static final String SELECT_BY_SOURCE_SYSTEM = SELECT_COLS +
            " WHERE t.source_system_id = ?";

    private static final String SELECT_BY_STATUS = SELECT_COLS +
            " WHERE t.processing_status = ?";

    private static final String UPDATE_BATCH_ID =
            "UPDATE incoming_transaction SET batch_id = ?, updated_at = NOW() WHERE id = ?";

    // =========================================================================
    // SAVE
    // =========================================================================

    @Override
    public IncomingTransaction save(IncomingTransaction transaction) {
        try (PreparedStatement ps = connection.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {

            ps.setLong(1, transaction.getSourceSystemId());
            ps.setString(2, transaction.getTxnType().name());
            ps.setBigDecimal(3, transaction.getAmount());
            ps.setTimestamp(4, Timestamp.valueOf(transaction.getIngestTimestamp()));
            ps.setString(5, transaction.getProcessingStatus().name());

            if (transaction.getBatchId() == null) {
                ps.setNull(6, Types.VARCHAR);
            } else {
                ps.setString(6, transaction.getBatchId());
            }

            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    transaction.setId(keys.getLong(1));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to save IncomingTransaction: " + e.getMessage(), e);
        }
        return transaction;
    }

    // =========================================================================
    // FIND ALL
    // =========================================================================

    @Override
    public List<IncomingTransaction> findAll() {
        List<IncomingTransaction> list = new ArrayList<>();

        try (PreparedStatement ps = connection.prepareStatement(SELECT_ALL);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(mapRow(rs));
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch all IncomingTransactions: " + e.getMessage(), e);
        }
        return list;
    }

    // =========================================================================
    // FIND BY ID
    // =========================================================================

    @Override
    public IncomingTransaction findById(long id) {
        try (PreparedStatement ps = connection.prepareStatement(SELECT_BY_ID)) {

            ps.setLong(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch IncomingTransaction id=" + id + ": " + e.getMessage(), e);
        }
        return null;
    }

    // =========================================================================
    // UPDATE BATCH ID
    // =========================================================================

    @Override
    public void updateBatchId(long id, String batchId) {
        try (PreparedStatement ps = connection.prepareStatement(UPDATE_BATCH_ID)) {

            ps.setString(1, batchId);
            ps.setLong(2, id);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Failed to update batchId for IncomingTransaction id=" + id + ": " + e.getMessage(), e);
        }
    }

    // =========================================================================
    // FIND BY SOURCE SYSTEM ID
    // =========================================================================

    @Override
    public List<IncomingTransaction> findBySourceSystemId(long sourceSystemId) {
        List<IncomingTransaction> list = new ArrayList<>();

        try (PreparedStatement ps = connection.prepareStatement(SELECT_BY_SOURCE_SYSTEM)) {

            ps.setLong(1, sourceSystemId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch IncomingTransactions for sourceSystemId="
                    + sourceSystemId + ": " + e.getMessage(), e);
        }
        return list;
    }

    // =========================================================================
    // FIND BY PROCESSING STATUS
    // =========================================================================

    @Override
    public List<IncomingTransaction> findByProcessingStatus(ProcessingStatus status) {
        List<IncomingTransaction> list = new ArrayList<>();

        try (PreparedStatement ps = connection.prepareStatement(SELECT_BY_STATUS)) {

            ps.setString(1, status.name());

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch IncomingTransactions by status="
                    + status + ": " + e.getMessage(), e);
        }
        return list;
    }

    // =========================================================================
    // PRIVATE HELPER
    // SourceSystem is reconstructed inline from the JOIN — no second query needed
    // =========================================================================

    private IncomingTransaction mapRow(ResultSet rs) throws SQLException {
        Timestamp createdAt   = rs.getTimestamp("created_at");
        Timestamp updatedAt   = rs.getTimestamp("updated_at");
        Timestamp ssCreatedAt = rs.getTimestamp("ss_created_at");
        Timestamp ssUpdatedAt = rs.getTimestamp("ss_updated_at");

        SourceSystem sourceSystem = new SourceSystem(
                rs.getLong("ss_id"),
                ssCreatedAt != null ? ssCreatedAt.toLocalDateTime() : null,
                ssUpdatedAt != null ? ssUpdatedAt.toLocalDateTime() : null,
                SourceType.valueOf(rs.getString("system_code")),
                rs.getString("file_path"),
                rs.getBoolean("is_active")
        );

        IncomingTransaction txn = new IncomingTransaction(
                rs.getLong("id"),
                createdAt != null ? createdAt.toLocalDateTime() : null,
                updatedAt != null ? updatedAt.toLocalDateTime() : null,
                rs.getLong("source_system_id"),
                TransactionType.valueOf(rs.getString("txn_type")),
                rs.getBigDecimal("amount"),
                rs.getTimestamp("ingest_timestamp").toLocalDateTime(),
                ProcessingStatus.valueOf(rs.getString("processing_status")),
                sourceSystem,
                rs.getString("batch_id")
        );
        return txn;
    }
}