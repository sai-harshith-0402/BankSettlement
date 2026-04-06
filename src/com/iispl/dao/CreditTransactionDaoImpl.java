package com.iispl.dao;

import com.iispl.entity.Bank;
import com.iispl.entity.CreditTransaction;
import com.iispl.entity.SourceSystem;
import com.iispl.enums.ChannelType;
import com.iispl.enums.TransactionStatus;

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

    private final Connection connection;

    public CreditTransactionDaoImpl(Connection connection) {
        this.connection = connection;
    }

    // =========================================================================
    // SAVE
    // =========================================================================

    @Override
    public CreditTransaction save(CreditTransaction txn) {
        String sql = "INSERT INTO credit_transaction "
                   + "(source_system_id, channel, from_bank_id, to_bank_id, amount, txn_date, "
                   + " status, settlement_batch_id, credit_account_id, created_at, updated_at) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())";

        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, txn.getSourceSystemId());
            ps.setString(2, txn.getChannel().name());
            ps.setLong(3, txn.getFromBankId());
            ps.setLong(4, txn.getToBankId());
            ps.setBigDecimal(5, txn.getAmount());
            ps.setTimestamp(6, Timestamp.valueOf(txn.getTxnDate()));
            ps.setString(7, txn.getStatus().name());
            ps.setString(8, txn.getSettlementBatchId());
            ps.setLong(9, txn.getCreditAccountId());
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    txn.setId(rs.getLong(1));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save CreditTransaction: " + e.getMessage(), e);
        }
        return txn;
    }

    // =========================================================================
    // FIND ALL
    // =========================================================================

    @Override
    public List<CreditTransaction> findAll() {
        String sql = "SELECT * FROM credit_transaction";
        List<CreditTransaction> list = new ArrayList<>();

        try (PreparedStatement ps = connection.prepareStatement(sql);
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
        String sql = "SELECT * FROM credit_transaction WHERE id = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
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
        String sql = "UPDATE credit_transaction SET settlement_batch_id = ?, updated_at = NOW() WHERE id = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, settlementBatchId);
            ps.setLong(2, transactionId);
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
        String sql = "SELECT * FROM credit_transaction WHERE credit_account_id = ?";
        List<CreditTransaction> list = new ArrayList<>();

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
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
        String sql = "SELECT * FROM credit_transaction WHERE settlement_batch_id = ?";
        List<CreditTransaction> list = new ArrayList<>();

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
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
        String sql = "SELECT * FROM credit_transaction WHERE status = ?";
        List<CreditTransaction> list = new ArrayList<>();

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
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
        String sql = "SELECT * FROM credit_transaction WHERE txn_date BETWEEN ? AND ?";
        List<CreditTransaction> list = new ArrayList<>();

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
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
        String sql = "UPDATE credit_transaction SET status = ?, updated_at = NOW() WHERE id = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, newStatus.name());
            ps.setLong(2, transactionId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update status for CreditTransaction id="
                    + transactionId + ": " + e.getMessage(), e);
        }
    }

    // =========================================================================
    // PRIVATE HELPER
    // fromBank, toBank and sourceSystem are hydrated by the service layer;
    // the DAO stores and retrieves only their IDs.
    // =========================================================================

    private CreditTransaction mapRow(ResultSet rs) throws SQLException {
        Timestamp createdAt = rs.getTimestamp("created_at");
        Timestamp updatedAt = rs.getTimestamp("updated_at");

        return new CreditTransaction(
                rs.getLong("id"),
                createdAt != null ? createdAt.toLocalDateTime() : null,
                updatedAt != null ? updatedAt.toLocalDateTime() : null,
                (SourceSystem) null,                            // hydrated by service layer
                rs.getLong("source_system_id"),
                ChannelType.valueOf(rs.getString("channel")),
                (Bank) null,                                    // hydrated by service layer
                (Bank) null,                                    // hydrated by service layer
                rs.getBigDecimal("amount"),
                rs.getTimestamp("txn_date").toLocalDateTime(),
                TransactionStatus.valueOf(rs.getString("status")),
                rs.getLong("from_bank_id"),
                rs.getLong("to_bank_id"),
                rs.getString("settlement_batch_id"),
                rs.getLong("credit_account_id")
        );
    }
}