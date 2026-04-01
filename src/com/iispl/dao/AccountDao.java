package com.iispl.dao;

import com.iispl.entity.Account;
import com.iispl.enums.AccountType;

import java.math.BigDecimal;
import java.sql.Connection;
import java.util.List;

public interface AccountDao {

    /**
     * Inserts a new Account row. Returns the generated DB id.
     */
    long save(Account account, Connection conn);

    /**
     * Updates balance and status for an existing account.
     */
    void update(Account account, Connection conn);

    /**
     * Finds an account by its generated id.
     */
    Account findById(long accountId, Connection conn);

    /**
     * Finds an account by its account number (unique business key).
     */
    Account findByAccountNumber(String accountNumber, Connection conn);

    /**
     * Returns all accounts belonging to a customer.
     */
    List<Account> findByCustomerId(long customerId, Connection conn);

    /**
     * Returns all accounts of a given type held at a specific bank.
     * Useful for locating NOSTRO / VOSTRO accounts during settlement.
     */
    List<Account> findByBankIdAndType(long bankId, AccountType accountType, Connection conn);

    /**
     * Applies a credit (adds amount) to an account's balance atomically.
     */
    void credit(long accountId, BigDecimal amount, Connection conn);

    /**
     * Applies a debit (subtracts amount) to an account's balance atomically.
     */
    void debit(long accountId, BigDecimal amount, Connection conn);
}