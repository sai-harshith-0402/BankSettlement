package com.iispl.dao;

import com.iispl.entity.Bank;
import com.iispl.entity.InterBankTransaction;
import com.iispl.entity.SourceSystem;
import com.iispl.enums.ChannelType;
import com.iispl.enums.SourceType;
import com.iispl.enums.TransactionStatus;

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

public class InterBankTransactionDaoImpl implements InterBankTransactionDao {

    private final Connection connection;

    public InterBankTransactionDaoImpl(Connection connection) {
        this.connection = connection;
    }

    // SQL constants
    // InterBankTransaction extends Transaction — all fields live in one table.
    // fromBank / toBank objects are hydrated by the service layer; only their IDs are persisted.
    private static final String INSERT =
            "INSERT INTO interbank_transaction " +
            "(source_system_id, channel, from_bank_id, to_bank_id, amount, txn_date, " +
            " status, settlement_batch_id, nostro_account_id, created_at, updated_at) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())";

    private static final String SELECT_COLS =
            "SELECT t.id, t.source_system_id, t.channel, t.from_bank_id, t.to_bank_id, " +
            "       t.amount, t.txn_date, t.status, t.settlement_batch_id, " +
            "       t.nostro_account_id, t.created_at, t.updated_at, " +
            "       s.id AS ss_id, s.system_code, s.file_path, s.is_active, " +
            "       s.created_at AS ss_created_at, s.updated_at AS ss_updated_at " +
            "FROM   interbank_transaction t " +
            "JOIN   source_system s ON s.id = t.source_system_id";

    private static final String SELECT_ALL = SELECT_COLS;

    private static final String SELECT_BY_ID = SELECT_COLS +
            " WHERE t.id = ?";

    private static final String SELECT_BY_NOSTRO = SELECT_COLS +
            " WHERE t.nostro_account_id = ?";

    private static final String SELECT_BY_STATUS = SELECT_COLS +
            " WHERE t.status = ?";

    private static final String SELECT_BY_DATE_RANGE = SELECT_COLS +
            " WHERE t.txn_date BETWEEN ? AND ?";

    private static final String UPDATE_BATCH_ID =
            "UPDATE interbank_transaction SET settlement_batch_id = ?, updated_at = NOW() WHERE id = ?";

    private static final String UPDATE_STATUS =
            "UPDATE interbank_transaction SET status = ?, updated_at = NOW() WHERE id = ?";

    // =========================================================================
    // SAVE
    // =========================================================================

    @Override
    public InterBankTransaction save(InterBankTransaction txn) {
        try (PreparedStatement ps = connection.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {

            ps.setLong(1, txn.getSourceSystemId());
            ps.setString(2, txn.getChannel().name());
            ps.setLong(3, txn.getFromBankId());
            ps.setLong(4, txn.getToBankId());
            ps.setBigDecimal(5, txn.getAmount());
            ps.setTimestamp(6, Timestamp.valueOf(txn.getTxnDate()));
            ps.setString(7, txn.getStatus().name());

            if (txn.getSettlementBatchId() == null) {
                ps.setNull(8, Types.VARCHAR);
            } else {
                ps.setString(8, txn.getSettlementBatchId());
            }

            ps.setLong(9, txn.getNostroAccountId());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    txn.setId(keys.getLong(1));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to save InterBankTransaction: " + e.getMessage(), e);
        }
        return txn;
    }

    // =========================================================================
    // FIND ALL
    // =========================================================================

    @Override
    public List<InterBankTransaction> findAll() {
        List<InterBankTransaction> list = new ArrayList<>();

        try (PreparedStatement ps = connection.prepareStatement(SELECT_ALL);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(mapRow(rs));
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch all InterBankTransactions: " + e.getMessage(), e);
        }
        return list;
    }

    // =========================================================================
    // FIND BY ID
    // =========================================================================

    @Override
    public InterBankTransaction findById(long id) {
        try (PreparedStatement ps = connection.prepareStatement(SELECT_BY_ID)) {

            ps.setLong(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch InterBankTransaction id=" + id + ": " + e.getMessage(), e);
        }
        return null;
    }

    // =========================================================================
    // UPDATE BATCH ID
    // =========================================================================

    @Override
    public void updateBatchId(long id, String settlementBatchId) {
        try (PreparedStatement ps = connection.prepareStatement(UPDATE_BATCH_ID)) {

            ps.setString(1, settlementBatchId);
            ps.setLong(2, id);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Failed to update batchId for InterBankTransaction id=" + id + ": " + e.getMessage(), e);
        }
    }

    // =========================================================================
    // FIND BY NOSTRO ACCOUNT ID
    // =========================================================================

    @Override
    public List<InterBankTransaction> findByNostroAccountId(long nostroAccountId) {
        List<InterBankTransaction> list = new ArrayList<>();

        try (PreparedStatement ps = connection.prepareStatement(SELECT_BY_NOSTRO)) {

            ps.setLong(1, nostroAccountId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch InterBankTransactions for nostroAccountId="
                    + nostroAccountId + ": " + e.getMessage(), e);
        }
        return list;
    }

    // =========================================================================
    // FIND BY STATUS
    // =========================================================================

    @Override
    public List<InterBankTransaction> findByStatus(TransactionStatus status) {
        List<InterBankTransaction> list = new ArrayList<>();

        try (PreparedStatement ps = connection.prepareStatement(SELECT_BY_STATUS)) {

            ps.setString(1, status.name());

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch InterBankTransactions by status="
                    + status + ": " + e.getMessage(), e);
        }
        return list;
    }

    // =========================================================================
    // FIND BY DATE RANGE
    // =========================================================================

    @Override
    public List<InterBankTransaction> findByDateRange(LocalDateTime from, LocalDateTime to) {
        List<InterBankTransaction> list = new ArrayList<>();

        try (PreparedStatement ps = connection.prepareStatement(SELECT_BY_DATE_RANGE)) {

            ps.setTimestamp(1, Timestamp.valueOf(from));
            ps.setTimestamp(2, Timestamp.valueOf(to));

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch InterBankTransactions in date range: " + e.getMessage(), e);
        }
        return list;
    }

    // =========================================================================
    // UPDATE STATUS
    // =========================================================================

    @Override
    public void updateStatus(long id, TransactionStatus newStatus) {
        try (PreparedStatement ps = connection.prepareStatement(UPDATE_STATUS)) {

            ps.setString(1, newStatus.name());
            ps.setLong(2, id);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Failed to update status for InterBankTransaction id=" + id + ": " + e.getMessage(), e);
        }
    }

    // =========================================================================
    // PRIVATE HELPER
    // fromBank / toBank objects are hydrated by the service layer — passed as null here.
    // SourceSystem is reconstructed inline from the JOIN.
    // =========================================================================

    private InterBankTransaction mapRow(ResultSet rs) throws SQLException {
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

        return new InterBankTransaction(
                rs.getLong("id"),
                createdAt != null ? createdAt.toLocalDateTime() : null,
                updatedAt != null ? updatedAt.toLocalDateTime() : null,
                sourceSystem,
                rs.getLong("source_system_id"),
                ChannelType.valueOf(rs.getString("channel")),
                (Bank) null,                                // hydrated by service layer
                (Bank) null,                                // hydrated by service layer
                rs.getBigDecimal("amount"),
                rs.getTimestamp("txn_date").toLocalDateTime(),
                TransactionStatus.valueOf(rs.getString("status")),
                rs.getLong("from_bank_id"),
                rs.getLong("to_bank_id"),
                rs.getString("settlement_batch_id"),
                rs.getLong("nostro_account_id")
        );
    }
}