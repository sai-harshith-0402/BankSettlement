package com.iispl.ingestion;

import com.iispl.entity.CreditTransaction;
import com.iispl.entity.DebitTransaction;
import com.iispl.entity.IncomingTransaction;
import com.iispl.entity.InterBankTransaction;
import com.iispl.entity.ReversalTransaction;
import com.iispl.entity.SourceSystem;
import com.iispl.enums.ChannelType;
import com.iispl.enums.ProcessingStatus;
import com.iispl.enums.SourceType;
import com.iispl.enums.TransactionType;
import com.iispl.exception.AdapterException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * SwiftAdapter — SWIFT ingestion adapter.
 * File format : CSV
 * SourceType  : SWIFT
 *
 * Expected CSV structure (resources/swift_transactions.csv):
 * ──────────────────────────────────────────────────────────
 * Header row (first line) contains column names.
 * Extra columns are silently ignored.
 * Required columns:
 *   incomingTnxId, sourceSystemId, transactionType, channelType,
 *   fromBankName, toBankName, amount, processingStatus,
 *   ingestionTimeStamp, batchId
 * Subtype-specific (optional per row):
 *   creditAccountId, debitAccountId,
 *   originalTransactionId, reversalType,
 *   nostroAccountId, vostroAccountId
 *
 * Example:
 *   incomingTnxId,sourceSystemId,transactionType,channelType,...
 *   3001,3,CREDIT,SWIFT,Deutsche,HSBC,500000.00,RECEIVED,2025-04-07T11:00:00,BATCH-SWIFT-001,6001,,,,,
 *
 * Notes:
 *  - Values with commas must be quoted: "Deutsche Bank, Frankfurt"
 *  - Blank/empty lines are skipped
 *  - Timestamp format: yyyy-MM-dd'T'HH:mm:ss
 */
public class SwiftAdapter implements TransactionAdapter {

    // =========================================================================
    // CONSTANTS
    // =========================================================================

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private static final String CSV_DELIMITER = ",";

    private static final Set<String> ALLOWED_COLUMNS = Set.of(
            "incomingTnxId",
            "sourceSystemId",
            "transactionType",
            "channelType",
            "fromBankName",
            "toBankName",
            "amount",
            "processingStatus",
            "ingestionTimeStamp",
            "batchId",
            "creditAccountId",
            "debitAccountId",
            "originalTransactionId",
            "reversalType",
            "nostroAccountId",
            "vostroAccountId"
    );

    // =========================================================================
    // getSourceSystemType
    // =========================================================================

    @Override
    public String getSourceSystemType() {
        return SourceType.SWIFT.name();
    }

    // =========================================================================
    // INGEST
    // =========================================================================

    public List<IncomingTransaction> ingest(String filePath) throws AdapterException {
        List<IncomingTransaction> results = new ArrayList<>();

        File file = new File(filePath);
        if (!file.exists()) {
            throw new AdapterException(
                    "SWIFT CSV file not found: " + filePath, getSourceSystemType());
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {

            // ── Read header line ───────────────────────────────────────────────
            String headerLine = reader.readLine();
            if (headerLine == null || headerLine.isBlank()) {
                throw new AdapterException(
                        "SWIFT CSV has no header row: " + filePath, getSourceSystemType());
            }

            String[] headers = splitCsvLine(headerLine);
            logExtraColumns(headers, filePath);

            int rowIdx = 1;
            String line;

            while ((line = reader.readLine()) != null) {
                rowIdx++;

                // Skip blank lines
                if (line.isBlank()) {
                    System.out.println("[SwiftAdapter] Skipping blank line at row " + rowIdx);
                    continue;
                }

                try {
                    String[]            values = splitCsvLine(line);
                    Map<String, String> rawMap  = zipHeadersAndValues(headers, values, rowIdx);
                    Map<String, String> cleanMap = stripExtraColumns(rawMap);
                    results.add(adapt(cleanMap));
                } catch (AdapterException ae) {
                    System.err.println("[SwiftAdapter] Skipping row " + rowIdx
                            + " — " + ae.getMessage());
                }
            }

            System.out.println("[SwiftAdapter] Successfully ingested "
                    + results.size() + " transactions from: " + filePath);

        } catch (AdapterException ae) {
            throw ae;
        } catch (Exception e) {
            throw new AdapterException(
                    "SWIFT CSV ingestion failed for [" + filePath + "]: " + e.getMessage(),
                    getSourceSystemType(), e);
        }

        return results;
    }

    // =========================================================================
    // ADAPT
    // =========================================================================

    @Override
    public IncomingTransaction adapt(Map<String, String> row) throws AdapterException {
        // Safety net — strip extra columns even if called directly
        Map<String, String> cleanRow = stripExtraColumns(row);

        try {
            long             incomingTnxId      = parseLong(cleanRow,    "incomingTnxId");
            long             sourceSystemId     = parseLong(cleanRow,    "sourceSystemId");
            TransactionType  transactionType    = parseEnum(cleanRow,    "transactionType",  TransactionType.class);
            ChannelType      channelType        = parseEnum(cleanRow,    "channelType",       ChannelType.class);
            String           fromBankName       = requireValue(cleanRow, "fromBankName");
            String           toBankName         = requireValue(cleanRow, "toBankName");
            BigDecimal       amount             = parseDecimal(cleanRow, "amount");
            ProcessingStatus processingStatus   = parseEnum(cleanRow,    "processingStatus", ProcessingStatus.class);
            LocalDateTime    ingestionTimeStamp = parseDateTime(cleanRow,"ingestionTimeStamp");
            String           batchId            = cleanRow.getOrDefault("batchId", null);

            SourceSystem sourceSystem = new SourceSystem(sourceSystemId, SourceType.SWIFT, "");

            switch (transactionType) {

                case CREDIT: {
                    long creditAccountId = parseLong(cleanRow, "creditAccountId");
                    return new CreditTransaction(
                            incomingTnxId, sourceSystem, sourceSystemId,
                            transactionType, channelType, fromBankName, toBankName,
                            amount, processingStatus, ingestionTimeStamp, batchId,
                            creditAccountId);
                }

                case DEBIT: {
                    long debitAccountId = parseLong(cleanRow, "debitAccountId");
                    return new DebitTransaction(
                            incomingTnxId, sourceSystem, sourceSystemId,
                            transactionType, channelType, fromBankName, toBankName,
                            amount, processingStatus, ingestionTimeStamp, batchId,
                            debitAccountId);
                }

                case REVERSAL: {
                    long   originalTransactionId = parseLong(cleanRow,    "originalTransactionId");
                    String reversalType          = requireValue(cleanRow, "reversalType");
                    return new ReversalTransaction(
                            incomingTnxId, sourceSystem, sourceSystemId,
                            transactionType, channelType, fromBankName, toBankName,
                            amount, processingStatus, ingestionTimeStamp, batchId,
                            originalTransactionId, reversalType);
                }

                case INTRABANK: {
                    long nostroAccountId = parseLong(cleanRow, "nostroAccountId");
                    long vostroAccountId = parseLong(cleanRow, "vostroAccountId");
                    return new InterBankTransaction(
                            incomingTnxId, sourceSystem, sourceSystemId,
                            transactionType, channelType, fromBankName, toBankName,
                            amount, processingStatus, ingestionTimeStamp, batchId,
                            nostroAccountId, vostroAccountId);
                }

                default:
                    throw new AdapterException(
                            "Unknown transactionType: " + transactionType, getSourceSystemType());
            }

        } catch (AdapterException ae) {
            throw ae;
        } catch (Exception e) {
            throw new AdapterException(
                    "SWIFT adapt() failed: " + e.getMessage(), getSourceSystemType(), e);
        }
    }

    // =========================================================================
    // SEGREGATE
    // =========================================================================

    @Override
    public Map<TransactionType, List<IncomingTransaction>> segregate(
            List<IncomingTransaction> transactions) {

        Map<TransactionType, List<IncomingTransaction>> segregated = new HashMap<>();
        for (TransactionType type : TransactionType.values()) {
            segregated.put(type, new ArrayList<>());
        }
        for (IncomingTransaction txn : transactions) {
            segregated.get(txn.getTransactionType()).add(txn);
        }

        System.out.println("[SwiftAdapter] Segregation — "
                + "Credits: "    + segregated.get(TransactionType.CREDIT).size()
                + ", Debits: "   + segregated.get(TransactionType.DEBIT).size()
                + ", Reversals: " + segregated.get(TransactionType.REVERSAL).size()
                + ", Intrabank: " + segregated.get(TransactionType.INTRABANK).size());

        return segregated;
    }

    // =========================================================================
    // CSV HELPERS
    // =========================================================================

    /**
     * Splits a CSV line respecting quoted fields that may contain commas.
     * Example: SBI,"Deutsche Bank, Frankfurt",HDFC → 3 tokens
     */
    private String[] splitCsvLine(String line) {
        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                tokens.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        tokens.add(current.toString().trim()); // last token
        return tokens.toArray(new String[0]);
    }

    /**
     * Zips header names and row values into a Map.
     * If a row has fewer values than headers, missing values default to "".
     * If a row has more values than headers, extras are silently ignored.
     */
    private Map<String, String> zipHeadersAndValues(String[] headers, String[] values,
            int rowIdx) {
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i < headers.length; i++) {
            String value = (i < values.length) ? values[i] : "";
            map.put(headers[i].trim(), value);
        }
        if (values.length > headers.length) {
            System.out.println("[SwiftAdapter] Row " + rowIdx + " has "
                    + (values.length - headers.length) + " extra value(s) — ignored.");
        }
        return map;
    }

    private Map<String, String> stripExtraColumns(Map<String, String> rawMap) {
        Map<String, String> clean = new HashMap<>();
        for (Map.Entry<String, String> entry : rawMap.entrySet()) {
            if (ALLOWED_COLUMNS.contains(entry.getKey())) {
                clean.put(entry.getKey(), entry.getValue());
            }
        }
        return clean;
    }

    private void logExtraColumns(String[] headers, String filePath) {
        for (String header : headers) {
            if (!ALLOWED_COLUMNS.contains(header.trim())) {
                System.out.println("[SwiftAdapter] Extra column ignored: '"
                        + header.trim() + "' in " + filePath);
            }
        }
    }

    // =========================================================================
    // VALUE HELPERS
    // =========================================================================

    private String requireValue(Map<String, String> row, String key) throws AdapterException {
        String value = row.get(key);
        if (value == null || value.isBlank()) {
            throw new AdapterException(
                    "Missing required field '" + key + "' in SWIFT row", getSourceSystemType());
        }
        return value;
    }

    private long parseLong(Map<String, String> row, String key) throws AdapterException {
        try {
            return Long.parseLong(requireValue(row, key));
        } catch (NumberFormatException e) {
            throw new AdapterException(
                    "Invalid numeric value for '" + key + "': " + row.get(key), getSourceSystemType());
        }
    }

    private BigDecimal parseDecimal(Map<String, String> row, String key) throws AdapterException {
        try {
            return new BigDecimal(requireValue(row, key));
        } catch (NumberFormatException e) {
            throw new AdapterException(
                    "Invalid decimal value for '" + key + "': " + row.get(key), getSourceSystemType());
        }
    }

    private LocalDateTime parseDateTime(Map<String, String> row, String key) throws AdapterException {
        try {
            return LocalDateTime.parse(requireValue(row, key), DATE_TIME_FORMATTER);
        } catch (Exception e) {
            throw new AdapterException(
                    "Invalid dateTime for '" + key + "': " + row.get(key)
                    + " (expected yyyy-MM-dd'T'HH:mm:ss)", getSourceSystemType());
        }
    }

    private <E extends Enum<E>> E parseEnum(Map<String, String> row, String key,
            Class<E> enumClass) throws AdapterException {
        String value = requireValue(row, key);
        try {
            return Enum.valueOf(enumClass, value);
        } catch (IllegalArgumentException e) {
            throw new AdapterException(
                    "Invalid value for '" + key + "': '" + value
                    + "' is not a valid " + enumClass.getSimpleName(), getSourceSystemType());
        }
    }
}