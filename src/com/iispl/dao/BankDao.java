package com.iispl.dao;

import com.iispl.entity.Bank;

import java.util.List;

public interface BankDao {

    // Persist a new bank record
    Bank saveBank(Bank bank);

    // Toggle or set the isActive status of a bank
    void changeStatus(Long bankId, boolean isActive);

    // Fetch all banks in the system
    List<Bank> findAllBanks();

    // Fetch a single bank by its primary key; returns null if not found
    Bank findBankById(Long id);

    // Fetch a bank by its unique bank code; returns null if not found
    Bank findBankByBankCode(String bankCode);

    // Fetch a bank by its IFSC code; returns null if not found
    Bank findBankByIfscCode(String ifscCode);

    // Fetch only active banks (isActive = true)
    List<Bank> findAllActiveBanks();

    // Update mutable fields of an existing bank record
    Bank updateBank(Bank bank);

    // Remove a bank record by its primary key
    void deleteBank(Long bankId);
}