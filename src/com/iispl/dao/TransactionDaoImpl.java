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
                     "(source_system_id, txn_type, amount, ingest_timestamp, processing_status, created_at, updated_at) " +
                     "VALUES (?, ?, ?, ?, ?, NOW(), NOW())";

        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, txn.getSourceSystemId());
            ps.setString(2, txn.getTxnType().name());
            ps.setBigDecimal(3, txn.getAmount());
            ps.setTimestamp(4, Timestamp.valueOf(txn.getIngestTimestamp()));
            ps.setString(5, txn.getProcessingStatus().name());
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
        long baseId = saveBaseTransaction(txn, conn);

        String sql = "INSERT INTO credit_transaction (id, credit_account_id) VALUES (?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, baseId);
            ps.setLong(2, txn.getCreditAccountId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save CreditTransaction subtype row id=" + baseId, e);
        }
        return baseId;
    }

    // =========================================================================
    // DEBIT TRANSACTION
    // =========================================================================

    @Override
    public long saveDebitTransaction(DebitTransaction txn, Connection conn) {
        long baseId = saveBaseTransaction(txn, conn);

        String sql = "INSERT INTO debit_transaction (id, debit_account_id) VALUES (?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, baseId);
            ps.setLong(2, txn.getDebitAccountId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save DebitTransaction subtype row id=" + baseId, e);
        }
        return baseId;
    }

    // =========================================================================
    // INTER-BANK TRANSACTION
    // =========================================================================

    @Override
    public long saveInterBankTransaction(InterBankTransaction txn, Connection conn) {
        long baseId = saveBaseTransaction(txn, conn);

        String sql = "INSERT INTO inter_bank_transaction (id, nostro_account_id) VALUES (?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, baseId);
            ps.setLong(2, txn.getNostroAccountId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save InterBankTransaction subtype row id=" + baseId, e);
        }
        return baseId;
    }

    // =========================================================================
    // REVERSAL TRANSACTION
    // =========================================================================

    @Override
    public long saveReversalTransaction(ReversalTransaction txn, Connection conn) {
        long baseId = saveBaseTransaction(txn, conn);

        String sql = "INSERT INTO reversal_transaction (id, original_transaction_id, reversal_reason) VALUES (?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, baseId);
            ps.setLong(2, txn.getOriginalTransactionId());
            ps.setString(3, txn.getReversalReason());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save ReversalTransaction subtype row id=" + baseId, e);
        }
        return baseId;
    }

    // =========================================================================
    // UPDATE STATUS
    // =========================================================================

    @Override
    public void updateTransactionStatus(long transactionId, TransactionStatus status, Connection conn) {
        String sql = "UPDATE transaction SET status = ?, updated_at = NOW() WHERE id = ?";
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
                     "t.status, t.from_bank_id, t.to_bank_id, t.txn_subtype " +
                     "FROM transaction t WHERE t.id = ?";

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
                     "t.status, t.from_bank_id, t.to_bank_id, t.txn_subtype " +
                     "FROM transaction t " +
                     "JOIN settlement_batch_transaction sbt ON sbt.transaction_id = t.id " +
                     "WHERE sbt.batch_id = ?";

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
        String sql = "INSERT INTO transaction " +
                     "(source_system_id, channel, from_bank_id, to_bank_id, amount, txn_date, " +
                     " status, txn_subtype, created_at, updated_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())";

        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, txn.getSourceSystemId());
            ps.setString(2, txn.getChannel() != null ? txn.getChannel().name() : null);
            ps.setLong(3, txn.getFromBankId());
            ps.setLong(4, txn.getToBankId());
            ps.setBigDecimal(5, txn.getAmount());
            ps.setTimestamp(6, Timestamp.valueOf(txn.getTxnDate()));
            ps.setString(7, txn.getStatus() != null ? txn.getStatus().name() : null);
            ps.setString(8, txn.getClass().getSimpleName());
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
        String amountStr      = rs.getString("amount");
        Timestamp txnDate     = rs.getTimestamp("txn_date");
        String statusStr      = rs.getString("status");
        String subtype        = rs.getString("txn_subtype");

        // Lightweight placeholder objects — full hydration done by service if needed
        SourceSystem src = new SourceSystem(sourceSystemId, SourceType.INTERNAL, null, true);
        Bank fromBank    = new Bank(String.valueOf(fromBankId), "Bank " + fromBankId, null, true);
        Bank toBank      = new Bank(String.valueOf(toBankId),   "Bank " + toBankId,   null, true);

        ChannelType channel = channelStr != null
                ? ChannelType.valueOf(channelStr) : ChannelType.INTERNAL;
        TransactionStatus status = statusStr != null
                ? TransactionStatus.valueOf(statusStr) : TransactionStatus.INITIATED;
        java.math.BigDecimal amount = new java.math.BigDecimal(amountStr != null ? amountStr : "0");
        java.time.LocalDateTime ldt = txnDate != null ? txnDate.toLocalDateTime() : java.time.LocalDateTime.now();

        Transaction txn = switch (subtype != null ? subtype : "") {
            case "CreditTransaction" -> {
                long creditAccId = fetchCreditAccountId(id, conn);
                yield new CreditTransaction(src, sourceSystemId, channel,
                        fromBank, toBank, amount, ldt, status, fromBankId, toBankId, creditAccId);
            }
            case "DebitTransaction" -> {
                long debitAccId = fetchDebitAccountId(id, conn);
                yield new DebitTransaction(src, sourceSystemId, channel,
                        fromBank, toBank, amount, ldt, status, fromBankId, toBankId, debitAccId);
            }
            case "ReversalTransaction" -> {
                Object[] rev = fetchReversalFields(id, conn);
                yield new ReversalTransaction(src, sourceSystemId, channel,
                        fromBank, toBank, amount, ldt, status, fromBankId, toBankId,
                        (long) rev[0], (String) rev[1]);
            }
            default -> {
                long nostroAccId = fetchNostroAccountId(id, conn);
                yield new InterBankTransaction(src, sourceSystemId, channel,
                        fromBank, toBank, amount, ldt, status, fromBankId, toBankId, nostroAccId);
            }
        };

        txn.setId(id);
        return txn;
    }

    private long fetchCreditAccountId(long txnId, Connection conn) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT credit_account_id FROM credit_transaction WHERE id = ?")) {
            ps.setLong(1, txnId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong("credit_account_id") : 0L;
            }
        }
    }

    private long fetchDebitAccountId(long txnId, Connection conn) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT debit_account_id FROM debit_transaction WHERE id = ?")) {
            ps.setLong(1, txnId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong("debit_account_id") : 0L;
            }
        }
    }

    private long fetchNostroAccountId(long txnId, Connection conn) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT nostro_account_id FROM inter_bank_transaction WHERE id = ?")) {
            ps.setLong(1, txnId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong("nostro_account_id") : 0L;
            }
        }
    }

    private Object[] fetchReversalFields(long txnId, Connection conn) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT original_transaction_id, reversal_reason FROM reversal_transaction WHERE id = ?")) {
            ps.setLong(1, txnId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Object[]{ rs.getLong("original_transaction_id"),
                                         rs.getString("reversal_reason") };
                }
            }
        }
        return new Object[]{ 0L, "" };
    }
}