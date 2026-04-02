package com.iispl.dao;

import com.iispl.entity.Bank;
import com.iispl.entity.CreditTransaction;
import com.iispl.entity.DebitTransaction;
import com.iispl.entity.IncomingTransaction;
import com.iispl.entity.InterBankTransaction;
import com.iispl.entity.ReversalTransaction;
import com.iispl.entity.SourceSystem;
import com.iispl.entity.Transaction;
import com.iispl.enums.ChannelType;
import com.iispl.enums.ProcessingStatus;
import com.iispl.enums.SourceType;
import com.iispl.enums.TransactionStatus;
import com.iispl.enums.TransactionType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class TransactionDaoImpl implements TransactionDao {

    // =========================================================================
    // INCOMING TRANSACTION
    // =========================================================================

    @Override
    public long saveIncoming(IncomingTransaction txn, Connection conn) {
        String sql = "INSERT INTO incoming_transaction " +
                     "(source_system_id, txn_type, amount, ingest_timestamp, processing_status, settlement_batch_id, created_at, updated_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?, NOW(), NOW())";

        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, txn.getSourceSystemId());
            ps.setString(2, txn.getTxnType().name());
            ps.setBigDecimal(3, txn.getAmount());
            ps.setTimestamp(4, Timestamp.valueOf(txn.getIngestTimestamp()));
            ps.setString(5, txn.getProcessingStatus().name());
            if (txn.getBatchId() != null) {
                ps.setLong(6, Long.parseLong(txn.getBatchId()));
            } else {
                ps.setNull(6, java.sql.Types.BIGINT);
            }
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    long id = rs.getLong(1);
                    txn.setId(id);
                    return id;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save IncomingTransaction: " + e.getMessage(), e);
        }
        throw new RuntimeException("saveIncoming: no generated key returned");
    }

    @Override
    public void updateIncomingStatus(long incomingId, String processingStatus, Connection conn) {
        String sql = "UPDATE incoming_transaction SET processing_status = ?, updated_at = NOW() WHERE id = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, processingStatus);
            ps.setLong(2, incomingId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update IncomingTransaction status id=" + incomingId, e);
        }
    }

    // =========================================================================
    // CREDIT TRANSACTION  (base table + subtype table)
    // =========================================================================

    @Override
    public long saveCreditTransaction(CreditTransaction txn, Connection conn) {
        return saveBaseTransaction(txn, conn);
    }

    // =========================================================================
    // DEBIT TRANSACTION
    // =========================================================================

    @Override
    public long saveDebitTransaction(DebitTransaction txn, Connection conn) {
        return saveBaseTransaction(txn, conn);
    }

    // =========================================================================
    // INTER-BANK TRANSACTION
    // =========================================================================

    @Override
    public long saveInterBankTransaction(InterBankTransaction txn, Connection conn) {
        return saveBaseTransaction(txn, conn);
    }

    // =========================================================================
    // REVERSAL TRANSACTION
    // =========================================================================

    @Override
    public long saveReversalTransaction(ReversalTransaction txn, Connection conn) {
        return saveBaseTransaction(txn, conn);
    }

    // =========================================================================
    // UPDATE STATUS
    // =========================================================================

    @Override
    public void updateTransactionStatus(long transactionId, TransactionStatus status, Connection conn) {
        String sql = "UPDATE settlement_transaction SET status = ?, updated_at = NOW() WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setLong(2, transactionId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update transaction status id=" + transactionId, e);
        }
    }

    // =========================================================================
    // FIND BY ID
    // =========================================================================

    @Override
    public Transaction findById(long transactionId, Connection conn) {
        String sql = "SELECT t.id, t.source_system_id, t.channel, t.amount, t.txn_date, " +
                     "t.status, t.from_bank_id, t.to_bank_id, t.txn_subtype, t.settlement_batch_id, " +
                     "t.credit_account_id, t.debit_account_id, t.nostro_account_id, " +
                     "t.original_transaction_id, t.reversal_reason " +
                     "FROM settlement_transaction t WHERE t.id = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, transactionId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return (mapBaseRow(rs, conn));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find transaction id=" + transactionId, e);
        }
        return null;
    }

    // =========================================================================
    // FIND BY BATCH
    // =========================================================================

    @Override
    public List<Transaction> findByBatchId(long batchId, Connection conn) {
        String sql = "SELECT t.id, t.source_system_id, t.channel, t.amount, t.txn_date, " +
                     "t.status, t.from_bank_id, t.to_bank_id, t.txn_subtype, t.settlement_batch_id, " +
                     "t.credit_account_id, t.debit_account_id, t.nostro_account_id, " +
                     "t.original_transaction_id, t.reversal_reason " +
                     "FROM settlement_transaction t " +
                     "WHERE t.settlement_batch_id = ?";

        List<Transaction> result = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, batchId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapBaseRow(rs, conn));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch transactions for batchId=" + batchId, e);
        }
        return result;
    }

    // =========================================================================
    // PRIVATE HELPERS
    // =========================================================================

    /**
     * Inserts a row into the base 'transaction' table and returns the generated id.
     * Called by every saveXxx method before inserting the subtype row.
     */
    private long saveBaseTransaction(Transaction txn, Connection conn) {
        String sql = "INSERT INTO settlement_transaction " +
                     "(source_system_id, source_system_ref_id, channel, from_bank_id, to_bank_id, amount, txn_date, " +
                     " status, txn_subtype, settlement_batch_id, " +
                     " credit_account_id, debit_account_id, nostro_account_id, " +
                     " original_transaction_id, reversal_reason, created_at, updated_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())";

        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, txn.getSourceSystemId());
            ps.setLong(2, txn.getSourceSystemId());
            ps.setString(3, txn.getChannel() != null ? txn.getChannel().name() : null);
            ps.setLong(4, txn.getFromBankId());
            ps.setLong(5, txn.getToBankId());
            ps.setBigDecimal(6, txn.getAmount());
            ps.setTimestamp(7, Timestamp.valueOf(txn.getTxnDate()));
            ps.setString(8, txn.getStatus() != null ? txn.getStatus().name() : null);
            ps.setString(9, txn.getClass().getSimpleName());

            // settlement_batch_id
            if (txn.getSettlementBatchId() != null) {
                ps.setLong(10, Long.parseLong(txn.getSettlementBatchId()));
            } else {
                ps.setNull(10, java.sql.Types.BIGINT);
            }

            // subtype-specific columns — null unless applicable
            if (txn instanceof CreditTransaction ct) {
                ps.setLong(11, ct.getCreditAccountId());
                ps.setNull(12, java.sql.Types.BIGINT);
                ps.setNull(13, java.sql.Types.BIGINT);
                ps.setNull(14, java.sql.Types.BIGINT);
                ps.setNull(15, java.sql.Types.VARCHAR);
            } else if (txn instanceof DebitTransaction dt) {
                ps.setNull(11, java.sql.Types.BIGINT);
                ps.setLong(12, dt.getDebitAccountId());
                ps.setNull(13, java.sql.Types.BIGINT);
                ps.setNull(14, java.sql.Types.BIGINT);
                ps.setNull(15, java.sql.Types.VARCHAR);
            } else if (txn instanceof InterBankTransaction it) {
                ps.setNull(11, java.sql.Types.BIGINT);
                ps.setNull(12, java.sql.Types.BIGINT);
                ps.setLong(13, it.getNostroAccountId());
                ps.setNull(14, java.sql.Types.BIGINT);
                ps.setNull(15, java.sql.Types.VARCHAR);
            } else if (txn instanceof ReversalTransaction rt) {
                ps.setNull(11, java.sql.Types.BIGINT);
                ps.setNull(12, java.sql.Types.BIGINT);
                ps.setNull(13, java.sql.Types.BIGINT);
                ps.setLong(14, rt.getOriginalTransactionId());
                ps.setString(15, rt.getReversalReason());
            } else {
                ps.setNull(11, java.sql.Types.BIGINT);
                ps.setNull(12, java.sql.Types.BIGINT);
                ps.setNull(13, java.sql.Types.BIGINT);
                ps.setNull(14, java.sql.Types.BIGINT);
                ps.setNull(15, java.sql.Types.VARCHAR);
            }

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    long id = rs.getLong(1);
                    txn.setId(id);
                    return id;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert base transaction row: " + e.getMessage(), e);
        }
        throw new RuntimeException("saveBaseTransaction: no generated key returned");
    }

    /**
     * Reads a ResultSet row from the 'transaction' table and returns the appropriate
     * Transaction subtype by inspecting the txn_subtype discriminator column.
     * Placeholder Bank/SourceSystem objects are used — BankDao/SourceSystemDao would
     * hydrate them fully in a complete implementation.
     */
    private Transaction mapBaseRow(ResultSet rs, Connection conn) throws SQLException {
        long   id             = rs.getLong("id");
        long   sourceSystemId = rs.getLong("source_system_id");
        String channelStr     = rs.getString("channel");
        long   fromBankId     = rs.getLong("from_bank_id");
        long   toBankId       = rs.getLong("to_bank_id");
        java.math.BigDecimal amount = rs.getBigDecimal("amount");
        Timestamp txnDate     = rs.getTimestamp("txn_date");
        String statusStr      = rs.getString("status");
        String subtype        = rs.getString("txn_subtype");

        // Lightweight placeholder objects — full hydration done by service if needed
        SourceSystem src = new SourceSystem(SourceType.INTERNAL, null, true);
        src.setId(sourceSystemId);
        Bank fromBank = new Bank(String.valueOf(fromBankId), "Bank " + fromBankId, null, true);
        Bank toBank   = new Bank(String.valueOf(toBankId),   "Bank " + toBankId,   null, true);

        ChannelType channel = channelStr != null
                ? ChannelType.valueOf(channelStr) : ChannelType.INTERNAL;
        TransactionStatus status = statusStr != null
                ? TransactionStatus.valueOf(statusStr) : TransactionStatus.INITIATED;
        java.time.LocalDateTime ldt = txnDate != null
                ? txnDate.toLocalDateTime() : java.time.LocalDateTime.now();

        Transaction txn = switch (subtype != null ? subtype : "") {
            case "CreditTransaction" -> new CreditTransaction(
                    src, sourceSystemId, channel, fromBank, toBank, amount, ldt, status,
                    fromBankId, toBankId, rs.getLong("credit_account_id"));
            case "DebitTransaction" -> new DebitTransaction(
                    src, sourceSystemId, channel, fromBank, toBank, amount, ldt, status,
                    fromBankId, toBankId, rs.getLong("debit_account_id"));
            case "ReversalTransaction" -> new ReversalTransaction(
                    src, sourceSystemId, channel, fromBank, toBank, amount, ldt, status,
                    fromBankId, toBankId,
                    rs.getLong("original_transaction_id"), rs.getString("reversal_reason"));
            default -> new InterBankTransaction(
                    src, sourceSystemId, channel, fromBank, toBank, amount, ldt, status,
                    fromBankId, toBankId, rs.getLong("nostro_account_id"));
        };

        txn.setId(id);
        return txn;
    }
}