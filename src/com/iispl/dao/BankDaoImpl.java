package com.iispl.dao;

import com.iispl.entity.Bank;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class BankDaoImpl implements BankDao {

    // =========================================================================
    // SAVE
    // =========================================================================

    @Override
    public long save(Bank bank, Connection conn) {
        String sql = "INSERT INTO bank (bank_code, bank_name, ifsc_code, is_active) VALUES (?, ?, ?, ?)";

        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, bank.getBankCode());
            ps.setString(2, bank.getBankName());
            ps.setString(3, bank.getIfscCode());
            ps.setBoolean(4, bank.isActive());
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save Bank bankCode="
                    + bank.getBankCode() + ": " + e.getMessage(), e);
        }
        throw new RuntimeException("save Bank: no generated key returned");
    }

    // =========================================================================
    // FIND BY ID
    // =========================================================================

    @Override
    public Bank findById(long bankId, Connection conn) {
        String sql = "SELECT id, bank_code, bank_name, ifsc_code, is_active FROM bank WHERE id = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, bankId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return (mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find Bank id=" + bankId, e);
        }
        return null;
    }

    // =========================================================================
    // FIND BY BANK CODE
    // =========================================================================

    @Override
    public Bank findByBankCode(String bankCode, Connection conn) {
        String sql = "SELECT id, bank_code, bank_name, ifsc_code, is_active FROM bank WHERE bank_code = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, bankCode);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return (mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find Bank bankCode=" + bankCode, e);
        }
        return null;
    }

    // =========================================================================
    // FIND ALL ACTIVE
    // =========================================================================

    @Override
    public List<Bank> findAllActive(Connection conn) {
        String sql = "SELECT id, bank_code, bank_name, ifsc_code, is_active " +
                     "FROM bank WHERE is_active = TRUE ORDER BY bank_code";

        List<Bank> banks = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                banks.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch active banks: " + e.getMessage(), e);
        }
        return banks;
    }

    // =========================================================================
    // PRIVATE HELPER
    // =========================================================================

    private Bank mapRow(ResultSet rs) throws SQLException {
        return new Bank(
                rs.getString("bank_code"),
                rs.getString("bank_name"),
                rs.getString("ifsc_code"),
                rs.getBoolean("is_active")
        );
    }
}