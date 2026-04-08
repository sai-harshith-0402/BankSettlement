package com.iispl.service;

import com.iispl.entity.NPCI;
import com.iispl.entity.NPCIBank;

import java.math.BigDecimal;
import java.util.List;

public interface NPCIService {

    /**
     * Returns the full NPCI entity containing all registered NPCIBanks.
     */
    NPCI getNPCI();

    /**
     * Looks up an NPCIBank by its exact bank name.
     * Returns null if not found.
     *
     * @param bankName  the fromBankName or toBankName from a transaction
     * @return          matching NPCIBank or null
     */
    NPCIBank findByBankName(String bankName);

    /**
     * Credits a bank's balance (used during CREDIT transaction settlement).
     * toBank.balance += amount
     *
     * @param bankName  the bank to credit
     * @param amount    amount to add
     */
    void creditBalance(String bankName, BigDecimal amount);

    /**
     * Debits a bank's balance (used during DEBIT transaction settlement).
     * fromBank.balance -= amount
     *
     * @param bankName  the bank to debit
     * @param amount    amount to subtract
     */
    void debitBalance(String bankName, BigDecimal amount);

    /**
     * Returns current balance for a bank by name.
     *
     * @param bankName  bank name to query
     * @return          current balance or null if bank not found
     */
    BigDecimal getBalance(String bankName);

    /**
     * Returns all registered NPCI banks.
     */
    List<NPCIBank> getAllBanks();
}