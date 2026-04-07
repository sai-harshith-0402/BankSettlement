package com.iispl.dao;

import com.iispl.connectionpool.ConnectionPool;
import com.iispl.entity.IncomingTransaction;
import com.iispl.entity.SourceSystem;
import com.iispl.enums.ChannelType;
import com.iispl.enums.ProcessingStatus;
import com.iispl.enums.SourceType;
import com.iispl.enums.TransactionType;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class TransactionDaoImpl implements TransactionDao {

    DataSource dataSource = ConnectionPool.getDataSource();

    // Persist a new incoming transaction to DB
    @Override
    public IncomingTransaction save(IncomingTransaction transaction) {
        try (Connection con = dataSource.getConnection()) {
            String sql = "insert into incoming_transaction values(?,?,?,?,?,?,?,?,?,?,?)";
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
            ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return transaction;
    }

    // Fetch all incoming transactions
    @Override
    public List<IncomingTransaction> findAll() {
        List<IncomingTransaction> list = new ArrayList<>();
        try (Connection con = dataSource.getConnection()) {
            String sql = "select * from incoming_transaction";
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
    public IncomingTransaction findById(long id) {
        IncomingTransaction transaction = null;
        try (Connection con = dataSource.getConnection()) {
            String sql = "select * from incoming_transaction where incoming_tnx_id=?";
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

    // Assign or update the batch id on a transaction
    @Override
    public void updateBatchId(long id, String batchId) {
        try (Connection con = dataSource.getConnection()) {
            String sql = "update incoming_transaction set batch_id=? where incoming_tnx_id=?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, batchId);
            ps.setLong(2, id);
            ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Fetch all transactions from a specific source system
    @Override
    public List<IncomingTransaction> findBySourceSystemId(long sourceSystemId) {
        List<IncomingTransaction> list = new ArrayList<>();
        try (Connection con = dataSource.getConnection()) {
            String sql = "select * from incoming_transaction where source_system_id=?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setLong(1, sourceSystemId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    // Fetch all transactions with a given processing status
    @Override
    public List<IncomingTransaction> findByProcessingStatus(ProcessingStatus status) {
        List<IncomingTransaction> list = new ArrayList<>();
        try (Connection con = dataSource.getConnection()) {
            String sql = "select * from incoming_transaction where processing_status=?";
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

    // Map a ResultSet row to an IncomingTransaction object
    private IncomingTransaction mapRow(ResultSet rs) throws Exception {
        SourceSystem sourceSystem = new SourceSystem(
                rs.getLong("source_system_id"),
                SourceType.valueOf(rs.getString("source_type")),
                rs.getString("file_path")
        );
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
                rs.getTimestamp("ingestion_time_stamp").toLocalDateTime(),
                rs.getString("batch_id")
        );
    }
}