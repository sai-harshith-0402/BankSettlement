package com.iispl.dao;

import com.iispl.entity.Customer;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Optional;

public class CustomerDaoImpl implements CustomerDao {

    // =========================================================================
    // SAVE
    // =========================================================================

    @Override
    public long save(Customer customer, Connection conn) {
        String sql = "INSERT INTO customer (first_name, last_name, email, onboarding_date, created_at, updated_at) " +
                     "VALUES (?, ?, ?, ?, NOW(), NOW())";

        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, customer.getFirstName());
            ps.setString(2, customer.getLastName());
            ps.setString(3, customer.getEmail());
            ps.setDate(4, customer.getOnboardingDate() != null
                    ? Date.valueOf(customer.getOnboardingDate()) : null);
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
                    + customer.getEmail() + ": " + e.getMessage(), e);
        }
        throw new RuntimeException("save Customer: no generated key returned");
    }

    // =========================================================================
    // UPDATE
    // =========================================================================

    @Override
    public void update(Customer customer, Connection conn) {
        String sql = "UPDATE customer SET email = ?, updated_at = NOW() WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, customer.getEmail());
            ps.setLong(2, customer.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update Customer id=" + customer.getId(), e);
        }
    }

    // =========================================================================
    // FIND BY ID
    // =========================================================================

    @Override
    public Optional<Customer> findById(long customerId, Connection conn) {
        String sql = "SELECT id, first_name, last_name, email, onboarding_date " +
                     "FROM customer WHERE id = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find Customer id=" + customerId, e);
        }
        return Optional.empty();
    }

    // =========================================================================
    // FIND BY EMAIL
    // =========================================================================

    @Override
    public Optional<Customer> findByEmail(String email, Connection conn) {
        String sql = "SELECT id, first_name, last_name, email, onboarding_date " +
                     "FROM customer WHERE email = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find Customer email=" + email, e);
        }
        return Optional.empty();
    }

    // =========================================================================
    // PRIVATE HELPER
    // =========================================================================

    private Customer mapRow(ResultSet rs) throws SQLException {
        Customer customer = new Customer(
                rs.getString("first_name"),
                rs.getString("last_name"),
                rs.getString("email"),
                rs.getDate("onboarding_date") != null
                        ? rs.getDate("onboarding_date").toLocalDate() : null,
                new ArrayList<>()   // accounts loaded separately via AccountDao
        );
        customer.setId(rs.getLong("id"));
        return customer;
    }
}