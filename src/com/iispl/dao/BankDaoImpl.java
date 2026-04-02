package com.iispl.dao;

import com.iispl.entity.Bank;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class BankDaoImpl implements BankDao {

    // =========================================================================
    // SAVE
    // =========================================================================

    // FIX: Added created_at, updated_at columns to INSERT — Bank now extends
    //      BaseEntity and those columns exist on the bank table.
    @Override
    public long save(Bank bank, Connection conn) {
        String sql = "INSERT INTO bank (bank_code, bank_name, ifsc_code, is_active, created_at, updated_at) " +
                     "VALUES (?, ?, ?, ?, NOW(), NOW())";

        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, bank.getBankCode());
            ps.setString(2, bank.getBankName());
            ps.setString(3, bank.getIfscCode());
            ps.setBoolean(4, bank.isActive());
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    long id = rs.getLong(1);
                    bank.setId(id);
                    return id;
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
        String sql = "SELECT id, bank_code, bank_name, ifsc_code, is_active, created_at, updated_at " +
                     "FROM bank WHERE id = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, bankId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
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
        String sql = "SELECT id, bank_code, bank_name, ifsc_code, is_active, created_at, updated_at " +
                     "FROM bank WHERE bank_code = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, bankCode);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
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
        String sql = "SELECT id, bank_code, bank_name, ifsc_code, is_active, created_at, updated_at " +
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

    // FIX: Old code called new Bank(bankCode, bankName, ifscCode, isActive) — 4-arg constructor
    //      that no longer exists. Bank now requires
    //      (Long id, LocalDateTime createdAt, LocalDateTime updatedAt,
    //       String bankCode, String bankName, String ifscCode, boolean isActive).
    //      Also added created_at and updated_at to every SELECT so mapRow can read them.
    private Bank mapRow(ResultSet rs) throws SQLException {
        Timestamp createdAt = rs.getTimestamp("created_at");
        Timestamp updatedAt = rs.getTimestamp("updated_at");

        return new Bank(
                rs.getLong("id"),
                createdAt != null ? createdAt.toLocalDateTime() : null,
                updatedAt != null ? updatedAt.toLocalDateTime() : null,
                rs.getString("bank_code"),
                rs.getString("bank_name"),
                rs.getString("ifsc_code"),
                rs.getBoolean("is_active")
        );
    }
}