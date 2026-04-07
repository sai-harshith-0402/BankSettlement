package com.iispl.dao;

import com.iispl.connectionpool.ConnectionPool;
import com.iispl.entity.ReversalTransaction;
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
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ReversalTransactionDaoImpl implements ReversalTransactionDao {

    DataSource dataSource = ConnectionPool.getDataSource();

    // Persist a new reversal transaction to DB
    @Override
    public ReversalTransaction save(ReversalTransaction transaction) {
        try (Connection con = dataSource.getConnection()) {
            String sql = "insert into reversal_transaction values(?,?,?,?,?,?,?,?,?,?,?,?,?)";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setLong(1, transaction.getIncomingTnxId());
            ps.setLong(2, transaction.getSourceSystem().getSourceSystemId());
            ps.setLong(3, transaction.getSourceSystemId());
            ps.setString(4, transaction.getTransactionType().name());
            ps.setString(5, transaction.getChannelType().name());
            ps.setString(6, transaction.getFromBankName());
            ps.setString(7, transaction.getToBankName());
            ps.setBigDecimal(8, transaction.getAmount());
            ps.setString(9, transaction.getProcessingStatus().name());
            ps.setTimestamp(10, Timestamp.valueOf(transaction.getIngestionTimeStamp()));
            ps.setString(11, transaction.getBatchId());
            ps.setLong(12, transaction.getOriginalTransactionId());
            ps.setString(13, transaction.getReversalType());
            ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return transaction;
    }

    // Fetch all reversal transactions
    @Override
    public List<ReversalTransaction> findAll() {
        List<ReversalTransaction> list = new ArrayList<>();
        try (Connection con = dataSource.getConnection()) {
            String sql = "select * from reversal_transaction";
            PreparedStatement ps = con.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    // Fetch by primary key; returns null if not found
    @Override
    public ReversalTransaction findById(long id) {
        ReversalTransaction transaction = null;
        try (Connection con = dataSource.getConnection()) {
            String sql = "select * from reversal_transaction where incoming_tnx_id=?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setLong(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                transaction = mapRow(rs);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return transaction;
    }

    // Assign or update the settlement batch id on a transaction
    @Override
    public void updateBatchId(long id, String settlementBatchId) {
        try (Connection con = dataSource.getConnection()) {
            String sql = "update reversal_transaction set batch_id=? where incoming_tnx_id=?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, settlementBatchId);
            ps.setLong(2, id);
            ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Fetch all reversals that reference a given original transaction
    @Override
    public List<ReversalTransaction> findByOriginalTransactionId(long originalTransactionId) {
        List<ReversalTransaction> list = new ArrayList<>();
        try (Connection con = dataSource.getConnection()) {
            String sql = "select * from reversal_transaction where original_transaction_id=?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setLong(1, originalTransactionId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    // Fetch all reversal transactions with a specific status
    @Override
    public List<ReversalTransaction> findByStatus(TransactionStatus status) {
        List<ReversalTransaction> list = new ArrayList<>();
        try (Connection con = dataSource.getConnection()) {
            String sql = "select * from reversal_transaction where processing_status=?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, status.name());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    // Fetch all reversal transactions whose txn_date falls within the given range
    @Override
    public List<ReversalTransaction> findByDateRange(LocalDateTime from, LocalDateTime to) {
        List<ReversalTransaction> list = new ArrayList<>();
        try (Connection con = dataSource.getConnection()) {
            String sql = "select * from reversal_transaction where ingestion_time_stamp between ? and ?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setTimestamp(1, Timestamp.valueOf(from));
            ps.setTimestamp(2, Timestamp.valueOf(to));
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    // Update the status of a specific reversal transaction
    @Override
    public void updateStatus(long id, TransactionStatus newStatus) {
        try (Connection con = dataSource.getConnection()) {
            String sql = "update reversal_transaction set processing_status=? where incoming_tnx_id=?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, newStatus.name());
            ps.setLong(2, id);
            ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Map a ResultSet row to a ReversalTransaction object
    private ReversalTransaction mapRow(ResultSet rs) throws Exception {
        SourceSystem sourceSystem = new SourceSystem(
                rs.getLong("source_system_id"),
                SourceType.valueOf(rs.getString("source_type")),
                rs.getString("file_path")
        );
        return new ReversalTransaction(
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
                rs.getLong("original_transaction_id"),
                rs.getString("reversal_type")
        );
    }
}