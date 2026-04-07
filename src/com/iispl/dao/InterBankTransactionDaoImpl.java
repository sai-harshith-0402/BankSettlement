package com.iispl.dao;

import com.iispl.connectionpool.ConnectionPool;
import com.iispl.entity.InterBankTransaction;
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

public class InterBankTransactionDaoImpl implements InterBankTransactionDao {

    DataSource dataSource = ConnectionPool.getDataSource();

    // Persist a new interbank transaction to DB
    @Override
    public InterBankTransaction save(InterBankTransaction transaction) {
        try (Connection con = dataSource.getConnection()) {
            String sql = "insert into inter_bank_transaction values(?,?,?,?,?,?,?,?,?,?,?,?,?)";
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
            ps.setLong(12, transaction.getNostroAccountId());
            ps.setLong(13, transaction.getVostroAccountId());
            ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return transaction;
    }

    // Fetch all interbank transactions
    @Override
    public List<InterBankTransaction> findAll() {
        List<InterBankTransaction> list = new ArrayList<>();
        try (Connection con = dataSource.getConnection()) {
            String sql = "select * from inter_bank_transaction";
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
    public InterBankTransaction findById(long id) {
        InterBankTransaction transaction = null;
        try (Connection con = dataSource.getConnection()) {
            String sql = "select * from inter_bank_transaction where incoming_tnx_id=?";
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
            String sql = "update inter_bank_transaction set batch_id=? where incoming_tnx_id=?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, settlementBatchId);
            ps.setLong(2, id);
            ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Fetch all interbank transactions linked to a specific nostro account
    @Override
    public List<InterBankTransaction> findByNostroAccountId(long nostroAccountId) {
        List<InterBankTransaction> list = new ArrayList<>();
        try (Connection con = dataSource.getConnection()) {
            String sql = "select * from inter_bank_transaction where nostro_account_id=?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setLong(1, nostroAccountId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    // Fetch all transactions with a specific status
    @Override
    public List<InterBankTransaction> findByStatus(TransactionStatus status) {
        List<InterBankTransaction> list = new ArrayList<>();
        try (Connection con = dataSource.getConnection()) {
            String sql = "select * from inter_bank_transaction where processing_status=?";
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

    // Fetch all transactions whose txn_date falls within the given range
    @Override
    public List<InterBankTransaction> findByDateRange(LocalDateTime from, LocalDateTime to) {
        List<InterBankTransaction> list = new ArrayList<>();
        try (Connection con = dataSource.getConnection()) {
            String sql = "select * from inter_bank_transaction where ingestion_time_stamp between ? and ?";
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

    // Update the status of a specific transaction
    @Override
    public void updateStatus(long id, TransactionStatus newStatus) {
        try (Connection con = dataSource.getConnection()) {
            String sql = "update inter_bank_transaction set processing_status=? where incoming_tnx_id=?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, newStatus.name());
            ps.setLong(2, id);
            ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Map a ResultSet row to an InterBankTransaction object
    private InterBankTransaction mapRow(ResultSet rs) throws Exception {
        SourceSystem sourceSystem = new SourceSystem(
                rs.getLong("source_system_id"),
                SourceType.valueOf(rs.getString("source_type")),
                rs.getString("file_path")
        );
        return new InterBankTransaction(
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
                rs.getLong("nostro_account_id"),
                rs.getLong("vostro_account_id")
        );
    }
}