package com.iispl.dao;

import com.iispl.connectionpool.ConnectionPool;
import com.iispl.entity.Account;
import com.iispl.entity.Customer;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class CustomerDaoImpl implements CustomerDao {

    DataSource dataSource = ConnectionPool.getDataSource();

    // =========================================================================
    // SAVE
    // =========================================================================

    @Override
    public long save(Customer customer) {
        String sql = "INSERT INTO customer (first_name, last_name, email_id, on_boarding_date, bank_id, is_active, created_at, updated_at) "
                   + "VALUES (?, ?, ?, ?, ?, ?, NOW(), NOW())";

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, customer.getFirstName());
            ps.setString(2, customer.getLastName());
            ps.setString(3, customer.getEmailId());
            ps.setTimestamp(4, customer.getOnBoardingDate() != null
                    ? Timestamp.valueOf(customer.getOnBoardingDate()) : null);
            ps.setString(5, customer.getBankId());
            ps.setBoolean(6, customer.isActive());
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    long id = rs.getLong(1);
                    customer.setId(id);
                    return id;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save Customer email="
                    + customer.getEmailId() + ": " + e.getMessage(), e);
        }
        throw new RuntimeException("save Customer: no generated key returned");
    }

    // =========================================================================
    // DELETE
    // =========================================================================

    @Override
    public void delete(Long customerId) {
        String sql = "DELETE FROM customer WHERE id = ?";

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, customerId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete Customer id=" + customerId + ": " + e.getMessage(), e);
        }
    }

    // =========================================================================
    // FIND BY BANK ID
    // =========================================================================

    @Override
    public List<Customer> findByBankId(Long bankId) {
        String sql = "SELECT * FROM customer WHERE bank_id = ?";
        List<Customer> list = new ArrayList<>();

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, bankId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find Customers for bankId=" + bankId + ": " + e.getMessage(), e);
        }
        return list;
    }

    // =========================================================================
    // FIND ALL
    // =========================================================================

    @Override
    public List<Customer> findAll() {
        String sql = "SELECT * FROM customer";
        List<Customer> list = new ArrayList<>();

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch all Customers: " + e.getMessage(), e);
        }
        return list;
    }

    // =========================================================================
    // FIND BY ID
    // =========================================================================

    @Override
    public Customer findById(Long customerId) {
        String sql = "SELECT * FROM customer WHERE id = ?";

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find Customer id=" + customerId + ": " + e.getMessage(), e);
        }
        return null;
    }

    // =========================================================================
    // FIND BY EMAIL
    // =========================================================================

    @Override
    public Customer findByEmail(String email) {
        String sql = "SELECT * FROM customer WHERE email_id = ?";

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find Customer email=" + email + ": " + e.getMessage(), e);
        }
        return null;
    }

    // =========================================================================
    // UPDATE
    // =========================================================================

    @Override
    public void update(Customer customer) {
        String sql = "UPDATE customer SET email_id = ?, is_active = ?, updated_at = NOW() WHERE id = ?";

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, customer.getEmailId());
            ps.setBoolean(2, customer.isActive());
            ps.setLong(3, customer.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update Customer id=" + customer.getId() + ": " + e.getMessage(), e);
        }
    }

    // =========================================================================
    // PRIVATE HELPER
    // accounts are loaded separately via AccountDao — passed as empty list here
    // =========================================================================

    private Customer mapRow(ResultSet rs) throws SQLException {
        Timestamp createdAt = rs.getTimestamp("created_at");
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        Timestamp onBoardingDate = rs.getTimestamp("on_boarding_date");

        return new Customer(
                rs.getLong("id"),
                createdAt != null ? createdAt.toLocalDateTime() : null,
                updatedAt != null ? updatedAt.toLocalDateTime() : null,
                rs.getString("first_name"),
                rs.getString("last_name"),
                rs.getString("email_id"),
                onBoardingDate != null ? onBoardingDate.toLocalDateTime() : null,
                rs.getString("bank_id"),
                rs.getBoolean("is_active"),
                new ArrayList<>()   // accounts loaded separately via AccountDao
        );
    }
}
//package com.iispl.dao;
//
//import com.iispl.entity.Customer;
//
//import java.sql.Connection;
//import java.sql.Date;
//import java.sql.PreparedStatement;
//import java.sql.ResultSet;
//import java.sql.SQLException;
//import java.sql.Statement;
//import java.sql.Timestamp;
//import java.util.ArrayList;
//import java.util.List;
//
//public class CustomerDaoImpl implements CustomerDao {
//
//    private final Connection connection;
//
//    public CustomerDaoImpl(Connection connection) {
//        this.connection = connection;
//    }
//
//    // =========================================================================
//    // SAVE
//    // =========================================================================
//
//    @Override
//    public long save(Customer customer) {
//        String sql = "INSERT INTO customer (first_name, last_name, email, onboarding_date, created_at, updated_at) "
//                   + "VALUES (?, ?, ?, ?, NOW(), NOW())";
//
//        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
//            ps.setString(1, customer.getFirstName());
//            ps.setString(2, customer.getLastName());
//            ps.setString(3, customer.getEmail());
//            ps.setDate(4, customer.getOnboardingDate() != null
//                    ? Date.valueOf(customer.getOnboardingDate()) : null);
//            ps.executeUpdate();
//
//            try (ResultSet rs = ps.getGeneratedKeys()) {
//                if (rs.next()) {
//                    long id = rs.getLong(1);
//                    customer.setId(id);
//                    return id;
//                }
//            }
//        } catch (SQLException e) {
//            throw new RuntimeException("Failed to save Customer email="
//                    + customer.getEmail() + ": " + e.getMessage(), e);
//        }
//        throw new RuntimeException("save Customer: no generated key returned");
//    }
//
//    // =========================================================================
//    // DELETE
//    // =========================================================================
//
//    @Override
//    public void delete(Long customerId) {
//        String sql = "DELETE FROM customer WHERE id = ?";
//
//        try (PreparedStatement ps = connection.prepareStatement(sql)) {
//            ps.setLong(1, customerId);
//            ps.executeUpdate();
//        } catch (SQLException e) {
//            throw new RuntimeException("Failed to delete Customer id=" + customerId + ": " + e.getMessage(), e);
//        }
//    }
//
//    // =========================================================================
//    // FIND BY BANK ID
//    // Customers are linked to banks through their accounts, so we join
//    // customer -> account on customer.id = account.customer_id and filter
//    // by account.bank_id. DISTINCT avoids duplicates for multi-account customers.
//    // =========================================================================
//
//    @Override
//    public List<Customer> findByBankId(Long bankId) {
//        String sql = "SELECT DISTINCT c.id, c.first_name, c.last_name, c.email, "
//                   + "               c.onboarding_date, c.created_at, c.updated_at "
//                   + "FROM customer c "
//                   + "JOIN account a ON a.customer_id = c.id "
//                   + "WHERE a.bank_id = ?";
//        List<Customer> list = new ArrayList<>();
//
//        try (PreparedStatement ps = connection.prepareStatement(sql)) {
//            ps.setLong(1, bankId);
//            try (ResultSet rs = ps.executeQuery()) {
//                while (rs.next()) {
//                    list.add(mapRow(rs));
//                }
//            }
//        } catch (SQLException e) {
//            throw new RuntimeException("Failed to find Customers for bankId=" + bankId + ": " + e.getMessage(), e);
//        }
//        return list;
//    }
//
//    // =========================================================================
//    // FIND ALL
//    // =========================================================================
//
//    @Override
//    public List<Customer> findAll() {
//        String sql = "SELECT id, first_name, last_name, email, onboarding_date, created_at, updated_at "
//                   + "FROM customer";
//        List<Customer> list = new ArrayList<>();
//
//        try (PreparedStatement ps = connection.prepareStatement(sql);
//             ResultSet rs = ps.executeQuery()) {
//            while (rs.next()) {
//                list.add(mapRow(rs));
//            }
//        } catch (SQLException e) {
//            throw new RuntimeException("Failed to fetch all Customers: " + e.getMessage(), e);
//        }
//        return list;
//    }
//
//    // =========================================================================
//    // FIND BY ID
//    // =========================================================================
//
//    @Override
//    public Customer findById(Long customerId) {
//        String sql = "SELECT id, first_name, last_name, email, onboarding_date, created_at, updated_at "
//                   + "FROM customer WHERE id = ?";
//
//        try (PreparedStatement ps = connection.prepareStatement(sql)) {
//            ps.setLong(1, customerId);
//            try (ResultSet rs = ps.executeQuery()) {
//                if (rs.next()) {
//                    return mapRow(rs);
//                }
//            }
//        } catch (SQLException e) {
//            throw new RuntimeException("Failed to find Customer id=" + customerId + ": " + e.getMessage(), e);
//        }
//        return null;
//    }
//
//    // =========================================================================
//    // FIND BY EMAIL
//    // =========================================================================
//
//    @Override
//    public Customer findByEmail(String email) {
//        String sql = "SELECT id, first_name, last_name, email, onboarding_date, created_at, updated_at "
//                   + "FROM customer WHERE email = ?";
//
//        try (PreparedStatement ps = connection.prepareStatement(sql)) {
//            ps.setString(1, email);
//            try (ResultSet rs = ps.executeQuery()) {
//                if (rs.next()) {
//                    return mapRow(rs);
//                }
//            }
//        } catch (SQLException e) {
//            throw new RuntimeException("Failed to find Customer email=" + email + ": " + e.getMessage(), e);
//        }
//        return null;
//    }
//
//    // =========================================================================
//    // UPDATE
//    // =========================================================================
//
//    @Override
//    public void update(Customer customer) {
//        String sql = "UPDATE customer SET email = ?, updated_at = NOW() WHERE id = ?";
//
//        try (PreparedStatement ps = connection.prepareStatement(sql)) {
//            ps.setString(1, customer.getEmail());
//            ps.setLong(2, customer.getId());
//            ps.executeUpdate();
//        } catch (SQLException e) {
//            throw new RuntimeException("Failed to update Customer id=" + customer.getId() + ": " + e.getMessage(), e);
//        }
//    }
//
//    // =========================================================================
//    // PRIVATE HELPER
//    // accounts are loaded separately via AccountDao — passed as empty list here
//    // =========================================================================
//
//    private Customer mapRow(ResultSet rs) throws SQLException {
//        Timestamp createdAt = rs.getTimestamp("created_at");
//        Timestamp updatedAt = rs.getTimestamp("updated_at");
//
//        Customer customer = new Customer(
//                rs.getLong("id"),
//                createdAt != null ? createdAt.toLocalDateTime() : null,
//                updatedAt != null ? updatedAt.toLocalDateTime() : null,
//                rs.getString("first_name"),
//                rs.getString("last_name"),
//                rs.getString("email"),
//                rs.getDate("onboarding_date") != null
//                        ? rs.getDate("onboarding_date").toLocalDate() : null,
//                new ArrayList<>()   // accounts loaded separately via AccountDao
//        );
//        return customer;
//    }
//}