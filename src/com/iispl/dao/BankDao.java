package com.iispl.dao;

import com.iispl.entity.Bank;

import java.sql.Connection;
import java.util.List;

public interface BankDao {

    /**
     * Inserts a new Bank row. Returns the generated DB id.
     */
    long save(Bank bank, Connection conn);

    /**
     * Finds a bank by its generated DB id.
     */
    Bank findById(long bankId, Connection conn);

    /**
     * Finds a bank by its bankCode (unique business key — e.g. BIC or IFSC prefix).
     */
    Bank findByBankCode(String bankCode, Connection conn);

    /**
     * Returns all active banks — used during adapter mapping to resolve
     * fromBankCode / toBankCode strings to Bank objects.
     */
    List<Bank> findAllActive(Connection conn);
}