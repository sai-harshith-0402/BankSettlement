package com.iispl.dao;

import com.iispl.dao.DebitTransactionDao;
import com.iispl.entity.Bank;
import com.iispl.entity.DebitTransaction;
import com.iispl.entity.SourceSystem;
import com.iispl.enums.ChannelType;
import com.iispl.enums.TransactionStatus;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class DebitTransactionDaoImpl implements DebitTransactionDao {

    private final Connection connection;

    public DebitTransactionDaoImpl(Connection connection) {
        this.connection = connection;
    }

    @Override
    public DebitTransaction save(DebitTransaction txn) {
        String sql = "INSERT INTO debit_transaction "
                   + "(source_system_id, channel, from_bank_id, to_bank_id, amount, txn_date, "
                   + " status, settlement_batch_id, debit_account_id, created_at, updated_at) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, txn.getSourceSystemId());
            ps.setString(2, txn.getChannel().name());
            ps.setLong(3, txn.getFromBankId());
            ps.setLong(4, txn.getToBankId());
            ps.setBigDecimal(5, txn.getAmount());
            ps.setTimestamp(6, Timestamp.valueOf(txn.getTxnDate()));
            ps.setString(7, txn.getStatus().name());
            ps.setString(8, txn.getSettlementBatchId());
            ps.setLong(9, txn.getDebitAccountId());
            ps.setTimestamp(10, Timestamp.valueOf(LocalDateTime.now()));
            ps.setTimestamp(11, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    txn.setId(rs.getLong(1));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error saving debit transaction: " + e.getMessage(), e);
        }
        return txn;
    }

    @Override
    public List<DebitTransaction> findAllTransactions() {
        String sql = "SELECT * FROM debit_transaction";
        List<DebitTransaction> list = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching all debit transactions: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public DebitTransaction findById(Long id) {
        String sql = "SELECT * FROM debit_transaction WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding debit transaction by id " + id + ": " + e.getMessage(), e);
        }
        return null;
    }

    @Override
    public void updateBatchId(Long transactionId, String settlementBatchId) {
        String sql = "UPDATE debit_transaction SET settlement_batch_id = ?, updated_at = ? WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, settlementBatchId);
            ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            ps.setLong(3, transactionId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error updating batch id for transaction " + transactionId + ": " + e.getMessage(), e);
        }
    }

    @Override
    public List<DebitTransaction> findByDebitAccountId(Long debitAccountId) {
        String sql = "SELECT * FROM debit_transaction WHERE debit_account_id = ?";
        List<DebitTransaction> list = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, debitAccountId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding transactions for debit account " + debitAccountId + ": " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public List<DebitTransaction> findBySettlementBatchId(String settlementBatchId) {
        String sql = "SELECT * FROM debit_transaction WHERE settlement_batch_id = ?";
        List<DebitTransaction> list = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, settlementBatchId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding transactions for batch " + settlementBatchId + ": " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public List<DebitTransaction> findByStatus(TransactionStatus status) {
        String sql = "SELECT * FROM debit_transaction WHERE status = ?";
        List<DebitTransaction> list = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, status.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding transactions by status " + status + ": " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public List<DebitTransaction> findByDateRange(LocalDateTime from, LocalDateTime to) {
        String sql = "SELECT * FROM debit_transaction WHERE txn_date BETWEEN ? AND ?";
        List<DebitTransaction> list = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(from));
            ps.setTimestamp(2, Timestamp.valueOf(to));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding transactions in date range: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public void updateStatus(Long transactionId, TransactionStatus newStatus) {
        String sql = "UPDATE debit_transaction SET status = ?, updated_at = ? WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, newStatus.name());
            ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            ps.setLong(3, transactionId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error updating status for transaction " + transactionId + ": " + e.getMessage(), e);
        }
    }

    // -----------------------------------------------------------------------
    // Private helper — fromBank, toBank and sourceSystem are hydrated by the
    // service layer; the DAO stores and retrieves only their IDs.
    // -----------------------------------------------------------------------

    private DebitTransaction mapRow(ResultSet rs) throws SQLException {
        return new DebitTransaction(
                rs.getLong("id"),
                rs.getTimestamp("created_at").toLocalDateTime(),
                rs.getTimestamp("updated_at").toLocalDateTime(),
                (SourceSystem) null,
                rs.getLong("source_system_id"),
                ChannelType.valueOf(rs.getString("channel")),
                (Bank) null,
                (Bank) null,
                rs.getBigDecimal("amount"),
                rs.getTimestamp("txn_date").toLocalDateTime(),
                TransactionStatus.valueOf(rs.getString("status")),
                rs.getLong("from_bank_id"),
                rs.getLong("to_bank_id"),
                rs.getString("settlement_batch_id"),
                rs.getLong("debit_account_id")
        );
    }
}