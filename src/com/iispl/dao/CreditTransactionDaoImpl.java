package com.iispl.dao;

import com.iispl.connectionpool.ConnectionPool;
import com.iispl.entity.CreditTransaction;
import com.iispl.entity.SourceSystem;
import com.iispl.enums.ChannelType;
import com.iispl.enums.ProcessingStatus;
import com.iispl.enums.SourceType;
import com.iispl.enums.TransactionStatus;
import com.iispl.enums.TransactionType;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class CreditTransactionDaoImpl implements CreditTransactionDao {

    // FIX 1: Removed injected Connection field; use ConnectionPool like DebitTransactionDaoImpl
    private final DataSource dataSource = ConnectionPool.getDataSource();

    // =========================================================================
    // SAVE
    // =========================================================================

    @Override
    public CreditTransaction save(CreditTransaction txn) {
        // FIX 2: Table name changed from debit_transaction → credit_transaction
        // FIX 3: Last column changed from debit_account_id → credit_account_id
        String sql = "INSERT INTO credit_transaction "
                + "(source_system_id, source_system_ref_id, transaction_type, channel_type, "
                + " from_bank_name, to_bank_name, amount, processing_status, "
                + " ingestion_time_stamp, batch_id, credit_account_id) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setLong(1,       txn.getSourceSystem().getSourceSystemId());
            ps.setLong(2,       txn.getIncomingTnxId());
            ps.setString(3,     txn.getTransactionType().name());
            ps.setString(4,     txn.getChannelType().name());
            ps.setString(5,     txn.getFromBankName());
            ps.setString(6,     txn.getToBankName());
            ps.setBigDecimal(7, txn.getAmount());
            ps.setString(8,     txn.getProcessingStatus().name());
            ps.setTimestamp(9,  Timestamp.valueOf(txn.getIngestionTimeStamp()));
            ps.setString(10,    txn.getBatchId());
            // FIX 4: getCreditAccountId() instead of getDebitAccountId()
            ps.setLong(11,      txn.getCreditAccountId());

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    txn.setIncomingTnxId(rs.getLong(1));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error saving credit transaction: " + e.getMessage(), e);
        }
        return txn;
    }

    // =========================================================================
    // FIND ALL
    // =========================================================================

    @Override
    public List<CreditTransaction> findAll() {
        // FIX 5: JOIN with source_system to hydrate SourceSystem in mapRow
        String sql = "SELECT ct.*, ss.source_type, ss.file_path "
                   + "FROM credit_transaction ct "
                   + "JOIN source_system ss ON ct.source_system_id = ss.source_system_id";

        List<CreditTransaction> list = new ArrayList<>();
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(mapRow(rs));
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch all CreditTransactions: " + e.getMessage(), e);
        }
        return list;
    }

    // =========================================================================
    // FIND BY ID
    // =========================================================================

    @Override
    public CreditTransaction findById(Long id) {
        // FIX 6: Correct PK column name incoming_tnx_id (not id) + JOIN for SourceSystem
        String sql = "SELECT ct.*, ss.source_type, ss.file_path "
                   + "FROM credit_transaction ct "
                   + "JOIN source_system ss ON ct.source_system_id = ss.source_system_id "
                   + "WHERE ct.incoming_tnx_id = ?";

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find CreditTransaction id=" + id + ": " + e.getMessage(), e);
        }
        return null;
    }

    // =========================================================================
    // UPDATE BATCH ID
    // =========================================================================

    @Override
    public void updateBatchId(Long transactionId, String settlementBatchId) {
        // FIX 7: Correct PK column name incoming_tnx_id; removed NOW() — no updated_at column in your schema
        String sql = "UPDATE credit_transaction SET batch_id = ? WHERE incoming_tnx_id = ?";

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, settlementBatchId);
            ps.setLong(2,   transactionId);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Failed to update batchId for CreditTransaction id="
                    + transactionId + ": " + e.getMessage(), e);
        }
    }

    // =========================================================================
    // FIND BY CREDIT ACCOUNT ID
    // =========================================================================

    @Override
    public List<CreditTransaction> findByCreditAccountId(Long creditAccountId) {
        String sql = "SELECT ct.*, ss.source_type, ss.file_path "
                   + "FROM credit_transaction ct "
                   + "JOIN source_system ss ON ct.source_system_id = ss.source_system_id "
                   + "WHERE ct.credit_account_id = ?";

        List<CreditTransaction> list = new ArrayList<>();
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, creditAccountId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find CreditTransactions for creditAccountId="
                    + creditAccountId + ": " + e.getMessage(), e);
        }
        return list;
    }

    // =========================================================================
    // FIND BY SETTLEMENT BATCH ID
    // =========================================================================

    @Override
    public List<CreditTransaction> findBySettlementBatchId(String settlementBatchId) {
        String sql = "SELECT ct.*, ss.source_type, ss.file_path "
                   + "FROM credit_transaction ct "
                   + "JOIN source_system ss ON ct.source_system_id = ss.source_system_id "
                   + "WHERE ct.batch_id = ?";

        List<CreditTransaction> list = new ArrayList<>();
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, settlementBatchId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find CreditTransactions for batchId="
                    + settlementBatchId + ": " + e.getMessage(), e);
        }
        return list;
    }

    // =========================================================================
    // FIND BY STATUS
    // =========================================================================

    @Override
    public List<CreditTransaction> findByStatus(TransactionStatus status) {
        // FIX 8: Column name is processing_status (not status) — consistent with your schema
        String sql = "SELECT ct.*, ss.source_type, ss.file_path "
                   + "FROM credit_transaction ct "
                   + "JOIN source_system ss ON ct.source_system_id = ss.source_system_id "
                   + "WHERE ct.processing_status = ?";

        List<CreditTransaction> list = new ArrayList<>();
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, status.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find CreditTransactions by status="
                    + status + ": " + e.getMessage(), e);
        }
        return list;
    }

    // =========================================================================
    // FIND BY DATE RANGE
    // =========================================================================

    @Override
    public List<CreditTransaction> findByDateRange(LocalDateTime from, LocalDateTime to) {
        // FIX 9: Column name is ingestion_time_stamp (not txn_date) — consistent with your schema
        String sql = "SELECT ct.*, ss.source_type, ss.file_path "
                   + "FROM credit_transaction ct "
                   + "JOIN source_system ss ON ct.source_system_id = ss.source_system_id "
                   + "WHERE ct.ingestion_time_stamp BETWEEN ? AND ?";

        List<CreditTransaction> list = new ArrayList<>();
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setTimestamp(1, Timestamp.valueOf(from));
            ps.setTimestamp(2, Timestamp.valueOf(to));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find CreditTransactions in date range: " + e.getMessage(), e);
        }
        return list;
    }

    // =========================================================================
    // UPDATE STATUS
    // =========================================================================

    @Override
    public void updateStatus(Long transactionId, TransactionStatus newStatus) {
        // FIX 10: Column name is processing_status (not status); PK is incoming_tnx_id (not id)
        String sql = "UPDATE credit_transaction SET processing_status = ? WHERE incoming_tnx_id = ?";

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, newStatus.name());
            ps.setLong(2,   transactionId);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Failed to update status for CreditTransaction id="
                    + transactionId + ": " + e.getMessage(), e);
        }
    }

    // =========================================================================
    // PRIVATE HELPER — mapRow
    // =========================================================================

    private CreditTransaction mapRow(ResultSet rs) throws SQLException {
        SourceSystem sourceSystem = new SourceSystem(
                rs.getLong("source_system_id"),
                SourceType.valueOf(rs.getString("source_type")),
                rs.getString("file_path")
        );
        return new CreditTransaction(
                rs.getLong("incoming_tnx_id"),
                sourceSystem,
                rs.getLong("source_system_id"),
                TransactionType.valueOf(rs.getString("transaction_type")),
                ChannelType.valueOf(rs.getString("channel_type")),
                rs.getString("from_bank_name"),
                rs.getString("to_bank_name"),
                rs.getBigDecimal("amount"),
                ProcessingStatus.valueOf(rs.getString("processing_status")),
                rs.getTimestamp("ingestion_time_stamp").toLocalDateTime(),
                rs.getString("batch_id"),
                rs.getLong("credit_account_id")
        );
    }
}