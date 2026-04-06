package com.iispl.dao;

import com.iispl.entity.Customer;

import java.util.List;

public interface CustomerDao {

    // Persist a new Customer row (accounts saved separately via AccountDao);
    // returns the generated DB id
    long save(Customer customer);

    // Hard-delete a customer record by primary key
    void delete(Long customerId);

    // Fetch all customers that have at least one account linked to the given bank
    List<Customer> findByBankId(Long bankId);

    // Fetch every customer row in the system
    List<Customer> findAll();

    // Fetch a single customer by primary key; returns null if not found
    Customer findById(Long customerId);

    // Fetch a single customer by email (unique business key); returns null if not found
    Customer findByEmail(String email);

    // Update mutable fields (email) on an existing customer row
    void update(Customer customer);
}