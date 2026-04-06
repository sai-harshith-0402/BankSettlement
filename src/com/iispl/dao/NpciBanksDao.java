package com.iispl.dao;

import com.iispl.entity.NPCIBank;

import java.math.BigDecimal;
import java.util.List;

/**
 * Data Access contract for NPCIBank entities.
 *
 * NPCIBank represents each member bank registered under NPCI
 * along with its current settlement balance. These records are
 * read frequently during UPI / NEFT settlement runs and updated
 * after each netting cycle.
 */
public interface NpciBanksDao {

    // -----------------------------------------------------------------------
    // Write operations
    // -----------------------------------------------------------------------

    /** Persists a new NPCIBank record. */
    void saveNPCIBank(NPCIBank npcIBank);

    /** Removes an NPCIBank record by its bankId. */
    void deleteNPCIBank(String bankId);

    // -----------------------------------------------------------------------
    // Read operations
    // -----------------------------------------------------------------------

    /** Returns every NPCIBank record in the table. */
    List<NPCIBank> findAllNPCIBanks();

    /** Returns the NPCIBank matching the given bankId, or null if not found. */
    NPCIBank findNPCIBankById(String bankId);

    /** Returns the NPCIBank matching the given bankName, or null if not found. */
    NPCIBank findNPCIBankByName(String bankName);

    /**
     * Returns all NPCIBanks whose balanceAmount is below the given threshold.
     * Used to flag banks with insufficient settlement funds before a netting run.
     */
    List<NPCIBank> findBanksBelowBalanceThreshold(BigDecimal threshold);

    // -----------------------------------------------------------------------
    // Update operations
    // -----------------------------------------------------------------------

    /**
     * Updates the balanceAmount for a given bank.
     * Called after each netting / settlement cycle to reflect the new position.
     */
    void updateBalance(String bankId, BigDecimal newBalance);

    /**
     * Credits an amount to a bank's balance (balance += amount).
     * Used when a bank is on the receiving end of a net settlement.
     */
    void creditBalance(String bankId, BigDecimal amount);

    /**
     * Debits an amount from a bank's balance (balance -= amount).
     * Used when a bank is on the paying end of a net settlement.
     */
    void debitBalance(String bankId, BigDecimal amount);
}