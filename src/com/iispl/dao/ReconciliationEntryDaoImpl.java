package com.iispl.dao;

import com.iispl.connectionpool.ConnectionPool;
import com.iispl.dao.ReconciliationEntryDao;
import com.iispl.entity.ReconciliationEntry;
import com.iispl.enums.ReconStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ReconciliationEntryDaoImpl implements ReconciliationEntryDao {

    // -----------------------------------------------------------------------
    // Write operations
    // -----------------------------------------------------------------------

    @Override
    public void saveReconciliationEntry(ReconciliationEntry entry) {
        String sql = "INSERT INTO reconciliation_entry "
                   + "(entry_id, reconciliation_date, account_id, expected_amount, actual_amount, variance, recon_status) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection con = ConnectionPool.getDataSource().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, entry.getEntryId());
            ps.setObject(2, entry.getReconciliationDate());
            ps.setLong(3, entry.getAccountId());
            ps.setBigDecimal(4, entry.getExpectedAmount());
            ps.setBigDecimal(5, entry.getActualAmount());
            ps.setBigDecimal(6, entry.getVariance());
            ps.setString(7, entry.getReconStatus().name());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save reconciliation entry [" + entry.getEntryId() + "]: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteReconciliationEntry(long entryId) {
        String sql = "DELETE FROM reconciliation_entry WHERE entry_id = ?";
        try (Connection con = ConnectionPool.getDataSource().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, entryId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete reconciliation entry [" + entryId + "]: " + e.getMessage(), e);
        }
    }

    // -----------------------------------------------------------------------
    // Read operations
    // -----------------------------------------------------------------------

    @Override
    public List<ReconciliationEntry> findAllReconciliationEntries() {
        String sql = "SELECT entry_id, reconciliation_date, account_id, expected_amount, "
                   + "actual_amount, variance, recon_status FROM reconciliation_entry";
        List<ReconciliationEntry> entries = new ArrayList<>();
        try (Connection con = ConnectionPool.getDataSource().getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) entries.add(mapRow(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch all reconciliation entries: " + e.getMessage(), e);
        }
        return entries;
    }

    @Override
    public ReconciliationEntry findReconciliationEntryById(long entryId) {
        String sql = "SELECT entry_id, reconciliation_date, account_id, expected_amount, "
                   + "actual_amount, variance, recon_status FROM reconciliation_entry WHERE entry_id = ?";
        try (Connection con = ConnectionPool.getDataSource().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, entryId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find reconciliation entry [" + entryId + "]: " + e.getMessage(), e);
        }
        return null;
    }

    @Override
    public List<ReconciliationEntry> findEntriesByAccountId(long accountId) {
        String sql = "SELECT entry_id, reconciliation_date, account_id, expected_amount, "
                   + "actual_amount, variance, recon_status FROM reconciliation_entry WHERE account_id = ?";
        List<ReconciliationEntry> entries = new ArrayList<>();
        try (Connection con = ConnectionPool.getDataSource().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, accountId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) entries.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find entries by account [" + accountId + "]: " + e.getMessage(), e);
        }
        return entries;
    }

    @Override
    public List<ReconciliationEntry> findEntriesByDate(LocalDate reconciliationDate) {
        String sql = "SELECT entry_id, reconciliation_date, account_id, expected_amount, "
                   + "actual_amount, variance, recon_status FROM reconciliation_entry WHERE reconciliation_date = ?";
        List<ReconciliationEntry> entries = new ArrayList<>();
        try (Connection con = ConnectionPool.getDataSource().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setObject(1, reconciliationDate);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) entries.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find entries by date [" + reconciliationDate + "]: " + e.getMessage(), e);
        }
        return entries;
    }

    @Override
    public List<ReconciliationEntry> findEntriesByStatus(ReconStatus reconStatus) {
        String sql = "SELECT entry_id, reconciliation_date, account_id, expected_amount, "
                   + "actual_amount, variance, recon_status FROM reconciliation_entry WHERE recon_status = ?";
        List<ReconciliationEntry> entries = new ArrayList<>();
        try (Connection con = ConnectionPool.getDataSource().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, reconStatus.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) entries.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find entries by status [" + reconStatus + "]: " + e.getMessage(), e);
        }
        return entries;
    }

    @Override
    public List<ReconciliationEntry> findEntriesByAccountAndDate(long accountId, LocalDate reconciliationDate) {
        String sql = "SELECT entry_id, reconciliation_date, account_id, expected_amount, "
                   + "actual_amount, variance, recon_status FROM reconciliation_entry "
                   + "WHERE account_id = ? AND reconciliation_date = ?";
        List<ReconciliationEntry> entries = new ArrayList<>();
        try (Connection con = ConnectionPool.getDataSource().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, accountId);
            ps.setObject(2, reconciliationDate);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) entries.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find entries by account and date: " + e.getMessage(), e);
        }
        return entries;
    }

    // -----------------------------------------------------------------------
    // Update operations
    // -----------------------------------------------------------------------

    @Override
    public void updateReconStatus(long entryId, ReconStatus newStatus) {
        String sql = "UPDATE reconciliation_entry SET recon_status = ? WHERE entry_id = ?";
        try (Connection con = ConnectionPool.getDataSource().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, newStatus.name());
            ps.setLong(2, entryId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update recon status for entry [" + entryId + "]: " + e.getMessage(), e);
        }
    }

    // -----------------------------------------------------------------------
    // Row mapper
    // -----------------------------------------------------------------------

    private ReconciliationEntry mapRow(ResultSet rs) throws SQLException {
        return new ReconciliationEntry(
            rs.getLong("entry_id"),
            rs.getObject("reconciliation_date", LocalDate.class),
            rs.getLong("account_id"),
            rs.getBigDecimal("expected_amount"),
            rs.getBigDecimal("actual_amount"),
            rs.getBigDecimal("variance"),
            ReconStatus.valueOf(rs.getString("recon_status"))
        );
    }
}