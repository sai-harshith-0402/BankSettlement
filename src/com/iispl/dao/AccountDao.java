package com.iispl.dao;

import com.iispl.entity.Account;
import com.iispl.enums.AccountType;

import java.math.BigDecimal;
import java.util.List;

public interface AccountDao {

    // Persist a new account record
    Account saveAccount(Account account);

    // Update all mutable fields of an existing account
    Account updateAccount(Account account);

    // Delete an account by its primary key
    void deleteAccount(Long id);

    // Fetch a single account by primary key; returns null if not found
    Account findAccountById(Long id);

    // Fetch all accounts in the system
    List<Account> findAllAccounts();

    // Fetch all accounts belonging to a specific customer
    List<Account> findAccountByCustomerId(Long customerId);

    // Update only the balance field of an account
    void updateBalance(Long accountId, BigDecimal newBalance);

    // Fetch an account by its unique account number; returns null if not found
    Account findAccountByAccountNumber(String accountNumber);

    // Fetch all accounts linked to a specific bank
    List<Account> findAccountsByBankId(Long bankId);

    // Fetch all accounts filtered by account type (SAVINGS, CURRENT, NOSTRO, etc.)
    List<Account> findAccountsByType(AccountType accountType);

    // Update only the status field (ACTIVE, INACTIVE, BLOCKED, etc.)
    void updateStatus(Long accountId, String status);
}