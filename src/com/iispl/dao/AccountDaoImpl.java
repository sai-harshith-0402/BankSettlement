package com.iispl.dao;

import com.iispl.dao.AccountDao;
import com.iispl.entity.Account;
import com.iispl.enums.AccountType;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AccountDaoImpl implements AccountDao {

    private final Connection connection;

    public AccountDaoImpl(Connection connection) {
        this.connection = connection;
    }

    @Override
    public Account saveAccount(Account account) {
        String sql = "INSERT INTO account (account_number, account_type, customer_id, bank_id, balance, status, created_at, updated_at) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, account.getAccountNumber());
            ps.setString(2, account.getAccountType().name());
            ps.setLong(3, account.getCustomerId());
            ps.setLong(4, account.getBankId());
            ps.setBigDecimal(5, account.getBalance());
            ps.setString(6, account.getStatus());
            ps.setTimestamp(7, Timestamp.valueOf(LocalDateTime.now()));
            ps.setTimestamp(8, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    account.setId(rs.getLong(1));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error saving account: " + e.getMessage(), e);
        }
        return account;
    }

    @Override
    public Account updateAccount(Account account) {
        String sql = "UPDATE account SET account_type = ?, balance = ?, status = ?, updated_at = ? WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, account.getAccountType().name());
            ps.setBigDecimal(2, account.getBalance());
            ps.setString(3, account.getStatus());
            ps.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
            ps.setLong(5, account.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error updating account: " + e.getMessage(), e);
        }
        return account;
    }

    @Override
    public void deleteAccount(Long id) {
        String sql = "DELETE FROM account WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error deleting account with id " + id + ": " + e.getMessage(), e);
        }
    }

    @Override
    public Account findAccountById(Long id) {
        String sql = "SELECT * FROM account WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding account by id " + id + ": " + e.getMessage(), e);
        }
        return null;
    }

    @Override
    public List<Account> findAllAccounts() {
        String sql = "SELECT * FROM account";
        List<Account> accounts = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                accounts.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching all accounts: " + e.getMessage(), e);
        }
        return accounts;
    }

    @Override
    public List<Account> findAccountByCustomerId(Long customerId) {
        String sql = "SELECT * FROM account WHERE customer_id = ?";
        List<Account> accounts = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    accounts.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding accounts for customer " + customerId + ": " + e.getMessage(), e);
        }
        return accounts;
    }

    @Override
    public void updateBalance(Long accountId, BigDecimal newBalance) {
        String sql = "UPDATE account SET balance = ?, updated_at = ? WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setBigDecimal(1, newBalance);
            ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            ps.setLong(3, accountId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error updating balance for account " + accountId + ": " + e.getMessage(), e);
        }
    }

    @Override
    public Account findAccountByAccountNumber(String accountNumber) {
        String sql = "SELECT * FROM account WHERE account_number = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, accountNumber);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding account by number " + accountNumber + ": " + e.getMessage(), e);
        }
        return null;
    }

    @Override
    public List<Account> findAccountsByBankId(Long bankId) {
        String sql = "SELECT * FROM account WHERE bank_id = ?";
        List<Account> accounts = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, bankId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    accounts.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding accounts for bank " + bankId + ": " + e.getMessage(), e);
        }
        return accounts;
    }

    @Override
    public List<Account> findAccountsByType(AccountType accountType) {
        String sql = "SELECT * FROM account WHERE account_type = ?";
        List<Account> accounts = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, accountType.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    accounts.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding accounts by type " + accountType + ": " + e.getMessage(), e);
        }
        return accounts;
    }

    @Override
    public void updateStatus(Long accountId, String status) {
        String sql = "UPDATE account SET status = ?, updated_at = ? WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            ps.setLong(3, accountId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error updating status for account " + accountId + ": " + e.getMessage(), e);
        }
    }

    // -----------------------------------------------------------------------
    // Private helper
    // -----------------------------------------------------------------------

    private Account mapRow(ResultSet rs) throws SQLException {
        return new Account(
                rs.getLong("id"),
                rs.getTimestamp("created_at").toLocalDateTime(),
                rs.getTimestamp("updated_at").toLocalDateTime(),
                rs.getLong("customer_id"),
                rs.getString("account_number"),
                AccountType.valueOf(rs.getString("account_type")),                
                rs.getLong("bank_id"),
                rs.getBigDecimal("balance"),
                rs.getString("status")
        );
    }
}