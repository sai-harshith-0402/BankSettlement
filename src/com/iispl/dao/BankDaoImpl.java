package com.iispl.dao;


import com.iispl.dao.BankDao;
import com.iispl.entity.Bank;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class BankDaoImpl implements BankDao {

    private final Connection connection;

    public BankDaoImpl(Connection connection) {
        this.connection = connection;
    }

    @Override
    public Bank saveBank(Bank bank) {
        String sql = "INSERT INTO bank (bank_code, bank_name, ifsc_code, is_active, created_at, updated_at) "
                   + "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, bank.getBankCode());
            ps.setString(2, bank.getBankName());
            ps.setString(3, bank.getIfscCode());
            ps.setBoolean(4, bank.isActive());
            ps.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()));
            ps.setTimestamp(6, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    bank.setId(rs.getLong(1));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error saving bank: " + e.getMessage(), e);
        }
        return bank;
    }

    @Override
    public void changeStatus(Long bankId, boolean isActive) {
        String sql = "UPDATE bank SET is_active = ?, updated_at = ? WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setBoolean(1, isActive);
            ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            ps.setLong(3, bankId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error changing status for bank " + bankId + ": " + e.getMessage(), e);
        }
    }

    @Override
    public List<Bank> findAllBanks() {
        String sql = "SELECT * FROM bank";
        List<Bank> banks = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                banks.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching all banks: " + e.getMessage(), e);
        }
        return banks;
    }

    @Override
    public Bank findBankById(Long id) {
        String sql = "SELECT * FROM bank WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding bank by id " + id + ": " + e.getMessage(), e);
        }
        return null;
    }

    @Override
    public Bank findBankByBankCode(String bankCode) {
        String sql = "SELECT * FROM bank WHERE bank_code = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, bankCode);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding bank by code " + bankCode + ": " + e.getMessage(), e);
        }
        return null;
    }

    @Override
    public Bank findBankByIfscCode(String ifscCode) {
        String sql = "SELECT * FROM bank WHERE ifsc_code = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, ifscCode);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding bank by IFSC " + ifscCode + ": " + e.getMessage(), e);
        }
        return null;
    }

    @Override
    public List<Bank> findAllActiveBanks() {
        String sql = "SELECT * FROM bank WHERE is_active = TRUE";
        List<Bank> banks = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                banks.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching active banks: " + e.getMessage(), e);
        }
        return banks;
    }

    @Override
    public Bank updateBank(Bank bank) {
        String sql = "UPDATE bank SET bank_code = ?, bank_name = ?, ifsc_code = ?, is_active = ?, updated_at = ? WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, bank.getBankCode());
            ps.setString(2, bank.getBankName());
            ps.setString(3, bank.getIfscCode());
            ps.setBoolean(4, bank.isActive());
            ps.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()));
            ps.setLong(6, bank.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error updating bank: " + e.getMessage(), e);
        }
        return bank;
    }

    @Override
    public void deleteBank(Long bankId) {
        String sql = "DELETE FROM bank WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, bankId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error deleting bank " + bankId + ": " + e.getMessage(), e);
        }
    }

    // -----------------------------------------------------------------------
    // Private helper
    // -----------------------------------------------------------------------

    private Bank mapRow(ResultSet rs) throws SQLException {
        return new Bank(
                rs.getLong("id"),
                rs.getTimestamp("created_at").toLocalDateTime(),
                rs.getTimestamp("updated_at").toLocalDateTime(),
                rs.getString("bank_code"),
                rs.getString("bank_name"),
                rs.getString("ifsc_code"),
                rs.getBoolean("is_active")
        );
    }
}