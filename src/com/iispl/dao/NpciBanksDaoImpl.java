package com.iispl.dao;

import com.iispl.connectionpool.ConnectionPool;
import com.iispl.dao.NpciBanksDao;
import com.iispl.entity.NPCIBank;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class NpciBanksDaoImpl implements NpciBanksDao {

    // -----------------------------------------------------------------------
    // Write operations
    // -----------------------------------------------------------------------

    @Override
    public void saveNPCIBank(NPCIBank npcIBank) {
        String sql = "INSERT INTO npci_bank (bank_id, bank_name, balance_amount) VALUES (?, ?, ?)";
        try (Connection con = ConnectionPool.getDataSource().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, npcIBank.getBankId());
            ps.setString(2, npcIBank.getBankName());
            ps.setBigDecimal(3, npcIBank.getBalanceAmount());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save NPCI bank [" + npcIBank.getBankId() + "]: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteNPCIBank(String bankId) {
        String sql = "DELETE FROM npci_bank WHERE bank_id = ?";
        try (Connection con = ConnectionPool.getDataSource().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, bankId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete NPCI bank [" + bankId + "]: " + e.getMessage(), e);
        }
    }

    // -----------------------------------------------------------------------
    // Read operations
    // -----------------------------------------------------------------------

    @Override
    public List<NPCIBank> findAllNPCIBanks() {
        String sql = "SELECT bank_id, bank_name, balance_amount FROM npci_bank";
        List<NPCIBank> banks = new ArrayList<>();
        try (Connection con = ConnectionPool.getDataSource().getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) banks.add(mapRow(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch all NPCI banks: " + e.getMessage(), e);
        }
        return banks;
    }

    @Override
    public NPCIBank findNPCIBankById(String bankId) {
        String sql = "SELECT bank_id, bank_name, balance_amount FROM npci_bank WHERE bank_id = ?";
        try (Connection con = ConnectionPool.getDataSource().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, bankId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find NPCI bank [" + bankId + "]: " + e.getMessage(), e);
        }
        return null;
    }

    @Override
    public NPCIBank findNPCIBankByName(String bankName) {
        String sql = "SELECT bank_id, bank_name, balance_amount FROM npci_bank WHERE bank_name = ?";
        try (Connection con = ConnectionPool.getDataSource().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, bankName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find NPCI bank by name [" + bankName + "]: " + e.getMessage(), e);
        }
        return null;
    }

    @Override
    public List<NPCIBank> findBanksBelowBalanceThreshold(BigDecimal threshold) {
        String sql = "SELECT bank_id, bank_name, balance_amount FROM npci_bank WHERE balance_amount < ?";
        List<NPCIBank> banks = new ArrayList<>();
        try (Connection con = ConnectionPool.getDataSource().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setBigDecimal(1, threshold);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) banks.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find banks below threshold [" + threshold + "]: " + e.getMessage(), e);
        }
        return banks;
    }

    // -----------------------------------------------------------------------
    // Update operations
    // -----------------------------------------------------------------------

    @Override
    public void updateBalance(String bankId, BigDecimal newBalance) {
        String sql = "UPDATE npci_bank SET balance_amount = ? WHERE bank_id = ?";
        try (Connection con = ConnectionPool.getDataSource().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setBigDecimal(1, newBalance);
            ps.setString(2, bankId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update balance for bank [" + bankId + "]: " + e.getMessage(), e);
        }
    }

    @Override
    public void creditBalance(String bankId, BigDecimal amount) {
        String sql = "UPDATE npci_bank SET balance_amount = balance_amount + ? WHERE bank_id = ?";
        try (Connection con = ConnectionPool.getDataSource().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setBigDecimal(1, amount);
            ps.setString(2, bankId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to credit balance for bank [" + bankId + "]: " + e.getMessage(), e);
        }
    }

    @Override
    public void debitBalance(String bankId, BigDecimal amount) {
        String sql = "UPDATE npci_bank SET balance_amount = balance_amount - ? WHERE bank_id = ?";
        try (Connection con = ConnectionPool.getDataSource().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setBigDecimal(1, amount);
            ps.setString(2, bankId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to debit balance for bank [" + bankId + "]: " + e.getMessage(), e);
        }
    }

    // -----------------------------------------------------------------------
    // Row mapper
    // -----------------------------------------------------------------------

    private NPCIBank mapRow(ResultSet rs) throws SQLException {
        return new NPCIBank(
            rs.getLong("bank_id"),
            rs.getString("bank_name"),
            rs.getBigDecimal("balance_amount")
        );
    }
}