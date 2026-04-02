package com.iispl.dao;

import com.iispl.entity.Account;
import com.iispl.enums.AccountType;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class AccountDaoImpl implements AccountDao {

    // =========================================================================
    // SAVE
    // =========================================================================

    @Override
    public long save(Account account, Connection conn) {
        String sql = "INSERT INTO account " +
                     "(account_number, account_type, customer_id, bank_id, balance, status, created_at, updated_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?, NOW(), NOW())";

        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, account.getAccountNumber());
            ps.setString(2, account.getAccountType().name());
            ps.setLong(3, account.getCustomerId());
            ps.setLong(4, account.getBankId());
            ps.setBigDecimal(5, account.getBalance());
            ps.setString(6, account.getStatus());
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    long id = rs.getLong(1);
                    account.setId(id);
                    return id;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save Account accountNumber="
                    + account.getAccountNumber() + ": " + e.getMessage(), e);
        }
        throw new RuntimeException("save Account: no generated key returned");
    }

    // =========================================================================
    // UPDATE
    // =========================================================================

    @Override
    public void update(Account account, Connection conn) {
        String sql = "UPDATE account SET balance = ?, status = ?, updated_at = NOW() WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBigDecimal(1, account.getBalance());
            ps.setString(2, account.getStatus());
            ps.setLong(3, account.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update Account id=" + account.getId(), e);
        }
    }

    // =========================================================================
    // FIND BY ID
    // =========================================================================

    @Override
    public Account findById(long accountId, Connection conn) {
        String sql = "SELECT id, account_number, account_type, customer_id, bank_id, balance, status, " +
                     "created_at, updated_at FROM account WHERE id = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, accountId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find Account id=" + accountId, e);
        }
        return null;
    }

    // =========================================================================
    // FIND BY ACCOUNT NUMBER
    // =========================================================================

    @Override
    public Account findByAccountNumber(String accountNumber, Connection conn) {
        String sql = "SELECT id, account_number, account_type, customer_id, bank_id, balance, status, " +
                     "created_at, updated_at FROM account WHERE account_number = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, accountNumber);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find Account accountNumber=" + accountNumber, e);
        }
        return null;
    }

    // =========================================================================
    // FIND BY CUSTOMER
    // =========================================================================

    @Override
    public List<Account> findByCustomerId(long customerId, Connection conn) {
        String sql = "SELECT id, account_number, account_type, customer_id, bank_id, balance, status, " +
                     "created_at, updated_at FROM account WHERE customer_id = ? ORDER BY id";

        List<Account> accounts = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    accounts.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find Accounts for customerId=" + customerId, e);
        }
        return accounts;
    }

    // =========================================================================
    // FIND BY BANK + TYPE
    // =========================================================================

    @Override
    public List<Account> findByBankIdAndType(long bankId, AccountType accountType, Connection conn) {
        String sql = "SELECT id, account_number, account_type, customer_id, bank_id, balance, status, " +
                     "created_at, updated_at FROM account WHERE bank_id = ? AND account_type = ? ORDER BY id";

        List<Account> accounts = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, bankId);
            ps.setString(2, accountType.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    accounts.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find Accounts bankId=" + bankId
                    + " type=" + accountType, e);
        }
        return accounts;
    }

    // =========================================================================
    // CREDIT
    // =========================================================================

    @Override
    public void credit(long accountId, BigDecimal amount, Connection conn) {
        String sql = "UPDATE account SET balance = balance + ?, updated_at = NOW() WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBigDecimal(1, amount);
            ps.setLong(2, accountId);
            int rows = ps.executeUpdate();
            if (rows == 0) {
                throw new RuntimeException("credit: Account id=" + accountId + " not found");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to credit accountId=" + accountId
                    + " amount=" + amount, e);
        }
    }

    // =========================================================================
    // DEBIT
    // =========================================================================

    @Override
    public void debit(long accountId, BigDecimal amount, Connection conn) {
        String sql = "UPDATE account SET balance = balance - ?, updated_at = NOW() WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBigDecimal(1, amount);
            ps.setLong(2, accountId);
            int rows = ps.executeUpdate();
            if (rows == 0) {
                throw new RuntimeException("debit: Account id=" + accountId + " not found");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to debit accountId=" + accountId
                    + " amount=" + amount, e);
        }
    }

    // =========================================================================
    // PRIVATE HELPER
    // =========================================================================

    // FIX: Old code called new Account(accountNumber, accountType, customerId, bankId, balance)
    //      then account.setId() and account.setStatus() separately — old 5-arg constructor
    //      no longer exists and status is now a constructor param.
    //      Account now requires (Long id, LocalDateTime createdAt, LocalDateTime updatedAt,
    //      String accountNumber, AccountType accountType, Long customerId, Long bankId,
    //      BigDecimal balance, String status).
    //      All SELECTs updated to include created_at, updated_at.
    private Account mapRow(ResultSet rs) throws SQLException {
        Timestamp createdAt = rs.getTimestamp("created_at");
        Timestamp updatedAt = rs.getTimestamp("updated_at");

        return new Account(
                rs.getLong("id"),
                createdAt != null ? createdAt.toLocalDateTime() : null,
                updatedAt != null ? updatedAt.toLocalDateTime() : null,
                rs.getString("account_number"),
                AccountType.valueOf(rs.getString("account_type")),
                rs.getLong("customer_id"),
                rs.getLong("bank_id"),
                rs.getBigDecimal("balance"),
                rs.getString("status")
        );
    }
}