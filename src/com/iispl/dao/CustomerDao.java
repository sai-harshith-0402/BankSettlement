package com.iispl.dao;

import com.iispl.entity.Customer;

import java.sql.Connection;

public interface CustomerDao {

    /**
     * Inserts a new Customer row (without accounts — accounts saved separately via AccountDao).
     * Returns the generated DB id.
     */
    long save(Customer customer, Connection conn);

    /**
     * Updates email for an existing customer.
     */
    void update(Customer customer, Connection conn);

    /**
     * Finds a customer by generated DB id.
     */
    Customer findById(long customerId, Connection conn);

    /**
     * Finds a customer by email (unique business key).
     */
    Customer findByEmail(String email, Connection conn);
}