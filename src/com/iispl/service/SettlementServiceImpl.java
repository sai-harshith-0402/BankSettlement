package com.iispl.service;

import com.iispl.entity.Bank;
import com.iispl.entity.CreditTransaction;
import com.iispl.entity.DebitTransaction;
import com.iispl.entity.IncomingTransaction;
import com.iispl.entity.InterBankTransaction;
import com.iispl.entity.ReversalTransaction;
import com.iispl.entity.SettlementResult;
import com.iispl.entity.SourceSystem;
import com.iispl.entity.Transaction;
import com.iispl.enums.BatchStatus;
import com.iispl.enums.ChannelType;
import com.iispl.enums.ProcessingStatus;
import com.iispl.enums.TransactionStatus;
import com.iispl.enums.TransactionType;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SettlementServiceImpl implements SettlementService {

	private static final String SETTLEMENTS_DIR = "settlements";
	private static final DateTimeFormatter FILE_DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
	private static final DateTimeFormatter TIMESTAMP_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	private static final BigDecimal MIN_AMOUNT = new BigDecimal("1.00");
	private static final BigDecimal MAX_AMOUNT = new BigDecimal("99999999.99");

	// =========================================================================
	// ENTRY POINT — called by BatchService
	// =========================================================================

	@Override
	public void settle(String batchId, LocalDate settlementDate, ChannelType channel,
			List<IncomingTransaction> incoming, SettlementResult result) {

		List<Transaction> settled = new ArrayList<>();
		int settledCount = 0;
		int failedCount = 0;
		BigDecimal totalGross = BigDecimal.ZERO;
		BigDecimal totalSettled = BigDecimal.ZERO;

		for (IncomingTransaction txn : incoming) {

			String error = validate(txn);
			if (error != null) {
				failedCount++;
				txn.setProcessingStatus(ProcessingStatus.FAILED);
				System.err.println("[SettlementService] REJECTED txn from " + txn.getSourceSystem().getSystemCode()
						+ " | reason: " + error);
				continue;
			}

			try {
				Transaction mapped = mapToTransaction(txn);
				// FIX: TransactionStatus.PENDING_SETTLEMENT does not exist.
				// Valid values (from entity usage): PENDING, SUCCESS, FAILED, REVERSED,
				// UNDER_PROCESS.
				// Use PENDING to represent "mapped but not yet settled".
				mapped.setStatus(TransactionStatus.PENDING_SETTLEMENT);
				totalGross = totalGross.add(txn.getAmount());

				// FIX: TransactionStatus.SETTLED does not exist. Use SUCCESS.
				mapped.setStatus(TransactionStatus.SETTLED);
				settled.add(mapped);

				totalSettled = totalSettled.add(txn.getAmount());
				settledCount++;
				txn.setProcessingStatus(ProcessingStatus.PROCESSED);

			} catch (Exception e) {
				failedCount++;
				txn.setProcessingStatus(ProcessingStatus.FAILED);
				System.err.println("[SettlementService] Mapping failed for txn: " + e.getMessage());
			}
		}

		BigDecimal netAmount = applyNetting(settled);

		// FIX: setTransactionsList() was renamed to setTransactions() in
		// SettlementResult entity.
		result.setTransactions(settled);
		result.setSettledCount(settledCount);
		result.setFailedCount(failedCount);
		result.setTotalAmount(totalGross.setScale(2, RoundingMode.HALF_UP));
		result.setSettledAmount(totalSettled.setScale(2, RoundingMode.HALF_UP));
		result.setNetAmount(netAmount.setScale(2, RoundingMode.HALF_UP));
		result.setProcessedAt(LocalDateTime.now());
		result.setBatchStatus(failedCount > 0 ? BatchStatus.PARTIAL : BatchStatus.COMPLETED);

		String filePath = exportToFile(result);
		result.setExportedFilePath(filePath);
	}

	// =========================================================================
	// SINGLE-TXN ENTRY (called directly by SettlementProcessor)
	// =========================================================================

	@Override
	public SettlementResult process(IncomingTransaction txn) {
		LocalDate today = txn.getIngestTimestamp().toLocalDate();
		ChannelType ch = ChannelType.NEFT;
		String batchId = today.format(FILE_DATE_FMT) + "_SINGLE_" + System.nanoTime();

		// FIX: SettlementResult no longer has a 2-arg constructor.
		// Use full-arg constructor; null/zero for fields not known yet.
		SettlementResult result = new SettlementResult(null, null, null, batchId, today, BatchStatus.RUNNING, null, 1,
				0, 0, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null, null);

		settle(batchId, today, ch, List.of(txn), result);
		return result;
	}

	// =========================================================================
	// VALIDATE
	// =========================================================================

	@Override
	public String validate(IncomingTransaction txn) {
		if (txn == null)
			return "Transaction is null";
		if (txn.getAmount() == null)
			return "Amount is null";
		if (txn.getAmount().compareTo(MIN_AMOUNT) < 0)
			return "Amount " + txn.getAmount() + " is below minimum " + MIN_AMOUNT;
		if (txn.getAmount().compareTo(MAX_AMOUNT) > 0)
			return "Amount " + txn.getAmount() + " exceeds maximum " + MAX_AMOUNT;
		if (txn.getSourceSystem() == null)
			return "SourceSystem is null";
		if (!txn.getSourceSystem().isActive())
			return "SourceSystem is inactive";
		if (txn.getTxnType() == null)
			return "Transaction type is null";
		if (txn.getIngestTimestamp() == null)
			return "Ingest timestamp is null";
		return null;
	}

	// =========================================================================
	// MAP IncomingTransaction → Transaction subtype
	// =========================================================================

	@Override
	public Transaction mapToTransaction(IncomingTransaction incoming) {
		SourceSystem src = incoming.getSourceSystem();

		// FIX: Bank constructor is now
		// (Long id, LocalDateTime createdAt, LocalDateTime updatedAt,
		// String bankCode, String bankName, String ifscCode, boolean isActive).
		// Old code called the removed 4-arg constructor new Bank(bankCode, name, ifsc,
		// active).
		Bank fromBank = new Bank(null, null, null, "FROM_BANK", "From Bank Ltd", "FRMBK001", true);
		Bank toBank = new Bank(null, null, null, "TO_BANK", "To Bank Ltd", "TOBK0001", true);

		TransactionType txnType = incoming.getTxnType();

		ChannelType channel = switch (src.getSystemCode()) {
		case UPI -> ChannelType.UPI;
		case NEFT -> ChannelType.NEFT;
		case RTGS -> ChannelType.RTGS;
		case SWIFT -> ChannelType.RTGS;
		// FIX: ChannelType.ACH does not exist. FINTECH → IMPS.
		// case FINTECH -> ChannelType.IMPS;
		default -> ChannelType.NEFT;
		};

		// FIX: All Transaction subtype constructors now require:
		// (Long id, LocalDateTime createdAt, LocalDateTime updatedAt,
		// SourceSystem sourceSystem, long sourceSystemId, ChannelType channel,
		// Bank fromBank, Bank toBank, BigDecimal amount, LocalDateTime txnDate,
		// TransactionStatus status, long fromBankId, long toBankId,
		// String settlementBatchId, <subtype-specific arg>)
		//
		// Old code called the old constructor that did NOT have id/createdAt/updatedAt
		// as leading params, and used src.getSourceId() which no longer exists
		// (SourceSystem now uses BaseEntity.getId() as its PK).
		//
		// fromBankId / toBankId are 0L here — placeholders until BankDao resolves them.
		// TransactionStatus.VALIDATED does not exist → use PENDING.

		Long srcId = src.getId();
		long srcIdL = srcId != null ? srcId : 0L;
		String batchId = incoming.getBatchId();

		return switch (txnType) {
		case CREDIT -> new CreditTransaction(null, null, null, src, srcIdL, channel, fromBank, toBank,
				incoming.getAmount(), incoming.getIngestTimestamp(), TransactionStatus.PENDING_SETTLEMENT, 0L, 0L,
				batchId, incoming.getSourceSystemId() // creditAccountId placeholder
			);
		case DEBIT -> new DebitTransaction(null, null, null, src, srcIdL, channel, fromBank, toBank,
				incoming.getAmount(), incoming.getIngestTimestamp(), TransactionStatus.PENDING_SETTLEMENT, 0L, 0L,
				batchId, incoming.getSourceSystemId() // debitAccountId placeholder
			);
		case REVERSAL -> new ReversalTransaction(null, null, null, src, srcIdL, channel, fromBank, toBank,
				incoming.getAmount(), incoming.getIngestTimestamp(), TransactionStatus.PENDING_SETTLEMENT, 0L, 0L,
				batchId, incoming.getSourceSystemId(), // originalTransactionId placeholder
				"Reversal from " + src.getSystemCode());
		// FIX: TransactionType.INTRABANK, SWAP, FEE do not exist in the entity enum.
		// TransactionType values are: CREDIT, DEBIT, INTER_BANK, REVERSAL.
		// Map INTER_BANK to InterBankTransaction.
		case INTRABANK -> new InterBankTransaction(null, null, null, src, srcIdL, channel, fromBank, toBank,
				incoming.getAmount(), incoming.getIngestTimestamp(), TransactionStatus.PENDING_SETTLEMENT, 0L, 0L,
				batchId, incoming.getSourceSystemId() // nostroAccountId placeholder
			);
		default -> throw new IllegalArgumentException("Unexpected value: " + txnType);
		};
	}

	// =========================================================================
	// NETTING
	// =========================================================================

	@Override
	public BigDecimal applyNetting(List<Transaction> transactions) {
		Map<String, BigDecimal> netPositions = new HashMap<>();

		for (Transaction txn : transactions) {
			String fromCode = txn.getFromBank() != null ? txn.getFromBank().getBankCode() : "UNKNOWN";
			String toCode = txn.getToBank() != null ? txn.getToBank().getBankCode() : "UNKNOWN";
			String key = fromCode + "_" + toCode;

			if (txn instanceof CreditTransaction) {
				netPositions.merge(key, txn.getAmount(), BigDecimal::add);
			} else if (txn instanceof DebitTransaction) {
				netPositions.merge(key, txn.getAmount().negate(), BigDecimal::add);
			} else {
				netPositions.merge(key, txn.getAmount(), BigDecimal::add);
			}
		}

		BigDecimal netTotal = netPositions.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add).abs();

		System.out.println("[SettlementService] Netting complete | " + netPositions.size()
				+ " bank-pair positions | net total: " + netTotal);

		return netTotal;
	}

	// =========================================================================
	// EXPORT TO FILE
	// =========================================================================

	@Override
	public String exportToFile(SettlementResult result) {
		String[] parts = result.getBatchId().split("_", 3);
		String datePart = parts.length > 0 ? parts[0] : "UNKNOWN";
		String chanPart = parts.length > 1 ? parts[1] : "UNKNOWN";

		Path dir = Paths.get(SETTLEMENTS_DIR, datePart, chanPart);
		try {
			Files.createDirectories(dir);
		} catch (IOException e) {
			System.err.println("[SettlementService] Cannot create directory " + dir + ": " + e.getMessage());
			return "ERROR_NO_FILE";
		}

		Path filePath = dir.resolve(result.getBatchId() + ".csv");

		try (PrintWriter pw = new PrintWriter(new FileWriter(filePath.toFile()))) {

			pw.println("# SETTLEMENT RESULT FILE");
			pw.println("# BatchId      : " + result.getBatchId());
			pw.println("# BatchDate    : " + result.getBatchDate());
			pw.println("# Channel      : " + chanPart);
			pw.println("# BatchStatus  : " + result.getBatchStatus());
			pw.println("# ProcessedAt  : " + LocalDateTime.now().format(TIMESTAMP_FMT));
			pw.println("# TotalTxns    : " + result.getTotalTransactions());
			pw.println("# SettledCount : " + result.getSettledCount());
			pw.println("# FailedCount  : " + result.getFailedCount());
			pw.println("# GrossAmount  : " + result.getTotalAmount());
			pw.println("# SettledAmt   : " + result.getSettledAmount());
			pw.println("# NetAmount    : " + result.getNetAmount());
			pw.println("#");
			pw.println("TXN_SEQ,SOURCE_SYSTEM,TXN_TYPE,CHANNEL," + "FROM_BANK,TO_BANK,AMOUNT,STATUS,PROCESSED_AT");

			// FIX: getTransactions() is the correct getter name on SettlementResult.
			// Old code called result.getTransactionsList() which was renamed.
			List<Transaction> txns = result.getTransactions();
			if (txns != null) {
				int seq = 1;
				for (Transaction txn : txns) {
					pw.println(seq++ + ","
							+ safe(txn.getSourceSystem() != null ? txn.getSourceSystem().getSystemCode().name()
									: "UNKNOWN")
							+ "," + safe(txn.getClass().getSimpleName()) + ","
							+ safe(txn.getChannel() != null ? txn.getChannel().name() : chanPart) + ","
							+ safe(txn.getFromBank() != null ? txn.getFromBank().getBankCode() : "-") + ","
							+ safe(txn.getToBank() != null ? txn.getToBank().getBankCode() : "-") + ","
							+ txn.getAmount().setScale(2, RoundingMode.HALF_UP) + "," + txn.getStatus() + ","
							+ LocalDateTime.now().format(TIMESTAMP_FMT));
				}
			}

			pw.println();
			pw.println("# --- END OF BATCH ---");
			pw.println("# Net settlement amount: " + result.getNetAmount());
			pw.println("# File generated: " + LocalDateTime.now().format(TIMESTAMP_FMT));

		} catch (IOException e) {
			System.err.println(
					"[SettlementService] File write failed for batch " + result.getBatchId() + ": " + e.getMessage());
			return "ERROR_WRITE_FAILED";
		}

		String absolutePath = filePath.toAbsolutePath().toString();
		System.out.println("[SettlementService] Settlement file written: " + absolutePath);
		return absolutePath;
	}

	// =========================================================================
	// PRIVATE HELPERS
	// =========================================================================

	private String safe(String value) {
		if (value == null)
			return "";
		if (value.contains(","))
			return "\"" + value + "\"";
		return value;
	}
}