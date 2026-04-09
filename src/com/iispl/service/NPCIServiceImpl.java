package com.iispl.service;

import com.iispl.dao.NpciBanksDao;
import com.iispl.entity.NPCI;
import com.iispl.entity.NPCIBank;

import java.math.BigDecimal;
import java.util.List;
import java.util.logging.Logger;

public class NPCIServiceImpl implements NPCIService {

    private static final Logger logger = Logger.getLogger(NPCIServiceImpl.class.getName());

    private final NPCI         npci;
    private final NpciBanksDao npciBanksDao;

    public NPCIServiceImpl(NPCI npci, NpciBanksDao npciBanksDao) {
        this.npci         = npci;
        this.npciBanksDao = npciBanksDao;
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
    // Checks in-memory list first (fast path); falls back to DB if not found.
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
        // Fallback: check DB
        NPCIBank bank = npciBanksDao.findNPCIBankByName(bankName);
        if (bank == null) {
            logger.warning("[NPCIService] Bank not found in memory or DB: " + bankName);
        }
        return bank;
    }

    // =========================================================================
    // CREDIT BALANCE
    // Updates in-memory NPCIBank AND persists to DB via NpciBanksDao.
    // =========================================================================

    @Override
    public void creditBalance(String bankName, BigDecimal amount) {
        NPCIBank bank = findByBankName(bankName);
        if (bank == null) {
            logger.severe("[NPCIService] creditBalance failed — bank not found: " + bankName);
            return;
        }
        BigDecimal before = bank.getBalanceAmount();
        BigDecimal after  = before.add(amount);

        // Update in-memory
        bank.setBalanceAmount(after);

        // Persist to DB
        npciBanksDao.creditBalance(String.valueOf(bank.getBankId()), amount);

        logger.info("[NPCIService] CREDIT | Bank: " + bankName
                + " | Before: " + before
                + " | Amount: +" + amount
                + " | After: " + after);
    }

    // =========================================================================
    // DEBIT BALANCE
    // Updates in-memory NPCIBank AND persists to DB via NpciBanksDao.
    // =========================================================================

    @Override
    public void debitBalance(String bankName, BigDecimal amount) {
        NPCIBank bank = findByBankName(bankName);
        if (bank == null) {
            logger.severe("[NPCIService] debitBalance failed — bank not found: " + bankName);
            return;
        }
        BigDecimal before = bank.getBalanceAmount();
        BigDecimal after  = before.subtract(amount);

        // Update in-memory
        bank.setBalanceAmount(after);

        // Persist to DB
        npciBanksDao.debitBalance(String.valueOf(bank.getBankId()), amount);

        logger.info("[NPCIService] DEBIT | Bank: " + bankName
                + " | Before: " + before
                + " | Amount: -" + amount
                + " | After: " + after);
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