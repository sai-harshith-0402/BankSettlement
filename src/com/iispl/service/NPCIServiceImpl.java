package com.iispl.service;

import com.iispl.entity.NPCI;
import com.iispl.entity.NPCIBank;

import java.math.BigDecimal;
import java.util.List;
import java.util.logging.Logger;

public class NPCIServiceImpl implements NPCIService {

    private static final Logger logger = Logger.getLogger(NPCIServiceImpl.class.getName());

    // NPCI holds the master list of all registered banks
    private final NPCI npci;

    public NPCIServiceImpl(NPCI npci) {
        this.npci = npci;
    }

    // =========================================================================
    // GET NPCI
    // =========================================================================

    @Override
    public NPCI getNPCI() {
        return npci;
    }

    // =========================================================================
    // FIND BY BANK NAME
    // =========================================================================

    @Override
    public NPCIBank findByBankName(String bankName) {
        if (bankName == null || bankName.isBlank()) {
            logger.warning("[NPCIService] findByBankName called with null/blank bankName");
            return null;
        }
        for (NPCIBank bank : npci.getNpciBanksList()) {
            if (bank.getBankName().equalsIgnoreCase(bankName)) {
                return bank;
            }
        }
        logger.warning("[NPCIService] Bank not found: " + bankName);
        return null;
    }

    // =========================================================================
    // CREDIT BALANCE
    // =========================================================================

    @Override
    public void creditBalance(String bankName, BigDecimal amount) {
        NPCIBank bank = findByBankName(bankName);
        if (bank == null) {
            logger.severe("[NPCIService] creditBalance failed — bank not found: " + bankName);
            return;
        }
        BigDecimal before = bank.getBalanceAmount();
        bank.setBalanceAmount(before.add(amount));
        logger.info("[NPCIService] CREDIT | Bank: " + bankName
                + " | Before: " + before
                + " | Amount: +" + amount
                + " | After: " + bank.getBalanceAmount());
    }

    // =========================================================================
    // DEBIT BALANCE
    // =========================================================================

    @Override
    public void debitBalance(String bankName, BigDecimal amount) {
        NPCIBank bank = findByBankName(bankName);
        if (bank == null) {
            logger.severe("[NPCIService] debitBalance failed — bank not found: " + bankName);
            return;
        }
        BigDecimal before = bank.getBalanceAmount();
        bank.setBalanceAmount(before.subtract(amount));
        logger.info("[NPCIService] DEBIT | Bank: " + bankName
                + " | Before: " + before
                + " | Amount: -" + amount
                + " | After: " + bank.getBalanceAmount());
    }

    // =========================================================================
    // GET BALANCE
    // =========================================================================

    @Override
    public BigDecimal getBalance(String bankName) {
        NPCIBank bank = findByBankName(bankName);
        if (bank == null) {
            logger.warning("[NPCIService] getBalance — bank not found: " + bankName);
            return null;
        }
        return bank.getBalanceAmount();
    }

    // =========================================================================
    // GET ALL BANKS
    // =========================================================================

    @Override
    public List<NPCIBank> getAllBanks() {
        return npci.getNpciBanksList();
    }
}