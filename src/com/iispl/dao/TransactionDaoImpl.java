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
import com.iispl.enums.SourceType;
import com.iispl.enums.TransactionStatus;
import com.iispl.enums.TransactionType;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class TransactionDaoImpl implements TransactionDao {

    // =========================================================================
    // INCOMING TRANSACTION
    // =========================================================================

    // FIX: Column "settlement_batch_id" renamed to "batch_id" to match
    //      incoming_transaction table in schema and IncomingTransaction.getBatchId().
    @Override
    public long saveIncoming(IncomingTransaction txn, Connection conn) {
        String sql = "INSERT INTO incoming_transaction " +
                     "(source_system_id, txn_type, amount, ingest_timestamp, " +
                     " processing_status, batch_id, created_at, updated_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?, NOW(), NOW())";

        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, txn.getSourceSystemId());
            ps.setString(2, txn.getTxnType().name());
            ps.setBigDecimal(3, txn.getAmount());
            ps.setTimestamp(4, Timestamp.valueOf(txn.getIngestTimestamp()));
            ps.setString(5, txn.getProcessingStatus().name());
            if (txn.getBatchId() != null) {
                ps.setString(6, txn.getBatchId());
            } else {
                ps.setNull(6, java.sql.Types.VARCHAR);
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
    // SAVE SUBTYPES — all delegate to saveBaseTransaction
    // =========================================================================

    @Override
    public long saveCreditTransaction(CreditTransaction txn, Connection conn) {
        return saveBaseTransaction(txn, conn);
    }

    @Override
    public long saveDebitTransaction(DebitTransaction txn, Connection conn) {
        return saveBaseTransaction(txn, conn);
    }

    @Override
    public long saveInterBankTransaction(InterBankTransaction txn, Connection conn) {
        return saveBaseTransaction(txn, conn);
    }

    @Override
    public long saveReversalTransaction(ReversalTransaction txn, Connection conn) {
        return saveBaseTransaction(txn, conn);
    }

    // =========================================================================
    // UPDATE STATUS
    // =========================================================================

    // FIX: Table was "settlement_transaction" — corrected to "transaction"
    //      to match the schema. settlement_transaction is the join table,
    //      not the main transaction table.
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

    // FIX: Table was "settlement_transaction" — corrected to "transaction".
    //      Added created_at, updated_at, account_id to SELECT.
    //      txn_type replaces txn_subtype as the discriminator column name (matches schema).
    @Override
    public Transaction findById(long transactionId, Connection conn) {
        String sql = "SELECT id, source_system_id, channel, amount, txn_date, status, " +
                     "from_bank_id, to_bank_id, txn_type, settlement_batch_id, account_id, " +
                     "credit_account_id, debit_account_id, nostro_account_id, " +
                     "original_transaction_id, reversal_reason, created_at, updated_at " +
                     "FROM transaction WHERE id = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, transactionId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapBaseRow(rs);
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

    // FIX: Table corrected to "transaction"; join with settlement_transaction
    //      to filter by batch. settlement_batch_id stored as VARCHAR on transaction
    //      row to match entity field type (String settlementBatchId).
    @Override
    public List<Transaction> findByBatchId(long batchId, Connection conn) {
        String sql = "SELECT t.id, t.source_system_id, t.channel, t.amount, t.txn_date, t.status, " +
                     "t.from_bank_id, t.to_bank_id, t.txn_type, t.settlement_batch_id, t.account_id, " +
                     "t.credit_account_id, t.debit_account_id, t.nostro_account_id, " +
                     "t.original_transaction_id, t.reversal_reason, t.created_at, t.updated_at " +
                     "FROM transaction t " +
                     "INNER JOIN settlement_transaction st ON st.transaction_id = t.id " +
                     "WHERE st.settlement_id = ?";

        List<Transaction> result = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, batchId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapBaseRow(rs));
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

    // FIX 1: Table corrected from "settlement_transaction" to "transaction".
    // FIX 2: Removed bogus "source_system_ref_id" duplicate column — only
    //        source_system_id exists on the transaction table.
    // FIX 3: Added account_id column to INSERT (required NOT NULL FK on transaction table).
    // FIX 4: settlement_batch_id is VARCHAR on the entity (String), so use setString not setLong.
    // FIX 5: txn_type replaces txn_subtype as discriminator column name.
    private long saveBaseTransaction(Transaction txn, Connection conn) {
        String sql = "INSERT INTO transaction " +
                     "(source_system_id, account_id, channel, from_bank_id, to_bank_id, " +
                     " amount, txn_date, status, txn_type, settlement_batch_id, " +
                     " credit_account_id, debit_account_id, nostro_account_id, " +
                     " original_transaction_id, reversal_reason, created_at, updated_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())";

        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, txn.getSourceSystemId());

            // FIX: account_id is required. Resolved from the subtype-specific account FK.
            long accountId = resolveAccountId(txn);
            ps.setLong(2, accountId);

            ps.setString(3, txn.getChannel() != null ? txn.getChannel().name() : null);
            ps.setLong(4, txn.getFromBankId());
            ps.setLong(5, txn.getToBankId());
            ps.setBigDecimal(6, txn.getAmount());
            ps.setTimestamp(7, Timestamp.valueOf(txn.getTxnDate()));
            ps.setString(8, txn.getStatus() != null ? txn.getStatus().name() : null);
            ps.setString(9, txn.getClass().getSimpleName()
                    .replace("Transaction", "").toUpperCase()); // e.g. "CREDIT"

            // settlement_batch_id is a String field on the entity
            if (txn.getSettlementBatchId() != null) {
                ps.setString(10, txn.getSettlementBatchId());
            } else {
                ps.setNull(10, java.sql.Types.VARCHAR);
            }

            // subtype-specific nullable columns
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
            throw new RuntimeException("Failed to insert transaction row: " + e.getMessage(), e);
        }
        throw new RuntimeException("saveBaseTransaction: no generated key returned");
    }

    // Derives the primary account_id from whichever subtype-specific FK is present.
    private long resolveAccountId(Transaction txn) {
        if (txn instanceof CreditTransaction ct)    return ct.getCreditAccountId();
        if (txn instanceof DebitTransaction dt)      return dt.getDebitAccountId();
        if (txn instanceof InterBankTransaction it)  return it.getNostroAccountId();
        if (txn instanceof ReversalTransaction rt)   return rt.getOriginalTransactionId();
        throw new IllegalArgumentException("Unknown transaction subtype: " + txn.getClass());
    }

    // FIX: All entity constructors updated to full-arg form matching new entities.
    //      SourceSystem(Long id, LocalDateTime createdAt, LocalDateTime updatedAt,
    //                   SourceType systemCode, String filePath, boolean isActive)
    //      Bank(Long id, LocalDateTime createdAt, LocalDateTime updatedAt,
    //           String bankCode, String bankName, String ifscCode, boolean isActive)
    //      CreditTransaction / DebitTransaction / InterBankTransaction / ReversalTransaction
    //      all require (Long id, LocalDateTime createdAt, LocalDateTime updatedAt,
    //                   SourceSystem, long sourceSystemId, ChannelType, Bank fromBank,
    //                   Bank toBank, BigDecimal amount, LocalDateTime txnDate,
    //                   TransactionStatus status, long fromBankId, long toBankId,
    //                   String settlementBatchId, <subtype-specific args>)
    //      txn_subtype column renamed to txn_type in SELECT to match schema.
    private Transaction mapBaseRow(ResultSet rs) throws SQLException {
        long         id             = rs.getLong("id");
        long         sourceSystemId = rs.getLong("source_system_id");
        String       channelStr     = rs.getString("channel");
        long         fromBankId     = rs.getLong("from_bank_id");
        long         toBankId       = rs.getLong("to_bank_id");
        BigDecimal   amount         = rs.getBigDecimal("amount");
        Timestamp    txnTs          = rs.getTimestamp("txn_date");
        String       statusStr      = rs.getString("status");
        String       txnType        = rs.getString("txn_type");
        String       batchId        = rs.getString("settlement_batch_id");
        Timestamp    createdAt      = rs.getTimestamp("created_at");
        Timestamp    updatedAt      = rs.getTimestamp("updated_at");

        LocalDateTime createdLdt = createdAt != null ? createdAt.toLocalDateTime() : null;
        LocalDateTime updatedLdt = updatedAt != null ? updatedAt.toLocalDateTime() : null;
        LocalDateTime txnDate    = txnTs     != null ? txnTs.toLocalDateTime()     : LocalDateTime.now();

        // Lightweight placeholder objects — fully hydrated by service layer when needed
        SourceSystem src = new SourceSystem(
                sourceSystemId, createdLdt, updatedLdt, SourceType.CBS, null, true);
        Bank fromBank = new Bank(
                fromBankId, null, null, String.valueOf(fromBankId), "Bank " + fromBankId, null, true);
        Bank toBank = new Bank(
                toBankId,   null, null, String.valueOf(toBankId),   "Bank " + toBankId,   null, true);

        ChannelType       channel = channelStr != null ? ChannelType.valueOf(channelStr)           : null;
        TransactionStatus status  = statusStr  != null ? TransactionStatus.valueOf(statusStr)      : null;

        Transaction txn = switch (txnType != null ? txnType : "") {
            case "CREDIT" -> new CreditTransaction(
                    id, createdLdt, updatedLdt,
                    src, sourceSystemId, channel, fromBank, toBank,
                    amount, txnDate, status, fromBankId, toBankId, batchId,
                    rs.getLong("credit_account_id"));

            case "DEBIT" -> new DebitTransaction(
                    id, createdLdt, updatedLdt,
                    src, sourceSystemId, channel, fromBank, toBank,
                    amount, txnDate, status, fromBankId, toBankId, batchId,
                    rs.getLong("debit_account_id"));

            case "REVERSAL" -> new ReversalTransaction(
                    id, createdLdt, updatedLdt,
                    src, sourceSystemId, channel, fromBank, toBank,
                    amount, txnDate, status, fromBankId, toBankId, batchId,
                    rs.getLong("original_transaction_id"),
                    rs.getString("reversal_reason"));

            default -> new InterBankTransaction(
                    id, createdLdt, updatedLdt,
                    src, sourceSystemId, channel, fromBank, toBank,
                    amount, txnDate, status, fromBankId, toBankId, batchId,
                    rs.getLong("nostro_account_id"));
        };

        return txn;
    }
}