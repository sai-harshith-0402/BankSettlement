package com.iispl.utility;

import com.iispl.connectionpool.ConnectionPool;
import com.iispl.dao.*;
import com.iispl.entity.*;
import com.iispl.enums.*;
import com.iispl.ingestion.*;
import com.iispl.service.*;
import com.iispl.threading.PipelineOrchestrator;

import java.math.BigDecimal;
import java.sql.Connection;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ╔══════════════════════════════════════════════════════════════════╗
 * ║         IISPL — Bank Settlement Utility  (Entry Point)          ║
 * ║                                                                  ║
 * ║  Wires every layer of the project:                              ║
 * ║   ConnectionPool → DAOs → Services → Adapters → Threading       ║
 * ╚══════════════════════════════════════════════════════════════════╝
 *
 * Menu Map
 * ─────────────────────────────────────────────
 * 1. Bank Management
 *    1.1 Add Bank
 *    1.2 Find Bank by ID
 *    1.3 Find Bank by Code
 *    1.4 List All Active Banks
 *
 * 2. Customer Management
 *    2.1 Add Customer
 *    2.2 Find Customer by ID
 *    2.3 Find Customer by Email
 *    2.4 Update Customer Email
 *
 * 3. Account Management
 *    3.1 Open New Account
 *    3.2 Find Account by Number
 *    3.3 List Accounts for Customer
 *    3.4 Credit Account
 *    3.5 Debit Account
 *    3.6 Find NOSTRO / VOSTRO Accounts for Bank
 *
 * 4. Transaction Management
 *    4.1 Record Incoming Transaction
 *    4.2 Update Incoming Transaction Status
 *    4.3 Find Transaction by ID
 *    4.4 Find Transactions by Batch DB-ID
 *    4.5 Update Transaction Status
 *    4.6 Process Single Transaction (immediate settlement)
 *
 * 5. Batch & Settlement Processing
 *    5.1 Read & Adapt from Source File
 *    5.2 Group Transactions (Date + Channel)
 *    5.3 Process Batch
 *    5.4 Build Batch ID Manually
 *    5.5 Resolve Settlement Date for a TXN
 *    5.6 Validate an Incoming Transaction
 *    5.7 Export Settlement Result to File
 *
 * 6. Settlement Records (DB)
 *    6.1 Find Settlement Record by ID
 *    6.2 Find Records by Date
 *    6.3 Find Records by Status
 *    6.4 Update Settlement Status
 *
 * 7. Reports
 *    7.1 Settlement Summary for Date
 *    7.2 Bank-wise Account Summary
 *    7.3 Customer Account Balance View
 *    7.4 Batch Netting Report
 *
 * 8. Full Pipeline (Orchestrator — all sources in parallel)
 *
 * 0. Exit
 */
public class BankSettlementUtility {

    // =========================================================================
    // CONSTANTS — settlement window minutes per channel (mirrors BatchScheduler)
    // =========================================================================
    private static final Map<ChannelType, Integer> WINDOW_MINUTES = Map.of(
            ChannelType.UPI,      15,
            ChannelType.NEFT,     60,
            ChannelType.RTGS,     0,   // real-time, no window
            ChannelType.SWIFT,    0,
            ChannelType.ACH,      30,
            ChannelType.INTERNAL, 0
    );

    // =========================================================================
    // DAO layer
    // =========================================================================
    private final BankDao        bankDao        = new BankDaoImpl();
    private final CustomerDao    customerDao    = new CustomerDaoImpl();
    private final AccountDao     accountDao     = new AccountDaoImpl();
    private final TransactionDao transactionDao = new TransactionDaoImpl();
    private final SettlementDao  settlementDao  = new SettlementDaoImpl();

    // =========================================================================
    // Source Systems — loaded at startup (DB-backed in production)
    // =========================================================================
    private final SourceSystem cbsSrc = new SourceSystem(
            1L, null, null, SourceType.CBS,     "resources/cbs_transactions.xlsx",     true);
    private final SourceSystem rtgsSrc = new SourceSystem(
            2L, null, null, SourceType.RTGS,    "resources/rtgs_transactions.xlsx",    true);
    private final SourceSystem swiftSrc = new SourceSystem(
            3L, null, null, SourceType.SWIFT,   "resources/swift_transactions.xlsx",   true);
    private final SourceSystem neftSrc = new SourceSystem(
            4L, null, null, SourceType.NEFT,    "resources/neft_transactions.xlsx",    true);
    private final SourceSystem fintechSrc = new SourceSystem(
            5L, null, null, SourceType.FINTECH, "resources/fintech_transactions.xlsx", true);

    private final List<SourceSystem> allSources;

    // =========================================================================
    // Service layer
    // =========================================================================
    private final AdapterRegistry   adapterRegistry;
    private final SettlementService settlementService;
    private final BatchService      batchService;

    // =========================================================================
    // Batch ID sequence counter (mirrors BatchServiceImpl behaviour)
    // =========================================================================
    private final AtomicInteger batchSeq = new AtomicInteger(1);

    // =========================================================================
    // I/O
    // =========================================================================
    private final Scanner sc = new Scanner(System.in);

    // ─────────────────────────────────────────────────────────────────────────
    public BankSettlementUtility() {
        allSources = List.of(cbsSrc, rtgsSrc, swiftSrc, neftSrc, fintechSrc);

        adapterRegistry = new AdapterRegistry();
        adapterRegistry.register(SourceType.CBS,     new CbsAdapter(cbsSrc));
        adapterRegistry.register(SourceType.RTGS,    new RtgsAdapter(rtgsSrc));
        adapterRegistry.register(SourceType.SWIFT,   new SwiftAdapter(swiftSrc));
        adapterRegistry.register(SourceType.NEFT,    new NeftUpiAdapter(neftSrc));
        adapterRegistry.register(SourceType.FINTECH, new FintechAdapter(fintechSrc));

        settlementService = new SettlementServiceImpl();
        batchService      = new BatchServiceImpl(adapterRegistry, settlementService);
    }

    // =========================================================================
    // ENTRY POINT
    // =========================================================================
    public static void main(String[] args) {
        new BankSettlementUtility().run();
    }

    public void run() {
        printBanner();
        int choice;
        do {
            printMainMenu();
            choice = readInt("Enter choice");
            switch (choice) {
                case 1 -> bankMenu();
                case 2 -> customerMenu();
                case 3 -> accountMenu();
                case 4 -> transactionMenu();
                case 5 -> batchMenu();
                case 6 -> settlementRecordsMenu();
                case 7 -> reportMenu();
                case 8 -> runFullPipeline();
                case 0 -> System.out.println("\n  Goodbye! — IISPL Settlement Platform shutting down.");
                default -> System.out.println("  ✗ Invalid choice. Please try again.");
            }
        } while (choice != 0);
        sc.close();
    }

    // =========================================================================
    // 1. BANK MANAGEMENT
    // =========================================================================
    private void bankMenu() {
        int ch;
        do {
            System.out.println("\n  ╔═══ 1. BANK MANAGEMENT ═══════════════════╗");
            System.out.println("  ║  1. Add New Bank                         ║");
            System.out.println("  ║  2. Find Bank by ID                      ║");
            System.out.println("  ║  3. Find Bank by Code                    ║");
            System.out.println("  ║  4. List All Active Banks                ║");
            System.out.println("  ║  0. Back                                 ║");
            System.out.println("  ╚══════════════════════════════════════════╝");
            ch = readInt("Choice");
            switch (ch) {
                case 1 -> doAddBank();
                case 2 -> doFindBankById();
                case 3 -> doFindBankByCode();
                case 4 -> doListActiveBanks();
                case 0 -> {}
                default -> System.out.println("  ✗ Invalid.");
            }
        } while (ch != 0);
    }

    private void doAddBank() {
        System.out.println("\n  --- Add New Bank ---");
        String code    = readString("Bank Code (e.g. SBI01)");
        String name    = readString("Bank Name");
        String ifsc    = readString("IFSC Code");
        boolean active = readYesNo("Active? [y/n]");
        Bank bank = new Bank(null, null, null, code, name, ifsc, active);
        try (Connection conn = getConn()) {
            long id = bankDao.save(bank, conn);
            System.out.println("  ✓ Bank saved — ID: " + id);
        } catch (Exception e) { err(e); }
    }

    private void doFindBankById() {
        long id = readLong("Bank ID");
        try (Connection conn = getConn()) {
            Bank b = bankDao.findById(id, conn);
            if (b != null) printBank(b); else System.out.println("  ✗ Bank not found.");
        } catch (Exception e) { err(e); }
    }

    private void doFindBankByCode() {
        String code = readString("Bank Code");
        try (Connection conn = getConn()) {
            Bank b = bankDao.findByBankCode(code, conn);
            if (b != null) printBank(b); else System.out.println("  ✗ Bank not found.");
        } catch (Exception e) { err(e); }
    }

    private void doListActiveBanks() {
        try (Connection conn = getConn()) {
            List<Bank> banks = bankDao.findAllActive(conn);
            if (banks.isEmpty()) { System.out.println("  No active banks."); return; }
            System.out.printf("%n  %-6s %-12s %-30s %-15s%n", "ID", "Code", "Name", "IFSC");
            sep(67);
            banks.forEach(b -> System.out.printf("  %-6d %-12s %-30s %-15s%n",
                    b.getId(), b.getBankCode(), b.getBankName(), b.getIfscCode()));
        } catch (Exception e) { err(e); }
    }

    // =========================================================================
    // 2. CUSTOMER MANAGEMENT
    // =========================================================================
    private void customerMenu() {
        int ch;
        do {
            System.out.println("\n  ╔═══ 2. CUSTOMER MANAGEMENT ════════════════╗");
            System.out.println("  ║  1. Add Customer                         ║");
            System.out.println("  ║  2. Find Customer by ID                  ║");
            System.out.println("  ║  3. Find Customer by Email               ║");
            System.out.println("  ║  4. Update Customer Email                ║");
            System.out.println("  ║  0. Back                                 ║");
            System.out.println("  ╚══════════════════════════════════════════╝");
            ch = readInt("Choice");
            switch (ch) {
                case 1 -> doAddCustomer();
                case 2 -> doFindCustomerById();
                case 3 -> doFindCustomerByEmail();
                case 4 -> doUpdateCustomerEmail();
                case 0 -> {}
                default -> System.out.println("  ✗ Invalid.");
            }
        } while (ch != 0);
    }

    private void doAddCustomer() {
        System.out.println("\n  --- Add Customer ---");
        String first   = readString("First Name");
        String last    = readString("Last Name");
        String email   = readString("Email");
        LocalDate date = readDate("Onboarding Date [YYYY-MM-DD]");
        Customer c = new Customer(null, null, null, first, last, email, date, new ArrayList<>());
        try (Connection conn = getConn()) {
            long id = customerDao.save(c, conn);
            System.out.println("  ✓ Customer saved — ID: " + id);
        } catch (Exception e) { err(e); }
    }

    private void doFindCustomerById() {
        long id = readLong("Customer ID");
        try (Connection conn = getConn()) {
            Customer c = customerDao.findById(id, conn);
            if (c != null) printCustomer(c); else System.out.println("  ✗ Not found.");
        } catch (Exception e) { err(e); }
    }

    private void doFindCustomerByEmail() {
        String email = readString("Email");
        try (Connection conn = getConn()) {
            Customer c = customerDao.findByEmail(email, conn);
            if (c != null) printCustomer(c); else System.out.println("  ✗ Not found.");
        } catch (Exception e) { err(e); }
    }

    private void doUpdateCustomerEmail() {
        long id       = readLong("Customer ID");
        String email  = readString("New Email");
        try (Connection conn = getConn()) {
            Customer c = customerDao.findById(id, conn);
            if (c == null) { System.out.println("  ✗ Customer not found."); return; }
            Customer updated = new Customer(
                    c.getId(), c.getCreatedAt(), c.getUpdatedAt(),
                    c.getFirstName(), c.getLastName(), email,
                    c.getOnboardingDate(), c.getAccountList());
            customerDao.update(updated, conn);
            System.out.println("  ✓ Email updated.");
        } catch (Exception e) { err(e); }
    }

    // =========================================================================
    // 3. ACCOUNT MANAGEMENT
    // =========================================================================
    private void accountMenu() {
        int ch;
        do {
            System.out.println("\n  ╔═══ 3. ACCOUNT MANAGEMENT ═════════════════╗");
            System.out.println("  ║  1. Open New Account                     ║");
            System.out.println("  ║  2. Find Account by Number               ║");
            System.out.println("  ║  3. List Accounts for Customer           ║");
            System.out.println("  ║  4. Credit Account                       ║");
            System.out.println("  ║  5. Debit Account                        ║");
            System.out.println("  ║  6. Find NOSTRO / VOSTRO for Bank        ║");
            System.out.println("  ║  0. Back                                 ║");
            System.out.println("  ╚══════════════════════════════════════════╝");
            ch = readInt("Choice");
            switch (ch) {
                case 1 -> doOpenAccount();
                case 2 -> doFindAccountByNumber();
                case 3 -> doListAccountsForCustomer();
                case 4 -> doCreditAccount();
                case 5 -> doDebitAccount();
                case 6 -> doFindNostroVostro();
                case 0 -> {}
                default -> System.out.println("  ✗ Invalid.");
            }
        } while (ch != 0);
    }

    private void doOpenAccount() {
        System.out.println("\n  --- Open New Account ---");
        long        customerId = readLong("Customer ID");
        long        bankId     = readLong("Bank ID");
        String      accNum     = readString("Account Number");
        AccountType type       = pickEnum("Account Type", AccountType.values());
        BigDecimal  bal        = readDecimal("Opening Balance");
        Account acc = new Account(null, null, null, accNum, type, customerId, bankId, bal, "ACTIVE");
        try (Connection conn = getConn()) {
            long id = accountDao.save(acc, conn);
            System.out.println("  ✓ Account opened — ID: " + id);
        } catch (Exception e) { err(e); }
    }

    private void doFindAccountByNumber() {
        String num = readString("Account Number");
        try (Connection conn = getConn()) {
            Account a = accountDao.findByAccountNumber(num, conn);
            if (a != null) printAccount(a); else System.out.println("  ✗ Account not found.");
        } catch (Exception e) { err(e); }
    }

    private void doListAccountsForCustomer() {
        long customerId = readLong("Customer ID");
        try (Connection conn = getConn()) {
            List<Account> list = accountDao.findByCustomerId(customerId, conn);
            if (list.isEmpty()) { System.out.println("  No accounts found."); return; }
            list.forEach(this::printAccount);
        } catch (Exception e) { err(e); }
    }

    private void doCreditAccount() {
        long       id  = readLong("Account ID");
        BigDecimal amt = readDecimal("Amount to Credit (₹)");
        try (Connection conn = getConn()) {
            accountDao.credit(id, amt, conn);
            Account a = accountDao.findById(id, conn);
            System.out.println("  ✓ Credited ₹" + amt + "  |  New Balance: ₹"
                    + (a != null ? a.getBalance() : "N/A"));
        } catch (Exception e) { err(e); }
    }

    private void doDebitAccount() {
        long       id  = readLong("Account ID");
        BigDecimal amt = readDecimal("Amount to Debit (₹)");
        try (Connection conn = getConn()) {
            accountDao.debit(id, amt, conn);
            Account a = accountDao.findById(id, conn);
            System.out.println("  ✓ Debited ₹" + amt + "  |  New Balance: ₹"
                    + (a != null ? a.getBalance() : "N/A"));
        } catch (Exception e) { err(e); }
    }

    private void doFindNostroVostro() {
        long bankId = readLong("Bank ID");
        System.out.println("  1. NOSTRO   2. VOSTRO   3. CORRESPONDENT");
        int pick = readInt("Type");
        AccountType type = switch (pick) {
            case 1 -> AccountType.NOSTRO;
            case 2 -> AccountType.VOSTRO;
            default -> AccountType.CORRESPONDENT;
        };
        try (Connection conn = getConn()) {
            List<Account> list = accountDao.findByBankIdAndType(bankId, type, conn);
            if (list.isEmpty()) { System.out.println("  No " + type + " accounts found."); return; }
            System.out.println("  " + type + " accounts for Bank ID " + bankId + ":");
            list.forEach(this::printAccount);
        } catch (Exception e) { err(e); }
    }

    // =========================================================================
    // 4. TRANSACTION MANAGEMENT
    // =========================================================================
    private void transactionMenu() {
        int ch;
        do {
            System.out.println("\n  ╔═══ 4. TRANSACTION MANAGEMENT ═════════════╗");
            System.out.println("  ║  1. Record Incoming Transaction          ║");
            System.out.println("  ║  2. Update Incoming Transaction Status   ║");
            System.out.println("  ║  3. Find Transaction by ID               ║");
            System.out.println("  ║  4. Find Transactions by Batch DB-ID     ║");
            System.out.println("  ║  5. Update Transaction Status            ║");
            System.out.println("  ║  6. Process Single Transaction           ║");
            System.out.println("  ║  0. Back                                 ║");
            System.out.println("  ╚══════════════════════════════════════════╝");
            ch = readInt("Choice");
            switch (ch) {
                case 1 -> doRecordIncoming();
                case 2 -> doUpdateIncomingStatus();
                case 3 -> doFindTransactionById();
                case 4 -> doFindTransactionsByBatch();
                case 5 -> doUpdateTransactionStatus();
                case 6 -> doProcessSingleTransaction();
                case 0 -> {}
                default -> System.out.println("  ✗ Invalid.");
            }
        } while (ch != 0);
    }

    private void doRecordIncoming() {
        System.out.println("\n  --- Record Incoming Transaction ---");
        TransactionType txnType = pickEnum("Transaction Type", TransactionType.values());
        BigDecimal      amount  = readDecimal("Amount (₹)");
        SourceSystem    src     = pickSourceSystem();
        if (src == null) return;

        IncomingTransaction txn = new IncomingTransaction(
                null, null, null,
                src.getId(), txnType, amount,
                LocalDateTime.now(), ProcessingStatus.RECEIVED, src, null);

        try (Connection conn = getConn()) {
            long id = transactionDao.saveIncoming(txn, conn);
            System.out.println("  ✓ Incoming transaction saved — ID: " + id);
        } catch (Exception e) { err(e); }
    }

    private void doUpdateIncomingStatus() {
        long            id     = readLong("Incoming Transaction ID");
        ProcessingStatus status = pickEnum("New Status", ProcessingStatus.values());
        try (Connection conn = getConn()) {
            transactionDao.updateIncomingStatus(id, status.name(), conn);
            System.out.println("  ✓ Processing status updated to " + status);
        } catch (Exception e) { err(e); }
    }

    private void doFindTransactionById() {
        long id = readLong("Transaction ID");
        try (Connection conn = getConn()) {
            Transaction txn = transactionDao.findById(id, conn);
            if (txn != null) printTransaction(txn); else System.out.println("  ✗ Not found.");
        } catch (Exception e) { err(e); }
    }

    private void doFindTransactionsByBatch() {
        long batchDbId = readLong("Settlement Batch DB-ID");
        try (Connection conn = getConn()) {
            List<Transaction> txns = transactionDao.findByBatchId(batchDbId, conn);
            if (txns.isEmpty()) { System.out.println("  No transactions found."); return; }
            System.out.println("  Found " + txns.size() + " transaction(s):");
            txns.forEach(this::printTransaction);
            printTransactionTotals(txns);
        } catch (Exception e) { err(e); }
    }

    private void doUpdateTransactionStatus() {
        long              id     = readLong("Transaction ID");
        TransactionStatus status = pickEnum("New Status", TransactionStatus.values());
        try (Connection conn = getConn()) {
            transactionDao.updateTransactionStatus(id, status, conn);
            System.out.println("  ✓ Transaction status updated to " + status);
        } catch (Exception e) { err(e); }
    }

    private void doProcessSingleTransaction() {
        System.out.println("\n  --- Process Single Transaction (immediate settlement) ---");
        TransactionType txnType = pickEnum("Transaction Type", TransactionType.values());
        BigDecimal      amount  = readDecimal("Amount (₹)");
        SourceSystem    src     = pickSourceSystem();
        if (src == null) return;

        IncomingTransaction txn = new IncomingTransaction(
                null, null, null,
                src.getId(), txnType, amount,
                LocalDateTime.now(), ProcessingStatus.RECEIVED, src, null);

        // Validate before processing
        String error = settlementService.validate(txn);
        if (error != null) {
            System.out.println("  ✗ Validation failed: " + error);
            return;
        }
        System.out.println("  ✓ Validation passed. Processing...");

        SettlementResult result = settlementService.process(txn);
        printSettlementResult(result);
    }

    // =========================================================================
    // 5. BATCH & SETTLEMENT PROCESSING
    // =========================================================================
    private void batchMenu() {
        int ch;
        do {
            System.out.println("\n  ╔═══ 5. BATCH & SETTLEMENT PROCESSING ══════╗");
            System.out.println("  ║  1. Read & Adapt from Source File        ║");
            System.out.println("  ║  2. Group Transactions (Date + Channel)  ║");
            System.out.println("  ║  3. Process Batch                        ║");
            System.out.println("  ║  4. Build Batch ID Manually              ║");
            System.out.println("  ║  5. Resolve Settlement Date for a TXN    ║");
            System.out.println("  ║  6. Validate an Incoming Transaction     ║");
            System.out.println("  ║  7. Export Settlement Result to File     ║");
            System.out.println("  ║  0. Back                                 ║");
            System.out.println("  ╚══════════════════════════════════════════╝");
            ch = readInt("Choice");
            switch (ch) {
                case 1 -> doReadAndAdapt();
                case 2 -> doGroupTransactions();
                case 3 -> doProcessBatch();
                case 4 -> doBuildBatchId();
                case 5 -> doResolveSettlementDate();
                case 6 -> doValidateTransaction();
                case 7 -> doExportToFile();
                case 0 -> {}
                default -> System.out.println("  ✗ Invalid.");
            }
        } while (ch != 0);
    }

    private void doReadAndAdapt() {
        SourceSystem src = pickSourceSystem();
        if (src == null) return;
        List<IncomingTransaction> txns = batchService.readAndAdapt(src);
        System.out.println("  ✓ Adapted " + txns.size() + " transaction(s) from "
                + src.getSystemCode() + " (" + src.getFilePath() + ")");
        if (!txns.isEmpty()) {
            System.out.println("  Preview (first transaction):");
            System.out.println("    Type   : " + txns.get(0).getTxnType());
            System.out.println("    Amount : ₹" + txns.get(0).getAmount());
            System.out.println("    Status : " + txns.get(0).getProcessingStatus());
        }
    }

    private void doGroupTransactions() {
        SourceSystem src = pickSourceSystem();
        if (src == null) return;
        List<IncomingTransaction> txns = batchService.readAndAdapt(src);
        if (txns.isEmpty()) { System.out.println("  No transactions adapted."); return; }

        Map<String, List<IncomingTransaction>> groups = batchService.groupByDateAndChannel(txns);
        System.out.printf("%n  Grouped into %d batch(es):%n", groups.size());
        System.out.printf("  %-40s %s%n", "Batch Key (Date_Channel)", "Count");
        sep(55);
        groups.forEach((k, v) -> System.out.printf("  %-40s %d%n", k, v.size()));
    }

    private void doProcessBatch() {
        SourceSystem src = pickSourceSystem();
        if (src == null) return;

        List<IncomingTransaction> txns = batchService.readAndAdapt(src);
        if (txns.isEmpty()) { System.out.println("  No transactions to process."); return; }

        Map<String, List<IncomingTransaction>> groups = batchService.groupByDateAndChannel(txns);
        if (groups.isEmpty()) { System.out.println("  No groups formed."); return; }

        List<String> keys = new ArrayList<>(groups.keySet());
        System.out.println("\n  Available Batches:");
        for (int i = 0; i < keys.size(); i++) {
            System.out.printf("  %2d.  %-40s (%d txns)%n",
                    i + 1, keys.get(i), groups.get(keys.get(i)).size());
        }
        System.out.println("  " + (keys.size() + 1) + ".  Process ALL batches");
        System.out.println("  0.   Cancel");

        int pick = readInt("Select batch");
        if (pick == 0) return;

        if (pick == keys.size() + 1) {
            System.out.println("\n  Processing all " + keys.size() + " batch(es)...");
            for (String key : keys) {
                System.out.println("\n  ── Batch: " + key + " ──────────────────────────────");
                SettlementResult r = batchService.processBatch(key, groups.get(key));
                printSettlementResult(r);
            }
        } else if (pick >= 1 && pick <= keys.size()) {
            String key = keys.get(pick - 1);
            SettlementResult r = batchService.processBatch(key, groups.get(key));
            printSettlementResult(r);
        } else {
            System.out.println("  ✗ Invalid selection.");
        }
    }

    private void doBuildBatchId() {
        LocalDate   date    = readDate("Settlement Date [YYYY-MM-DD]");
        ChannelType channel = pickEnum("Channel", ChannelType.values());
        int         seq     = readInt("Sequence Number (e.g. 1)");
        String      id      = batchService.buildBatchId(date, channel, seq);
        int         window  = WINDOW_MINUTES.getOrDefault(channel, 0);
        System.out.println("  ✓ Generated Batch ID    : " + id);
        System.out.println("    Settlement Window     : "
                + (window == 0 ? "Real-time (no windowing)" : window + " minutes"));
    }

    private void doResolveSettlementDate() {
        TransactionType txnType = pickEnum("Transaction Type", TransactionType.values());
        SourceSystem    src     = pickSourceSystem();
        if (src == null) return;
        BigDecimal amount = readDecimal("Amount (₹)");

        IncomingTransaction txn = new IncomingTransaction(
                null, null, null,
                src.getId(), txnType, amount,
                LocalDateTime.now(), ProcessingStatus.RECEIVED, src, null);

        ChannelType channel    = batchService.resolveChannel(txn);
        LocalDate   settlement = batchService.resolveSettlementDate(txn);
        System.out.println("  ✓ Resolved Channel         : " + channel);
        System.out.println("  ✓ Resolved Settlement Date : " + settlement);
        System.out.println("    Window (minutes)         : "
                + WINDOW_MINUTES.getOrDefault(channel, 0));
    }

    private void doValidateTransaction() {
        TransactionType txnType = pickEnum("Transaction Type", TransactionType.values());
        BigDecimal      amount  = readDecimal("Amount (₹)");
        SourceSystem    src     = pickSourceSystem();
        if (src == null) return;

        IncomingTransaction txn = new IncomingTransaction(
                null, null, null,
                src.getId(), txnType, amount,
                LocalDateTime.now(), ProcessingStatus.RECEIVED, src, null);

        String error = settlementService.validate(txn);
        if (error == null) {
            System.out.println("  ✓ Transaction is VALID — all rules passed.");
        } else {
            System.out.println("  ✗ Validation FAILED: " + error);
        }
    }

    private void doExportToFile() {
        System.out.println("\n  --- Export Settlement Result to File ---");
        SourceSystem src = pickSourceSystem();
        if (src == null) return;

        List<IncomingTransaction> txns = batchService.readAndAdapt(src);
        if (txns.isEmpty()) { System.out.println("  No transactions to export."); return; }

        Map<String, List<IncomingTransaction>> groups = batchService.groupByDateAndChannel(txns);
        String firstKey = groups.keySet().iterator().next();
        System.out.println("  Processing batch key: " + firstKey);

        SettlementResult result = batchService.processBatch(firstKey, groups.get(firstKey));
        String path = settlementService.exportToFile(result);
        System.out.println("  ✓ Exported to: " + path);
        printSettlementResult(result);
    }

    // =========================================================================
    // 6. SETTLEMENT RECORDS (DB)
    // =========================================================================
    private void settlementRecordsMenu() {
        int ch;
        do {
            System.out.println("\n  ╔═══ 6. SETTLEMENT RECORDS (DB) ════════════╗");
            System.out.println("  ║  1. Find Settlement Record by ID         ║");
            System.out.println("  ║  2. Find Records by Date                 ║");
            System.out.println("  ║  3. Find Records by Status               ║");
            System.out.println("  ║  4. Update Settlement Status             ║");
            System.out.println("  ║  0. Back                                 ║");
            System.out.println("  ╚══════════════════════════════════════════╝");
            ch = readInt("Choice");
            switch (ch) {
                case 1 -> doFindSettlementById();
                case 2 -> doFindSettlementByDate();
                case 3 -> doFindSettlementByStatus();
                case 4 -> doUpdateSettlementStatus();
                case 0 -> {}
                default -> System.out.println("  ✗ Invalid.");
            }
        } while (ch != 0);
    }

    private void doFindSettlementById() {
        long id = readLong("Settlement Record ID");
        try (Connection conn = getConn()) {
            SettlementResult r = settlementDao.findById(id, conn);
            if (r != null) printSettlementResult(r); else System.out.println("  ✗ Not found.");
        } catch (Exception e) { err(e); }
    }

    private void doFindSettlementByDate() {
        LocalDate date = readDate("Date [YYYY-MM-DD]");
        try (Connection conn = getConn()) {
            List<SettlementResult> list = settlementDao.findByDate(date, conn);
            if (list.isEmpty()) { System.out.println("  No records for " + date + "."); return; }
            list.forEach(this::printSettlementResult);
        } catch (Exception e) { err(e); }
    }

    private void doFindSettlementByStatus() {
        BatchStatus status = pickEnum("Status", BatchStatus.values());
        try (Connection conn = getConn()) {
            List<SettlementResult> list = settlementDao.findByStatus(status, conn);
            if (list.isEmpty()) { System.out.println("  No records with status " + status + "."); return; }
            list.forEach(this::printSettlementResult);
        } catch (Exception e) { err(e); }
    }

    private void doUpdateSettlementStatus() {
        long        id     = readLong("Settlement Record ID");
        BatchStatus status = pickEnum("New Status", BatchStatus.values());
        try (Connection conn = getConn()) {
            settlementDao.updateStatus(id, status, conn);
            System.out.println("  ✓ Status updated to " + status);
        } catch (Exception e) { err(e); }
    }

    // =========================================================================
    // 7. REPORTS
    // =========================================================================
    private void reportMenu() {
        int ch;
        do {
            System.out.println("\n  ╔═══ 7. REPORTS ════════════════════════════╗");
            System.out.println("  ║  1. Settlement Summary for Date          ║");
            System.out.println("  ║  2. Bank-wise Account Summary            ║");
            System.out.println("  ║  3. Customer Account Balance View        ║");
            System.out.println("  ║  4. Batch Netting Report                 ║");
            System.out.println("  ║  0. Back                                 ║");
            System.out.println("  ╚══════════════════════════════════════════╝");
            ch = readInt("Choice");
            switch (ch) {
                case 1 -> doSettlementSummaryForDate();
                case 2 -> doBankAccountSummary();
                case 3 -> doCustomerBalanceView();
                case 4 -> doBatchNettingReport();
                case 0 -> {}
                default -> System.out.println("  ✗ Invalid.");
            }
        } while (ch != 0);
    }

    private void doSettlementSummaryForDate() {
        LocalDate date = readDate("Date [YYYY-MM-DD]");
        try (Connection conn = getConn()) {
            List<SettlementResult> list = settlementDao.findByDate(date, conn);
            System.out.println("\n  ── Settlement Summary: " + date + " ──────────────────────────");
            if (list.isEmpty()) { System.out.println("  No settlements on this date."); return; }

            System.out.printf("  %-30s %-12s %8s %8s %15s%n",
                    "BatchId", "Status", "Settled", "Failed", "Net Amount (₹)");
            sep(78);

            BigDecimal grandNet  = BigDecimal.ZERO;
            int        grandTxns = 0;
            for (SettlementResult r : list) {
                System.out.printf("  %-30s %-12s %8d %8d %15s%n",
                        r.getBatchId(), r.getBatchStatus(),
                        r.getSettledCount(), r.getFailedCount(),
                        r.getNetAmount() != null ? r.getNetAmount().toPlainString() : "—");
                if (r.getNetAmount() != null) grandNet = grandNet.add(r.getNetAmount());
                grandTxns += r.getTotalTransactions();
            }
            sep(78);
            System.out.printf("  Batches: %d  |  Total TXNs: %d  |  Grand Net: ₹%s%n",
                    list.size(), grandTxns, grandNet.toPlainString());
        } catch (Exception e) { err(e); }
    }

    private void doBankAccountSummary() {
        long bankId = readLong("Bank ID");
        try (Connection conn = getConn()) {
            Bank b = bankDao.findById(bankId, conn);
            if (b == null) { System.out.println("  ✗ Bank not found."); return; }
            System.out.println("\n  Bank: " + b.getBankName()
                    + " (" + b.getBankCode() + ")  |  IFSC: " + b.getIfscCode());
            sep(60);
            for (AccountType type : AccountType.values()) {
                List<Account> accs = accountDao.findByBankIdAndType(bankId, type, conn);
                if (accs.isEmpty()) continue;
                BigDecimal total = accs.stream()
                        .map(Account::getBalance)
                        .filter(Objects::nonNull)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                System.out.printf("  %-15s : %3d account(s)  |  Total Balance: ₹%s%n",
                        type, accs.size(), total.toPlainString());
            }
        } catch (Exception e) { err(e); }
    }

    private void doCustomerBalanceView() {
        long customerId = readLong("Customer ID");
        try (Connection conn = getConn()) {
            Customer c = customerDao.findById(customerId, conn);
            if (c == null) { System.out.println("  ✗ Customer not found."); return; }
            System.out.println("\n  Customer  : " + c.getFirstName() + " " + c.getLastName());
            System.out.println("  Email     : " + c.getEmail());
            System.out.println("  Onboarded : " + c.getOnboardingDate());
            sep(70);
            List<Account> accs = accountDao.findByCustomerId(customerId, conn);
            if (accs.isEmpty()) { System.out.println("  No accounts found."); return; }
            System.out.printf("  %-6s %-20s %-14s %-10s %-14s%n",
                    "ID", "Account No", "Type", "Status", "Balance (₹)");
            sep(70);
            BigDecimal netWorth = BigDecimal.ZERO;
            for (Account a : accs) {
                System.out.printf("  %-6d %-20s %-14s %-10s %-14s%n",
                        a.getId(), a.getAccountNumber(), a.getAccountType(),
                        a.getStatus(),
                        a.getBalance() != null ? a.getBalance().toPlainString() : "—");
                if (a.getBalance() != null) netWorth = netWorth.add(a.getBalance());
            }
            sep(70);
            System.out.println("  Net Worth Across All Accounts : ₹" + netWorth.toPlainString());
        } catch (Exception e) { err(e); }
    }

    private void doBatchNettingReport() {
        System.out.println("\n  --- Batch Netting Report ---");
        SourceSystem src = pickSourceSystem();
        if (src == null) return;

        List<IncomingTransaction> txns = batchService.readAndAdapt(src);
        if (txns.isEmpty()) { System.out.println("  No transactions."); return; }

        Map<String, List<IncomingTransaction>> groups = batchService.groupByDateAndChannel(txns);
        System.out.printf("%n  %-40s %8s %8s %16s%n",
                "Batch Key", "TXNs", "Settled", "Net Amount (₹)");
        sep(76);

        BigDecimal grandNet = BigDecimal.ZERO;
        for (Map.Entry<String, List<IncomingTransaction>> e : groups.entrySet()) {
            SettlementResult r = batchService.processBatch(e.getKey(), e.getValue());
            BigDecimal net = r.getNetAmount() != null ? r.getNetAmount() : BigDecimal.ZERO;
            grandNet = grandNet.add(net);
            System.out.printf("  %-40s %8d %8d %16s%n",
                    e.getKey(), r.getTotalTransactions(),
                    r.getSettledCount(), net.toPlainString());
        }
        sep(76);
        System.out.printf("  %-40s %8s %8s %16s%n",
                "GRAND TOTAL", "", "", grandNet.toPlainString());
    }

    // =========================================================================
    // 8. FULL PIPELINE (Orchestrator)
    // =========================================================================
    private void runFullPipeline() {
        System.out.println("\n  ── FULL PIPELINE (NPCI-style Parallel) ───────────────");
        System.out.println("  Active Sources:");
        allSources.forEach(s -> System.out.println("    • "
                + s.getSystemCode() + " → " + s.getFilePath()));
        System.out.println();
        System.out.println("  Each source gets its own producer thread.");
        System.out.println("  BatchScheduler fires every 30 seconds.");
        System.out.println("  ⚠  Runs until all producers complete (up to 5 min).");
        if (!readYesNo("Start pipeline? [y/n]")) return;

        PipelineOrchestrator orchestrator =
                new PipelineOrchestrator(batchService, settlementService);
        orchestrator.startPipeline(allSources);
        System.out.println("  ✓ Pipeline completed.");
    }

    // =========================================================================
    // SHARED HELPER — Source System picker
    // =========================================================================
    private SourceSystem pickSourceSystem() {
        System.out.println("\n  Select Source System:");
        System.out.println("  1. CBS     (" + cbsSrc.getFilePath()     + ")");
        System.out.println("  2. RTGS    (" + rtgsSrc.getFilePath()    + ")");
        System.out.println("  3. SWIFT   (" + swiftSrc.getFilePath()   + ")");
        System.out.println("  4. NEFT    (" + neftSrc.getFilePath()    + ")");
        System.out.println("  5. FINTECH (" + fintechSrc.getFilePath() + ")");
        int ch = readInt("Choice");
        return switch (ch) {
            case 1 -> cbsSrc;
            case 2 -> rtgsSrc;
            case 3 -> swiftSrc;
            case 4 -> neftSrc;
            case 5 -> fintechSrc;
            default -> { System.out.println("  ✗ Invalid source."); yield null; }
        };
    }

    // =========================================================================
    // PRINT HELPERS
    // =========================================================================
    private void printBank(Bank b) {
        System.out.printf("  Bank     → ID:%-4d  Code:%-10s  Name:%-25s  IFSC:%-12s  Active:%b%n",
                b.getId(), b.getBankCode(), b.getBankName(), b.getIfscCode(), b.isActive());
    }

    private void printCustomer(Customer c) {
        System.out.printf("  Customer → ID:%-4d  Name:%-25s  Email:%-30s  Onboarded:%s%n",
                c.getId(), c.getFirstName() + " " + c.getLastName(),
                c.getEmail(), c.getOnboardingDate());
    }

    private void printAccount(Account a) {
        System.out.printf("  Account  → ID:%-4d  No:%-20s  Type:%-14s  Status:%-10s  Bal:₹%s%n",
                a.getId(), a.getAccountNumber(), a.getAccountType(),
                a.getStatus(), a.getBalance() != null ? a.getBalance().toPlainString() : "—");
    }

    private void printTransaction(Transaction t) {
        System.out.printf(
            "  TXN → ID:%-4d  Type:%-22s  Channel:%-10s  Amount:₹%-14s  Status:%-20s  Batch:%s%n",
            t.getId(), t.getClass().getSimpleName(), t.getChannel(),
            t.getAmount() != null ? t.getAmount().toPlainString() : "—",
            t.getStatus(), t.getSettlementBatchId());
    }

    private void printTransactionTotals(List<Transaction> txns) {
        BigDecimal cr = BigDecimal.ZERO, dr = BigDecimal.ZERO;
        for (Transaction t : txns) {
            if (t == null || t.getAmount() == null) continue;
            if      (t instanceof CreditTransaction) cr = cr.add(t.getAmount());
            else if (t instanceof DebitTransaction)  dr = dr.add(t.getAmount());
        }
        sep(55);
        System.out.println("  Total CREDIT  : ₹" + cr.toPlainString());
        System.out.println("  Total DEBIT   : ₹" + dr.toPlainString());
        System.out.println("  Net Position  : ₹" + cr.subtract(dr).toPlainString());
    }

    private void printSettlementResult(SettlementResult r) {
        System.out.println();
        System.out.println("  ┌─────────────────────────────────────────────────────┐");
        System.out.println("  │               SETTLEMENT RESULT                     │");
        System.out.println("  ├─────────────────────────────────────────────────────┤");
        System.out.printf ("  │  Batch ID      : %-34s│%n", r.getBatchId());
        System.out.printf ("  │  Batch Date    : %-34s│%n", r.getBatchDate());
        System.out.printf ("  │  Status        : %-34s│%n", r.getBatchStatus());
        System.out.printf ("  │  Total TXNs    : %-34d│%n", r.getTotalTransactions());
        System.out.printf ("  │  Settled       : %-34d│%n", r.getSettledCount());
        System.out.printf ("  │  Failed        : %-34d│%n", r.getFailedCount());
        System.out.printf ("  │  Gross Amount  : ₹%-33s│%n",
                r.getTotalAmount()   != null ? r.getTotalAmount().toPlainString()   : "—");
        System.out.printf ("  │  Settled Amt   : ₹%-33s│%n",
                r.getSettledAmount() != null ? r.getSettledAmount().toPlainString() : "—");
        System.out.printf ("  │  Net Amount    : ₹%-33s│%n",
                r.getNetAmount()     != null ? r.getNetAmount().toPlainString()     : "—");
        System.out.printf ("  │  File          : %-34s│%n",
                r.getExportedFilePath() != null ? r.getExportedFilePath() : "—");
        System.out.printf ("  │  Processed At  : %-34s│%n",
                r.getProcessedAt() != null
                        ? r.getProcessedAt().format(
                            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                        : "—");
        System.out.println("  └─────────────────────────────────────────────────────┘");
    }

    // =========================================================================
    // CONSOLE INPUT HELPERS
    // =========================================================================
    private int readInt(String prompt) {
        System.out.print("  " + prompt + ": ");
        try { return Integer.parseInt(sc.nextLine().trim()); }
        catch (NumberFormatException e) { return -99; }
    }

    private long readLong(String prompt) {
        System.out.print("  " + prompt + ": ");
        try { return Long.parseLong(sc.nextLine().trim()); }
        catch (NumberFormatException e) { return -1L; }
    }

    private String readString(String prompt) {
        System.out.print("  " + prompt + ": ");
        return sc.nextLine().trim();
    }

    private BigDecimal readDecimal(String prompt) {
        System.out.print("  " + prompt + ": ");
        try { return new BigDecimal(sc.nextLine().trim()); }
        catch (Exception e) {
            System.out.println("  ✗ Invalid number — defaulting to 0.");
            return BigDecimal.ZERO;
        }
    }

    private boolean readYesNo(String prompt) {
        System.out.print("  " + prompt + ": ");
        return sc.nextLine().trim().equalsIgnoreCase("y");
    }

    private LocalDate readDate(String prompt) {
        System.out.print("  " + prompt + ": ");
        try { return LocalDate.parse(sc.nextLine().trim()); }
        catch (Exception e) {
            System.out.println("  ✗ Invalid date — using today.");
            return LocalDate.now();
        }
    }

    /**
     * Generic enum picker — prints a numbered list, returns selected constant.
     * Falls back to values[0] on invalid input.
     */
    private <E extends Enum<E>> E pickEnum(String prompt, E[] values) {
        System.out.println("  " + prompt + ":");
        for (int i = 0; i < values.length; i++) {
            System.out.println("    " + (i + 1) + ". " + values[i]);
        }
        int pick = readInt("Select") - 1;
        if (pick >= 0 && pick < values.length) return values[pick];
        System.out.println("  ✗ Invalid — defaulting to " + values[0]);
        return values[0];
    }

    // =========================================================================
    // UTILITY HELPERS
    // =========================================================================
    private Connection getConn() throws Exception {
        return ConnectionPool.getDataSource().getConnection();
    }

    private void err(Exception e) {
        System.out.println("  ✗ Error: " + e.getMessage());
    }

    private void sep(int width) {
        System.out.println("  " + "─".repeat(width));
    }

    // =========================================================================
    // BANNER & MAIN MENU
    // =========================================================================
    private void printBanner() {
        System.out.println();
        System.out.println("  ╔═══════════════════════════════════════════════════════╗");
        System.out.println("  ║      IISPL — Inter-Bank Settlement Platform           ║");
        System.out.println("  ║      NPCI-style Batch Processing Engine  v2.0         ║");
        System.out.println("  ║      Channels : RTGS | NEFT | UPI | SWIFT | ACH       ║");
        System.out.println("  ║      Sources  : CBS | RTGS | SWIFT | NEFT | FINTECH   ║");
        System.out.println("  ╚═══════════════════════════════════════════════════════╝");
        System.out.println();
    }

    private void printMainMenu() {
        System.out.println();
        System.out.println("  ╔═══════════════════════════════════════════════════════╗");
        System.out.println("  ║                    MAIN MENU                         ║");
        System.out.println("  ╠═══════════════════════════════════════════════════════╣");
        System.out.println("  ║   1.  Bank Management                                ║");
        System.out.println("  ║   2.  Customer Management                            ║");
        System.out.println("  ║   3.  Account Management                             ║");
        System.out.println("  ║   4.  Transaction Management                         ║");
        System.out.println("  ║   5.  Batch & Settlement Processing                  ║");
        System.out.println("  ║   6.  Settlement Records  (DB)                       ║");
        System.out.println("  ║   7.  Reports                                        ║");
        System.out.println("  ║   8.  Full Pipeline  (All Sources — Parallel)        ║");
        System.out.println("  ║   0.  Exit                                           ║");
        System.out.println("  ╚═══════════════════════════════════════════════════════╝");
    }
}