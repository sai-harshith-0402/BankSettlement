package com.iispl.utility;

import com.iispl.connectionpool.ConnectionPool;
import com.iispl.dao.*;
import com.iispl.entity.*;
import com.iispl.enums.*;
import com.iispl.exception.AdapterException;
import com.iispl.ingestion.AdapterRegistry;
import com.iispl.service.*;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class BankSettlementUtility {

	// =========================================================================
	// SHARED SCANNER
	// =========================================================================

	private static final Scanner scanner = new Scanner(System.in);

	// =========================================================================
	// DAOs — wired once at startup
	// =========================================================================

	private static final BankDao bankDao;
	private static final CustomerDao customerDao;
	private static final AccountDao accountDao;
	private static final TransactionDao transactionDao;

	private static final NpciBanksDao npciBanksDao;
	private static final SourceSystemDao sourceSystemDao;
	private static final BatchDao batchDao;
	private static final SettlementDao settlementDao;
	private static final NettingPositionDao nettingPositionDao;
	private static final ReconciliationEntryDao reconciliationEntryDao;

	// =========================================================================
	// SERVICES
	// =========================================================================

	private static final BatchService batchService;
	private static final SettlementService settlementService;
	private static final NettingService nettingService;
	private static final ReconciliationService reconciliationService;
	private static final NPCIService npciService;

	// =========================================================================
	// STATIC INIT
	// =========================================================================

	static {
		try {
			// A single dedicated connection for DAOs that hold it (BankDaoImpl,
			// AccountDaoImpl, SourceSystemDaoImpl). All other DAOs fetch their own
			// connections per call from the pool.
			Connection sharedConn = ConnectionPool.getDataSource().getConnection();

			bankDao = new BankDaoImpl(sharedConn);
			accountDao = new AccountDaoImpl(sharedConn);
			sourceSystemDao = new SourceSystemDaoImpl(sharedConn);

			customerDao = new CustomerDaoImpl();
			transactionDao = new TransactionDaoImpl();
			npciBanksDao = new NpciBanksDaoImpl();
			batchDao = new BatchDaoImpl();
			settlementDao = new SettlementDaoImpl();
			nettingPositionDao = new NettingPositionDaoImpl();
			reconciliationEntryDao = new ReconciliationEntryDaoImpl();

			List<NPCIBank> bankList = npciBanksDao.findAllNPCIBanks();
			NPCI npci = new NPCI(bankList);
			AdapterRegistry adapterRegistry = new AdapterRegistry();

			npciService = new NPCIServiceImpl(npci, npciBanksDao);
			batchService = new BatchServiceImpl(adapterRegistry);
			settlementService = new SettlementServiceImpl(npciService);
			nettingService = new NettingServiceImpl(npciService);
			reconciliationService = new ReconciliationServiceImpl(npciService);

		} catch (SQLException e) {
			throw new RuntimeException("[FATAL] DB initialisation failed: " + e.getMessage(), e);
		}
	}

	// =========================================================================
	// MAIN
	// =========================================================================

	public static void main(String[] args) {
		printBanner();
		boolean running = true;
		while (running) {
			printMainMenu();
			int choice = readInt("Enter section: ");
			switch (choice) {
			case 1:
				runCrudSection();
				break;
			case 2:
				runOperationsSection();
				break;
			case 3:
				System.out.println("\nExiting Bank Settlement Utility. Goodbye!");
				running = false;
				break;
			default:
				System.out.println("[ERROR] Please enter 1, 2 or 3.");
			}
		}
		scanner.close();
	}

	// ─────────────────────────────────────────────────────────────────────────
	private static void printBanner() {
		System.out.println("╔═══════════════════════════════════════════════════════════╗");
		System.out.println("║         BANK SETTLEMENT SYSTEM  -  UTILITY CONSOLE       ║");
		System.out.println("╚═══════════════════════════════════════════════════════════╝");
	}

	private static void printMainMenu() {
		System.out.println("\n+-----------------------------------------------------------+");
		System.out.println("|                       MAIN MENU                          |");
		System.out.println("+-----------------------------------------------------------+");
		System.out.println("|  1.  CRUD Operations   (Bank / Customer / Account / Txn) |");
		System.out.println("|  2.  System Operations (Batch / Settlement / Netting /   |");
		System.out.println("|                         Reconciliation)                  |");
		System.out.println("|  3.  Exit                                                |");
		System.out.println("+-----------------------------------------------------------+");
	}

	// =========================================================================
	// ==========================================================================
	// SECTION 1 — MANUAL CRUD
	// ==========================================================================
	// =========================================================================

	private static void runCrudSection() {
		boolean back = false;
		while (!back) {
			printCrudMenu();
			int choice = readInt("Enter choice: ");
			switch (choice) {
			case 1:
				crudBankMenu();
				break;
			case 2:
				crudCustomerMenu();
				break;
			case 3:
				crudAccountMenu();
				break;
			case 4:
				crudTransactionMenu();
				break;
			case 5:
				back = true;
				break;
			default:
				System.out.println("[ERROR] Invalid choice.");
			}
		}
	}

	private static void printCrudMenu() {
		System.out.println("\n+-----------------------------------------------------------+");
		System.out.println("|             SECTION 1 — CRUD OPERATIONS                  |");
		System.out.println("+-----------------------------------------------------------+");
		System.out.println("|  1.  Bank Management                                     |");
		System.out.println("|  2.  Customer Management                                 |");
		System.out.println("|  3.  Account Management                                  |");
		System.out.println("|  4.  Transaction Management                              |");
		System.out.println("|  5.  Back to Main Menu                                   |");
		System.out.println("+-----------------------------------------------------------+");
	}

	// =========================================================================
	// 1-A BANK
	// =========================================================================

	private static void crudBankMenu() {
		boolean back = false;
		while (!back) {
			System.out.println("\n--- Bank Management ---");
			System.out.println("  1. Add Bank");
			System.out.println("  2. View All Banks");
			System.out.println("  3. View Bank by ID");
			System.out.println("  4. Update Bank");
			System.out.println("  5. Delete Bank");
			System.out.println("  6. Change Bank Status (activate / deactivate)");
			System.out.println("  7. Back");
			int c = readInt("Choice: ");
			switch (c) {
			case 1:
				addBank();
				break;
			case 2:
				viewAllBanks();
				break;
			case 3:
				viewBankById();
				break;
			case 4:
				updateBank();
				break;
			case 5:
				deleteBank();
				break;
			case 6:
				changeBankStatus();
				break;
			case 7:
				back = true;
				break;
			default:
				System.out.println("[ERROR] Invalid choice.");
			}
		}
	}

	private static void addBank() {
		System.out.println("\n>> Add Bank");
		String code = readString("  Bank Code  : ");
		String name = readString("  Bank Name  : ");
		String ifsc = readString("  IFSC Code  : ");
		boolean active = readBoolean("  Active? (y/n): ");

		Bank bank = new Bank(0, LocalDateTime.now(), LocalDateTime.now(), code, name, ifsc, active);
		bankDao.saveBank(bank);
		System.out.println("[SUCCESS] Bank saved. ID: " + bank.getId());
	}

	private static void viewAllBanks() {
		System.out.println("\n>> All Banks");
		List<Bank> banks = bankDao.findAllBanks();
		if (banks.isEmpty()) {
			System.out.println("[INFO] No banks found.");
			return;
		}
		System.out.println("[INFO] Total: " + banks.size());
		banks.forEach(BankSettlementUtility::printBank);
	}

	private static void viewBankById() {
		long id = readLong("  Bank ID: ");
		Bank bank = bankDao.findBankById(id);
		if (bank == null) {
			System.out.println("[WARN] Bank not found for ID " + id);
			return;
		}
		printBank(bank);
	}

	private static void updateBank() {
		long id = readLong("  Bank ID to update: ");
		Bank existing = bankDao.findBankById(id);
		if (existing == null) {
			System.out.println("[WARN] Bank not found.");
			return;
		}
		printBank(existing);
		String code = readStringWithDefault("  New Bank Code  [" + existing.getBankCode() + "]: ",
				existing.getBankCode());
		String name = readStringWithDefault("  New Bank Name  [" + existing.getBankName() + "]: ",
				existing.getBankName());
		String ifsc = readStringWithDefault("  New IFSC Code  [" + existing.getIfscCode() + "]: ",
				existing.getIfscCode());
		boolean active = readBoolean("  Active? (y/n): ");
		Bank updated = new Bank(id, existing.getCreatedAt(), LocalDateTime.now(), code, name, ifsc, active);
		bankDao.updateBank(updated);
		System.out.println("[SUCCESS] Bank updated.");
	}

	private static void deleteBank() {
		long id = readLong("  Bank ID to delete: ");
		bankDao.deleteBank(id);
		System.out.println("[SUCCESS] Bank deleted.");
	}

	private static void changeBankStatus() {
		long id = readLong("  Bank ID: ");
		boolean active = readBoolean("  Set Active? (y/n): ");
		bankDao.changeStatus(id, active);
		System.out.println("[SUCCESS] Bank status updated.");
	}

	private static void printBank(Bank b) {
		System.out.println("  +- Bank --------------------------------------------------");
		System.out.println("  | ID      : " + b.getId());
		System.out.println("  | Code    : " + b.getBankCode());
		System.out.println("  | Name    : " + b.getBankName());
		System.out.println("  | IFSC    : " + b.getIfscCode());
		System.out.println("  | Active  : " + b.isActive());
		System.out.println("  | Created : " + b.getCreatedAt());
		System.out.println("  +---------------------------------------------------------");
	}

	// =========================================================================
	// 1-B CUSTOMER
	// =========================================================================

	private static void crudCustomerMenu() {
		boolean back = false;
		while (!back) {
			System.out.println("\n--- Customer Management ---");
			System.out.println("  1. Add Customer");
			System.out.println("  2. View All Customers");
			System.out.println("  3. View Customer by ID");
			System.out.println("  4. View Customer by Email");
			System.out.println("  5. Update Customer");
			System.out.println("  6. Delete Customer");
			System.out.println("  7. Back");
			int c = readInt("Choice: ");
			switch (c) {
			case 1:
				addCustomer();
				break;
			case 2:
				viewAllCustomers();
				break;
			case 3:
				viewCustomerById();
				break;
			case 4:
				viewCustomerByEmail();
				break;
			case 5:
				updateCustomer();
				break;
			case 6:
				deleteCustomer();
				break;
			case 7:
				back = true;
				break;
			default:
				System.out.println("[ERROR] Invalid choice.");
			}
		}
	}

	private static void addCustomer() {
		System.out.println("\n>> Add Customer");
		String firstName = readString("  First Name  : ");
		String lastName = readString("  Last Name   : ");
		String email = readString("  Email       : ");
		String bankId = readString("  Bank ID     : ");
		boolean active = readBoolean("  Active? (y/n): ");

		Customer customer = new Customer(0, null, null, firstName, lastName, email, LocalDateTime.now(), bankId, active,
				new ArrayList<>());
		customerDao.save(customer);
		System.out.println("[SUCCESS] Customer saved. ID: " + customer.getId());
	}

	private static void viewAllCustomers() {
		System.out.println("\n>> All Customers");
		List<Customer> list = customerDao.findAll();
		if (list.isEmpty()) {
			System.out.println("[INFO] No customers found.");
			return;
		}
		System.out.println("[INFO] Total: " + list.size());
		list.forEach(BankSettlementUtility::printCustomer);
	}

	private static void viewCustomerById() {
		long id = readLong("  Customer ID: ");
		Customer c = customerDao.findById(id);
		if (c == null) {
			System.out.println("[WARN] Customer not found.");
			return;
		}
		printCustomer(c);
	}

	private static void viewCustomerByEmail() {
		String email = readString("  Email: ");
		Customer c = customerDao.findByEmail(email);
		if (c == null) {
			System.out.println("[WARN] Customer not found.");
			return;
		}
		printCustomer(c);
	}

	private static void updateCustomer() {
		long id = readLong("  Customer ID to update: ");
		Customer existing = customerDao.findById(id);
		if (existing == null) {
			System.out.println("[WARN] Customer not found.");
			return;
		}
		printCustomer(existing);
		String email = readStringWithDefault("  New Email   [" + existing.getEmailId() + "]: ", existing.getEmailId());
		boolean active = readBoolean("  Active? (y/n): ");
		existing.setEmailId(email);
		existing.setActive(active);
		customerDao.update(existing);
		System.out.println("[SUCCESS] Customer updated.");
	}

	private static void deleteCustomer() {
		long id = readLong("  Customer ID to delete: ");
		customerDao.delete(id);
		System.out.println("[SUCCESS] Customer deleted.");
	}

	private static void printCustomer(Customer c) {
		System.out.println("  +- Customer ----------------------------------------------");
		System.out.println("  | ID        : " + c.getId());
		System.out.println("  | Name      : " + c.getFirstName() + " " + c.getLastName());
		System.out.println("  | Email     : " + c.getEmailId());
		System.out.println("  | Bank ID   : " + c.getBankId());
		System.out.println("  | Active    : " + c.isActive());
		System.out.println("  | Onboarded : " + c.getOnBoardingDate());
		System.out.println("  +---------------------------------------------------------");
	}

	// =========================================================================
	// 1-C ACCOUNT
	// =========================================================================

	private static void crudAccountMenu() {
		boolean back = false;
		while (!back) {
			System.out.println("\n--- Account Management ---");
			System.out.println("  1.  Add Account");
			System.out.println("  2.  View All Accounts");
			System.out.println("  3.  View Account by ID");
			System.out.println("  4.  View Account by Account Number");
			System.out.println("  5.  View Accounts by Customer ID");
			System.out.println("  6.  View Accounts by Bank ID");
			System.out.println("  7.  Update Account");
			System.out.println("  8.  Update Balance");
			System.out.println("  9.  Update Status");
			System.out.println(" 10.  Delete Account");
			System.out.println(" 11.  Back");
			int c = readInt("Choice: ");
			switch (c) {
			case 1:
				addAccount();
				break;
			case 2:
				viewAllAccounts();
				break;
			case 3:
				viewAccountById();
				break;
			case 4:
				viewAccountByNumber();
				break;
			case 5:
				viewAccountsByCustomerId();
				break;
			case 6:
				viewAccountsByBankId();
				break;
			case 7:
				updateAccount();
				break;
			case 8:
				updateAccountBalance();
				break;
			case 9:
				updateAccountStatus();
				break;
			case 10:
				deleteAccount();
				break;
			case 11:
				back = true;
				break;
			default:
				System.out.println("[ERROR] Invalid choice.");
			}
		}
	}

	private static void addAccount() {
		System.out.println("\n>> Add Account");
		String accNum = readString("  Account Number : ");
		long custId = readLong("  Customer ID    : ");
		long bankId = readLong("  Bank ID        : ");

		AccountType type = readAccountType();
		BigDecimal bal = readBigDecimal("  Opening Balance: ");
		String status = readString("  Status (ACTIVE / INACTIVE / BLOCKED): ");

		Account acc = new Account(0, LocalDateTime.now(), LocalDateTime.now(), custId, accNum, type, bankId, bal,
				status);
		accountDao.saveAccount(acc);
		System.out.println("[SUCCESS] Account saved. ID: " + acc.getId());
	}

	private static void viewAllAccounts() {
		System.out.println("\n>> All Accounts");
		List<Account> list = accountDao.findAllAccounts();
		if (list.isEmpty()) {
			System.out.println("[INFO] No accounts found.");
			return;
		}
		System.out.println("[INFO] Total: " + list.size());
		list.forEach(BankSettlementUtility::printAccount);
	}

	private static void viewAccountById() {
		long id = readLong("  Account ID: ");
		Account a = accountDao.findAccountById(id);
		if (a == null) {
			System.out.println("[WARN] Account not found.");
			return;
		}
		printAccount(a);
	}

	private static void viewAccountByNumber() {
		String num = readString("  Account Number: ");
		Account a = accountDao.findAccountByAccountNumber(num);
		if (a == null) {
			System.out.println("[WARN] Account not found.");
			return;
		}
		printAccount(a);
	}

	private static void viewAccountsByCustomerId() {
		long custId = readLong("  Customer ID: ");
		List<Account> list = accountDao.findAccountByCustomerId(custId);
		if (list.isEmpty()) {
			System.out.println("[INFO] No accounts for this customer.");
			return;
		}
		list.forEach(BankSettlementUtility::printAccount);
	}

	private static void viewAccountsByBankId() {
		long bankId = readLong("  Bank ID: ");
		List<Account> list = accountDao.findAccountsByBankId(bankId);
		if (list.isEmpty()) {
			System.out.println("[INFO] No accounts for this bank.");
			return;
		}
		list.forEach(BankSettlementUtility::printAccount);
	}

	private static void updateAccount() {
		long id = readLong("  Account ID to update: ");
		Account existing = accountDao.findAccountById(id);
		if (existing == null) {
			System.out.println("[WARN] Account not found.");
			return;
		}
		printAccount(existing);
		AccountType type = readAccountType();
		BigDecimal bal = readBigDecimal("  New Balance [" + existing.getBalance() + "]: ");
		String status = readStringWithDefault("  New Status [" + existing.getStatus() + "]: ", existing.getStatus());
		existing.setAccountType(type);
		existing.setBalance(bal);
		existing.setStatus(status);
		accountDao.updateAccount(existing);
		System.out.println("[SUCCESS] Account updated.");
	}

	private static void updateAccountBalance() {
		long id = readLong("  Account ID: ");
		BigDecimal bal = readBigDecimal("  New Balance: ");
		accountDao.updateBalance(id, bal);
		System.out.println("[SUCCESS] Balance updated.");
	}

	private static void updateAccountStatus() {
		long id = readLong("  Account ID: ");
		String status = readString("  New Status (ACTIVE / INACTIVE / BLOCKED): ");
		accountDao.updateStatus(id, status);
		System.out.println("[SUCCESS] Status updated.");
	}

	private static void deleteAccount() {
		long id = readLong("  Account ID to delete: ");
		accountDao.deleteAccount(id);
		System.out.println("[SUCCESS] Account deleted.");
	}

	private static void printAccount(Account a) {
		System.out.println("  +- Account -----------------------------------------------");
		System.out.println("  | ID         : " + a.getId());
		System.out.println("  | Acc Number : " + a.getAccountNumber());
		System.out.println("  | Type       : " + a.getAccountType());
		System.out.println("  | Customer ID: " + a.getCustomerId());
		System.out.println("  | Bank ID    : " + a.getBankId());
		System.out.println("  | Balance    : " + a.getBalance());
		System.out.println("  | Status     : " + a.getStatus());
		System.out.println("  +---------------------------------------------------------");
	}

	// =========================================================================
	// 1-D TRANSACTION
	// =========================================================================

	private static void crudTransactionMenu() {
		boolean back = false;
		while (!back) {
			System.out.println("\n--- Transaction Management ---");
			System.out.println("  1. View All Transactions");
			System.out.println("  2. View Transaction by ID");
			System.out.println("  3. View by Source System ID");
			System.out.println("  4. View by Processing Status");
			System.out.println("  5. Update Batch ID on Transaction");
			System.out.println("  6. Back");
			int c = readInt("Choice: ");
			switch (c) {
			case 1:
				viewAllTransactions();
				break;
			case 2:
				viewTransactionById();
				break;
			case 3:
				viewTransactionsBySourceSystem();
				break;
			case 4:
				viewTransactionsByStatus();
				break;
			case 5:
				updateTransactionBatchId();
				break;
			case 6:
				back = true;
				break;
			default:
				System.out.println("[ERROR] Invalid choice.");
			}
		}
	}

	private static void viewAllTransactions() {
		System.out.println("\n>> All Incoming Transactions");
		List<IncomingTransaction> list = transactionDao.findAll();
		if (list.isEmpty()) {
			System.out.println("[INFO] No transactions found.");
			return;
		}
		System.out.println("[INFO] Total: " + list.size());
		list.forEach(BankSettlementUtility::printTransaction);
	}

	private static void viewTransactionById() {
		long id = readLong("  Transaction ID: ");
		IncomingTransaction txn = transactionDao.findById(id);
		if (txn == null) {
			System.out.println("[WARN] Transaction not found.");
			return;
		}
		printTransaction(txn);
	}

	private static void viewTransactionsBySourceSystem() {
		long ssId = readLong("  Source System ID: ");
		List<IncomingTransaction> list = transactionDao.findBySourceSystemId(ssId);
		if (list.isEmpty()) {
			System.out.println("[INFO] No transactions found.");
			return;
		}
		list.forEach(BankSettlementUtility::printTransaction);
	}

	private static void viewTransactionsByStatus() {
		System.out.println("  Statuses: " + Arrays.toString(ProcessingStatus.values()));
		String input = readString("  Enter Status: ");
		ProcessingStatus ps;
		try {
			ps = ProcessingStatus.valueOf(input.toUpperCase());
		} catch (IllegalArgumentException e) {
			System.out.println("[ERROR] Invalid status.");
			return;
		}
		List<IncomingTransaction> list = transactionDao.findByProcessingStatus(ps);
		if (list.isEmpty()) {
			System.out.println("[INFO] No transactions with status " + ps);
			return;
		}
		list.forEach(BankSettlementUtility::printTransaction);
	}

	private static void updateTransactionBatchId() {
		long id = readLong("  Transaction ID : ");
		String batchId = readString("  New Batch ID   : ");
		transactionDao.updateBatchId(id, batchId);
		System.out.println("[SUCCESS] Batch ID updated.");
	}

	private static void printTransaction(IncomingTransaction t) {
		System.out.println("  +- Transaction -------------------------------------------");
		System.out.println("  | ID         : " + t.getIncomingTnxId());
		System.out.println("  | Type       : " + t.getTransactionType());
		System.out.println("  | Channel    : " + t.getChannelType());
		System.out.println("  | From Bank  : " + t.getFromBankName());
		System.out.println("  | To Bank    : " + t.getToBankName());
		System.out.println("  | Amount     : " + t.getAmount());
		System.out.println("  | Status     : " + t.getProcessingStatus());
		System.out.println("  | Batch ID   : " + t.getBatchId());
		System.out.println("  | Ingested At: " + t.getIngestionTimeStamp());
		System.out.println("  +---------------------------------------------------------");
	}

	// =========================================================================
	// ==========================================================================
	// SECTION 2 — SYSTEM OPERATIONS (all data from DB, no user input)
	// ==========================================================================
	// =========================================================================

	private static void runOperationsSection() {
		boolean back = false;
		while (!back) {
			printOpsMenu();
			int choice = readInt("Enter choice: ");
			switch (choice) {
			case 1:
				opsCreateBatch();
				break;
			case 2:
				opsViewAllBatches();
				break;
			case 3:
				opsViewBatchesByStatus();
				break;
			case 4:
				opsCreateSettlement();
				break;
			case 5:
				opsViewAllSettlements();
				break;
			case 6:
				opsRunNetting();
				break;
			case 7:
				opsViewAllNetting();
				break;
			case 8:
				opsRunReconciliation();
				break;
			case 9:
				opsViewAllReconciliation();
				break;
			case 10:
				back = true;
				break;
			default:
				System.out.println("[ERROR] Invalid choice.");
			}
		}
	}

	private static void printOpsMenu() {
		System.out.println("\n+-----------------------------------------------------------+");
		System.out.println("|          SECTION 2 — SYSTEM OPERATIONS                   |");
		System.out.println("|       (all data fetched automatically from DB)            |");
		System.out.println("+-----------------------------------------------------------+");
		System.out.println("|  BATCH                                                   |");
		System.out.println("|  1.  Create Batches        (all active source systems)   |");
		System.out.println("|  2.  View All Batches                                    |");
		System.out.println("|  3.  View Batches by Status                              |");
		System.out.println("|                                                          |");
		System.out.println("|  SETTLEMENT                                              |");
		System.out.println("|  4.  Create Settlement     (all SCHEDULED batches)       |");
		System.out.println("|  5.  View All Settlements                                |");
		System.out.println("|                                                          |");
		System.out.println("|  NETTING                                                 |");
		System.out.println("|  6.  Run Netting           (all COMPLETED batches)       |");
		System.out.println("|  7.  View All Netting Positions                          |");
		System.out.println("|                                                          |");
		System.out.println("|  RECONCILIATION                                          |");
		System.out.println("|  8.  Run Reconciliation    (all COMPLETED batches)       |");
		System.out.println("|  9.  View All Reconciliation Entries                     |");
		System.out.println("|                                                          |");
		System.out.println("| 10.  Back to Main Menu                                   |");
		System.out.println("+-----------------------------------------------------------+");
	}

	// =========================================================================
	// 2-A BATCH
	// =========================================================================

	private static void opsCreateBatch() {
		System.out.println("\n=== CREATE BATCHES ===");
		List<SourceSystem> sources = sourceSystemDao.findAllActive();
		if (sources.isEmpty()) {
			System.out.println("[WARN] No active source systems found in DB.");
			return;
		}
		System.out.println("[INFO] Found " + sources.size() + " source system(s). Processing...\n");

		int total = 0;
		for (SourceSystem ss : sources) {
			System.out.println("[INFO] Source: " + ss.getSourceType() + " | File: " + ss.getFilePath());
			try {
				List<Batch> batches = batchService.createBatches(ss);
				if (batches.isEmpty()) {
					System.out.println("[WARN] No valid transactions for: " + ss.getSourceType());
				} else {
					for (Batch b : batches) {
						// FIX 1: Save the batch header to DB
						batchDao.saveBatch(b);

						// FIX 2: Persist every ingested transaction and link it to this batch.
						// Without this, incoming_transaction table stays empty after batching.
						for (IncomingTransaction txn : b.getTransactionList()) {
							// FIX: pass the batch's ID explicitly so the FK is satisfied
							// regardless of what batchId value came from the source file
							transactionDao.save(txn, b.getBatchId());
						}

						printBatchSummary(b);
						System.out.println("[INFO] Saved " + b.getTransactionList().size()
								+ " transaction(s) to DB for batch: " + b.getBatchId());
						total++;
					}
				}
			} catch (AdapterException e) {
				System.out.println("[ERROR] Adapter failed for " + ss.getSourceType() + ": " + e.getMessage());
			}
		}
		System.out.println("\n[SUMMARY] Total batches created and saved to DB: " + total);
	}

	private static void opsViewAllBatches() {
		System.out.println("\n=== ALL BATCHES ===");
		List<Batch> batches = batchDao.findAllBatches();
		if (batches.isEmpty()) {
			System.out.println("[INFO] No batches in DB.");
			return;
		}
		System.out.println("[INFO] Total: " + batches.size());
		batches.forEach(BankSettlementUtility::printBatchSummary);
	}

	private static void opsViewBatchesByStatus() {
		System.out.println("  Statuses: " + Arrays.toString(BatchStatus.values()));
		String input = readString("  Enter Status: ");
		BatchStatus status;
		try {
			status = BatchStatus.valueOf(input.toUpperCase());
		} catch (IllegalArgumentException e) {
			System.out.println("[ERROR] Invalid status.");
			return;
		}
		List<Batch> batches = batchDao.findBatchesByStatus(status);
		if (batches.isEmpty()) {
			System.out.println("[INFO] No batches with status: " + status);
			return;
		}
		System.out.println("[INFO] Found: " + batches.size());
		batches.forEach(BankSettlementUtility::printBatchSummary);
	}

	// =========================================================================
	// 2-B SETTLEMENT
	// =========================================================================

	private static void opsCreateSettlement() {
		System.out.println("\n=== CREATE SETTLEMENT ===");
		List<Batch> scheduled = batchDao.findBatchesByStatus(BatchStatus.SCHEDULED);
		if (scheduled.isEmpty()) {
			System.out.println("[INFO] No SCHEDULED batches to settle.");
			return;
		}
		System.out.println("[INFO] Found " + scheduled.size() + " SCHEDULED batch(es).\n");

		int success = 0, failed = 0;
		for (Batch batch : scheduled) {
			System.out.println("[INFO] Settling batch: " + batch.getBatchId());
			try {
				SettlementResult result = settlementService.settle(batch);
				settlementDao.saveSettlement(result);
				batchDao.updateBatchStatus(batch.getBatchId(), batch.getBatchStatus());
				printSettlementResult(result);
				success++;
			} catch (Exception e) {
				System.out.println("[ERROR] Failed for " + batch.getBatchId() + ": " + e.getMessage());
				batchDao.updateBatchStatus(batch.getBatchId(), BatchStatus.FAILED);
				failed++;
			}
		}
		System.out.println("\n[SUMMARY] Settled: " + success + " | Failed: " + failed);
	}

	private static void opsViewAllSettlements() {
		System.out.println("\n=== ALL SETTLEMENTS ===");
		List<SettlementResult> results = settlementDao.findAllSettlements();
		if (results.isEmpty()) {
			System.out.println("[INFO] No settlement records in DB.");
			return;
		}
		System.out.println("[INFO] Total: " + results.size());
		results.forEach(BankSettlementUtility::printSettlementResult);
	}

	// =========================================================================
	// 2-C NETTING
	// =========================================================================

	private static void opsRunNetting() {

	    System.out.println("\n=== RUN NETTING ===");

	    List<Batch> completed = batchDao.findBatchesByStatus(BatchStatus.COMPLETED);

	    if (completed.isEmpty()) {
	        System.out.println("[INFO] No COMPLETED batches available for netting.");
	        return;
	    }

	    int totalPositions = 0;
	    for (Batch batch : completed) {
	        System.out.println("[INFO] Netting batch: " + batch.getBatchId());
	        try {
	            List<NettingPosition> positions = nettingService.computeNetting(batch);
	            System.out.println("[INFO] Saved " + positions.size() + " netting records to DB");
	            positions.forEach(BankSettlementUtility::printNettingPosition);
	            totalPositions += positions.size();
	        } catch (Exception e) {
	            System.out.println("[ERROR] Netting failed for batch "
	                    + batch.getBatchId() + ": " + e.getMessage());
	        }
	    }
	    System.out.println("\n[SUMMARY] Total netting positions saved: " + totalPositions);
	}

	private static void opsViewAllNetting() {
		System.out.println("\n=== ALL NETTING POSITIONS ===");
		List<NettingPosition> list = nettingPositionDao.findAllNettingPositions();
		if (list.isEmpty()) {
			System.out.println("[INFO] No netting positions in DB.");
			return;
		}
		System.out.println("[INFO] Total: " + list.size());
		list.forEach(BankSettlementUtility::printNettingPosition);
	}

	// =========================================================================
	// 2-D RECONCILIATION
	// =========================================================================

	private static void opsRunReconciliation() {
	    System.out.println("\n=== RUN RECONCILIATION ===");

	    List<Batch> completed = batchDao.findBatchesByStatus(BatchStatus.COMPLETED);
	    if (completed.isEmpty()) {
	        System.out.println("[INFO] No COMPLETED batches available for reconciliation.");
	        return;
	    }
	    System.out.println("[INFO] Found " + completed.size() + " COMPLETED batch(es).\n");

	    int totalEntries = 0;
	    for (Batch batch : completed) {
	        System.out.println("[INFO] Reconciling batch: " + batch.getBatchId());
	        try {
	            List<NettingPosition> positions = nettingService.computeNetting(batch);

	            // FIX: reconcile() saves each entry to DB internally.
	            // Do NOT call reconciliationEntryDao.saveReconciliationEntry() here again —
	            // that was causing a primary-key violation on every entry.
	            List<ReconciliationEntry> entries = reconciliationService.reconcile(batch, positions);
	            for (ReconciliationEntry entry : entries) {
	                printReconciliationEntry(entry);   // display only — already saved inside reconcile()
	                totalEntries++;
	            }
	        } catch (Exception e) {
	            System.out.println("[ERROR] Reconciliation failed for "
	                    + batch.getBatchId() + ": " + e.getMessage());
	        }
	    }
	    System.out.println("\n[SUMMARY] Total reconciliation entries saved: " + totalEntries);
	}

	private static void opsViewAllReconciliation() {
		System.out.println("\n=== ALL RECONCILIATION ENTRIES ===");
		List<ReconciliationEntry> entries = reconciliationEntryDao.findAllReconciliationEntries();
		if (entries.isEmpty()) {
			System.out.println("[INFO] No reconciliation entries in DB.");
			return;
		}
		System.out.println("[INFO] Total: " + entries.size());
		entries.forEach(BankSettlementUtility::printReconciliationEntry);
	}

	// =========================================================================
	// PRINT HELPERS — Operations section
	// =========================================================================

	private static void printBatchSummary(Batch b) {
		System.out.println("  +- Batch -------------------------------------------------");
		System.out.println("  | Batch ID    : " + b.getBatchId());
		System.out.println("  | Date        : " + b.getBatchDate());
		System.out.println("  | Status      : " + b.getBatchStatus());
		System.out.println("  | Transactions: " + b.getTotalTransactions());
		System.out.println("  | Total Amount: " + b.getTotalAmount());
		System.out.println("  +---------------------------------------------------------");
	}

	private static void printSettlementResult(SettlementResult r) {
		System.out.println("  +- Settlement --------------------------------------------");
		System.out.println("  | Batch ID      : " + r.getBatchId());
		System.out.println("  | Status        : " + r.getStatus());
		System.out.println("  | Settled Count : " + r.getSettledCount());
		System.out.println("  | Failed Count  : " + r.getFailedCount());
		System.out.println("  | Settled Amount: " + r.getTotalSettledAmount());
		System.out.println("  | Processed At  : " + r.getProcessedAt());
		System.out.println("  +---------------------------------------------------------");
	}

	private static void printNettingPosition(NettingPosition p) {
		System.out.println("  +- Netting Position --------------------------------------");
		System.out.println("  | Position ID  : " + p.getPositiionId());
		System.out.println("  | Counterparty : " + p.getCounterpartyBankId());
		System.out.println("  | Gross Debit  : " + p.getGrossDebitAmount());
		System.out.println("  | Gross Credit : " + p.getGrossCreditAmount());
		System.out.println("  | Net Amount   : " + p.getNetAmount());
		System.out.println("  | Position Date: " + p.getPositionDate());
		System.out.println("  +---------------------------------------------------------");
	}

	private static void printReconciliationEntry(ReconciliationEntry e) {
		System.out.println("  +- Reconciliation Entry ----------------------------------");
		System.out.println("  | Entry ID     : " + e.getEntryId());
		System.out.println("  | Recon Date   : " + e.getReconciliationDate());
		System.out.println("  | Account ID   : " + e.getAccountId());
		System.out.println("  | Expected Amt : " + e.getExpectedAmount());
		System.out.println("  | Actual Amt   : " + e.getActualAmount());
		System.out.println("  | Variance     : " + e.getVariance());
		System.out.println("  | Status       : " + e.getReconStatus());
		System.out.println("  +---------------------------------------------------------");
	}

	// =========================================================================
	// INPUT HELPERS
	// =========================================================================

	private static int readInt(String prompt) {
		while (true) {
			System.out.print(prompt);
			try {
				return Integer.parseInt(scanner.nextLine().trim());
			} catch (NumberFormatException e) {
				System.out.println("[ERROR] Please enter a valid integer.");
			}
		}
	}

	private static long readLong(String prompt) {
		while (true) {
			System.out.print(prompt);
			try {
				return Long.parseLong(scanner.nextLine().trim());
			} catch (NumberFormatException e) {
				System.out.println("[ERROR] Please enter a valid number.");
			}
		}
	}

	private static BigDecimal readBigDecimal(String prompt) {
		while (true) {
			System.out.print(prompt);
			try {
				return new BigDecimal(scanner.nextLine().trim());
			} catch (NumberFormatException e) {
				System.out.println("[ERROR] Please enter a valid decimal.");
			}
		}
	}

	private static String readString(String prompt) {
		System.out.print(prompt);
		return scanner.nextLine().trim();
	}

	private static String readStringWithDefault(String prompt, String defaultValue) {
		System.out.print(prompt);
		String input = scanner.nextLine().trim();
		return input.isEmpty() ? defaultValue : input;
	}

	private static boolean readBoolean(String prompt) {
		System.out.print(prompt);
		return scanner.nextLine().trim().equalsIgnoreCase("y");
	}

	private static AccountType readAccountType() {
		System.out.println("  Account Types: " + Arrays.toString(AccountType.values()));
		while (true) {
			System.out.print("  Account Type : ");
			try {
				return AccountType.valueOf(scanner.nextLine().trim().toUpperCase());
			} catch (IllegalArgumentException e) {
				System.out.println("[ERROR] Invalid type. Choose from: " + Arrays.toString(AccountType.values()));
			}
		}
	}
}