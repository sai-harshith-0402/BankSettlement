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
import com.iispl.enums.SettlementStatus;
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

/**
 * SettlementServiceImpl
 *
 * Responsibilities:
 *  1. Validate each IncomingTransaction
 *  2. Map to concrete Transaction subtype
 *  3. Apply netting across bank pairs
 *  4. Compute totals (gross, settled, net)
 *  5. Write result CSV to disk under settlements/<date>/<channel>/
 */
public class SettlementServiceImpl implements SettlementService {

    private static final String SETTLEMENTS_DIR = "settlements";
    private static final DateTimeFormatter FILE_DATE_FMT =
            DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter TIMESTAMP_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // Minimum and maximum single transaction limits
    private static final BigDecimal MIN_AMOUNT = new BigDecimal("1.00");
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("99999999.99");

    // =========================================================================
    // ENTRY POINT — called by BatchService
    // =========================================================================

    @Override
    public void settle(String batchId,
                       LocalDate settlementDate,
                       ChannelType channel,
                       List<IncomingTransaction> incoming,
                       SettlementResult result) {

        List<Transaction> settled  = new ArrayList<>();
        int settledCount = 0;
        int failedCount  = 0;
        BigDecimal totalGross    = BigDecimal.ZERO;
        BigDecimal totalSettled  = BigDecimal.ZERO;

        for (IncomingTransaction txn : incoming) {

            // --- VALIDATE ---
            String error = validate(txn);
            if (error != null) {
                failedCount++;
                txn.setProcessingStatus(ProcessingStatus.FAILED);
                System.err.println("[SettlementService] REJECTED txn from "
                        + txn.getSourceSystem().getSystemCode()
                        + " | reason: " + error);
                continue;
            }

            // --- MAP ---
            try {
                Transaction mapped = mapToTransaction(txn);
                mapped.setStatus(TransactionStatus.PENDING_SETTLEMENT);
                totalGross = totalGross.add(txn.getAmount());

                // Settle: mark SETTLED (balance updates would happen via AccountDao)
                mapped.setStatus(TransactionStatus.SETTLED);
                settled.add(mapped);

                totalSettled = totalSettled.add(txn.getAmount());
                settledCount++;
                txn.setProcessingStatus(ProcessingStatus.PROCESSED);

            } catch (Exception e) {
                failedCount++;
                txn.setProcessingStatus(ProcessingStatus.FAILED);
                System.err.println("[SettlementService] Mapping failed for txn: "
                        + e.getMessage());
            }
        }

        // --- NET ---
        BigDecimal netAmount = applyNetting(settled);

        // --- POPULATE RESULT ---
        result.setTransactionsList(settled);
        result.setSettledCount(settledCount);
        result.setFailedCount(failedCount);
        result.setTotalAmount(totalGross.setScale(2, RoundingMode.HALF_UP));
        result.setSettledAmount(totalSettled.setScale(2, RoundingMode.HALF_UP));
        result.setNetAmount(netAmount.setScale(2, RoundingMode.HALF_UP));
        result.setProcessedAt(LocalDateTime.now());
        result.setBatchStatus(failedCount > 0 ? BatchStatus.PARTIAL : BatchStatus.COMPLETED);

        // --- EXPORT FILE ---
        String filePath = exportToFile(result);
        result.setExportedFilePath(filePath);
    }

    // =========================================================================
    // SINGLE-TXN ENTRY (called directly by SettlementProcessor)
    // =========================================================================

    @Override
    public SettlementResult process(IncomingTransaction txn) {
        LocalDate today   = txn.getIngestTimestamp().toLocalDate();
        ChannelType ch    = ChannelType.INTERNAL;  // default; BatchService refines
        String batchId    = today.format(FILE_DATE_FMT) + "_SINGLE_"
                + System.nanoTime();

        SettlementResult result = new SettlementResult(batchId, today);
        result.setTotalTransactions(1);

        settle(batchId, today, ch, List.of(txn), result);
        return result;
    }

    // =========================================================================
    // VALIDATE
    // =========================================================================

    @Override
    public String validate(IncomingTransaction txn) {
        if (txn == null) return "Transaction is null";
        if (txn.getAmount() == null) return "Amount is null";
        if (txn.getAmount().compareTo(MIN_AMOUNT) < 0)
            return "Amount " + txn.getAmount() + " is below minimum " + MIN_AMOUNT;
        if (txn.getAmount().compareTo(MAX_AMOUNT) > 0)
            return "Amount " + txn.getAmount() + " exceeds maximum " + MAX_AMOUNT;
        if (txn.getSourceSystem() == null) return "SourceSystem is null";
        if (!txn.getSourceSystem().isActive()) return "SourceSystem is inactive";
        if (txn.getTxnType() == null) return "Transaction type is null";
        if (txn.getIngestTimestamp() == null) return "Ingest timestamp is null";
        return null;  // valid
    }

    // =========================================================================
    // MAP IncomingTransaction → Transaction subtype
    // =========================================================================

    @Override
    public Transaction mapToTransaction(IncomingTransaction incoming) {
        SourceSystem src = incoming.getSourceSystem();

        // Placeholder banks — in a real system these come from BankDao lookups
        Bank fromBank = new Bank("FROM_BANK", "From Bank Ltd", "FRMBK001", true);
        Bank toBank   = new Bank("TO_BANK",   "To Bank Ltd",   "TOBK0001", true);

        TransactionType txnType = incoming.getTxnType();

        // Determine ChannelType from SourceType
        ChannelType channel = switch (src.getSystemCode()) {
            case UPI           -> ChannelType.UPI;
            case NEFT          -> ChannelType.NEFT;
            case RTGS          -> ChannelType.RTGS;
            case SWIFT         -> ChannelType.SWIFT;
            case FINTECH       -> ChannelType.ACH;
            default            -> ChannelType.INTERNAL;
        };

        return switch (txnType) {
            case CREDIT -> new CreditTransaction(
                    src, src.getSourceId(), channel,
                    fromBank, toBank,
                    incoming.getAmount(), incoming.getIngestTimestamp(),
                    TransactionStatus.VALIDATED,
                    0L, 0L,
                    incoming.getSourceSystemId()   // creditAccountId
            );
            case DEBIT -> new DebitTransaction(
                    src, src.getSourceId(), channel,
                    fromBank, toBank,
                    incoming.getAmount(), incoming.getIngestTimestamp(),
                    TransactionStatus.VALIDATED,
                    0L, 0L,
                    incoming.getSourceSystemId()   // debitAccountId
            );
            case REVERSAL -> new ReversalTransaction(
                    src, src.getSourceId(), channel,
                    fromBank, toBank,
                    incoming.getAmount(), incoming.getIngestTimestamp(),
                    TransactionStatus.VALIDATED,
                    0L, 0L,
                    incoming.getSourceSystemId(),  // originalTransactionId
                    "Reversal from " + src.getSystemCode()
            );
            case INTRABANK, SWAP, FEE -> new InterBankTransaction(
                    src, src.getSourceId(), channel,
                    fromBank, toBank,
                    incoming.getAmount(), incoming.getIngestTimestamp(),
                    TransactionStatus.VALIDATED,
                    0L, 0L,
                    incoming.getSourceSystemId()   // nostroAccountId
            );
        };
    }

    // =========================================================================
    // NETTING — collapses offsetting CREDIT/DEBIT pairs per bank-pair
    // =========================================================================

    @Override
    public BigDecimal applyNetting(List<Transaction> transactions) {
        // Key: "fromBank_toBank"
        Map<String, BigDecimal> netPositions = new HashMap<>();

        for (Transaction txn : transactions) {
            String fromCode = txn.getFromBank() != null
                    ? txn.getFromBank().getBankCode() : "UNKNOWN";
            String toCode   = txn.getToBank()   != null
                    ? txn.getToBank().getBankCode()   : "UNKNOWN";
            String key = fromCode + "_" + toCode;

            if (txn instanceof CreditTransaction) {
                netPositions.merge(key, txn.getAmount(), BigDecimal::add);
            } else if (txn instanceof DebitTransaction) {
                netPositions.merge(key, txn.getAmount().negate(), BigDecimal::add);
            } else {
                netPositions.merge(key, txn.getAmount(), BigDecimal::add);
            }
        }

        BigDecimal netTotal = netPositions.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .abs();

        System.out.println("[SettlementService] Netting complete | "
                + netPositions.size() + " bank-pair positions | net total: " + netTotal);

        return netTotal;
    }

    // =========================================================================
    // EXPORT TO FILE — writes CSV under settlements/<date>/<channel>/
    // =========================================================================

    @Override
    public String exportToFile(SettlementResult result) {
        // Derive channel from batchId: "20250401_NEFT_001" → "NEFT"
        String[] parts   = result.getBatchId().split("_", 3);
        String datePart  = parts.length > 0 ? parts[0] : "UNKNOWN";
        String chanPart  = parts.length > 1 ? parts[1] : "UNKNOWN";

        // Directory: settlements/20250401/NEFT/
        Path dir = Paths.get(SETTLEMENTS_DIR, datePart, chanPart);
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            System.err.println("[SettlementService] Cannot create directory "
                    + dir + ": " + e.getMessage());
            return "ERROR_NO_FILE";
        }

        // File: settlements/20250401/NEFT/20250401_NEFT_001.csv
        Path filePath = dir.resolve(result.getBatchId() + ".csv");

        try (PrintWriter pw = new PrintWriter(new FileWriter(filePath.toFile()))) {

            // ---- HEADER BLOCK ----
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

            // ---- COLUMN HEADERS ----
            pw.println("TXN_SEQ,SOURCE_SYSTEM,TXN_TYPE,CHANNEL,"
                    + "FROM_BANK,TO_BANK,AMOUNT,STATUS,PROCESSED_AT");

            // ---- DATA ROWS ----
            List<Transaction> txns = result.getTransactions();
            if (txns != null) {
                int seq = 1;
                for (Transaction txn : txns) {
                    pw.println(
                            seq++
                            + "," + safe(txn.getSourceSystem() != null
                                    ? txn.getSourceSystem().getSystemCode().name() : "UNKNOWN")
                            + "," + safe(txn.getClass().getSimpleName())
                            + "," + safe(txn.getChannel() != null
                                    ? txn.getChannel().name() : chanPart)
                            + "," + safe(txn.getFromBank() != null
                                    ? txn.getFromBank().getBankCode() : "-")
                            + "," + safe(txn.getToBank() != null
                                    ? txn.getToBank().getBankCode() : "-")
                            + "," + txn.getAmount().setScale(2, RoundingMode.HALF_UP)
                            + "," + txn.getStatus()
                            + "," + LocalDateTime.now().format(TIMESTAMP_FMT)
                    );
                }
            }

            // ---- FOOTER SUMMARY ----
            pw.println();
            pw.println("# --- END OF BATCH ---");
            pw.println("# Net settlement amount: " + result.getNetAmount());
            pw.println("# File generated: " + LocalDateTime.now().format(TIMESTAMP_FMT));

        } catch (IOException e) {
            System.err.println("[SettlementService] File write failed for batch "
                    + result.getBatchId() + ": " + e.getMessage());
            return "ERROR_WRITE_FAILED";
        }

        String absolutePath = filePath.toAbsolutePath().toString();
        System.out.println("[SettlementService] Settlement file written: " + absolutePath);
        return absolutePath;
    }

    // =========================================================================
    // PRIVATE HELPERS
    // =========================================================================

    /** Escapes commas in CSV fields by wrapping in quotes. */
    private String safe(String value) {
        if (value == null) return "";
        if (value.contains(",")) return "\"" + value + "\"";
        return value;
    }
}