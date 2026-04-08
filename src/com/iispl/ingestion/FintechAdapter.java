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

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * FintechAdapter — Fintech ingestion adapter.
 * File format : Excel (.xlsx)
 * SourceType  : FINTECH
 *
 * Expected .xlsx columns (extras are silently ignored):
 * ──────────────────────────────────────────────────────
 *   incomingTnxId | sourceSystemId | transactionType | channelType |
 *   fromBankName  | toBankName     | amount          | processingStatus |
 *   ingestionTimeStamp | batchId   |
 *   creditAccountId    (CREDIT rows)
 *   debitAccountId     (DEBIT rows)
 *   originalTransactionId + reversalType  (REVERSAL rows)
 *   nostroAccountId + vostroAccountId     (INTRABANK rows)
 */
public class FintechAdapter implements TransactionAdapter {

    // =========================================================================
    // CONSTANTS
    // =========================================================================

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

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
        return SourceType.FINTECH.name();
    }

    // =========================================================================
    // INGEST
    // =========================================================================

    public List<IncomingTransaction> ingest(String filePath) throws AdapterException {
        List<IncomingTransaction> results = new ArrayList<>();

        File file = new File(filePath);
        if (!file.exists()) {
            throw new AdapterException(
                    "Fintech .xlsx file not found: " + filePath, getSourceSystemType());
        }

        try (FileInputStream fis = new FileInputStream(file);
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                throw new AdapterException(
                        "Fintech .xlsx has no sheets: " + filePath, getSourceSystemType());
            }

            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                throw new AdapterException(
                        "Fintech .xlsx has no header row: " + filePath, getSourceSystemType());
            }

            Map<Integer, String> headerMap = buildHeaderMap(headerRow);
            logExtraColumns(headerMap, filePath);

            int totalRows = sheet.getLastRowNum();
            System.out.println("[FintechAdapter] Parsing " + totalRows
                    + " data rows from: " + filePath);

            for (int rowIdx = 1; rowIdx <= totalRows; rowIdx++) {
                Row row = sheet.getRow(rowIdx);
                if (row == null || isRowBlank(row)) {
                    System.out.println("[FintechAdapter] Skipping blank row at index " + rowIdx);
                    continue;
                }
                try {
                    Map<String, String> rawMap   = rowToMap(row, headerMap);
                    Map<String, String> cleanMap = stripExtraColumns(rawMap);
                    results.add(adapt(cleanMap));
                } catch (AdapterException ae) {
                    System.err.println("[FintechAdapter] Skipping row " + rowIdx
                            + " — " + ae.getMessage());
                }
            }

            System.out.println("[FintechAdapter] Successfully ingested "
                    + results.size() + " transactions.");

        } catch (AdapterException ae) {
            throw ae;
        } catch (Exception e) {
            throw new AdapterException(
                    "Fintech .xlsx ingestion failed for [" + filePath + "]: " + e.getMessage(),
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

            SourceSystem sourceSystem = new SourceSystem(sourceSystemId, SourceType.FINTECH, "");

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
                    "Fintech adapt() failed: " + e.getMessage(), getSourceSystemType(), e);
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

        System.out.println("[FintechAdapter] Segregation — "
                + "Credits: "    + segregated.get(TransactionType.CREDIT).size()
                + ", Debits: "   + segregated.get(TransactionType.DEBIT).size()
                + ", Reversals: " + segregated.get(TransactionType.REVERSAL).size()
                + ", Intrabank: " + segregated.get(TransactionType.INTRABANK).size());

        return segregated;
    }

    // =========================================================================
    // EXCEL HELPERS
    // =========================================================================

    private Map<Integer, String> buildHeaderMap(Row headerRow) {
        Map<Integer, String> map = new HashMap<>();
        for (Cell cell : headerRow) {
            String header = getCellValueAsString(cell).trim();
            if (!header.isEmpty()) {
                map.put(cell.getColumnIndex(), header);
            }
        }
        return map;
    }

    private Map<String, String> rowToMap(Row row, Map<Integer, String> headerMap) {
        Map<String, String> map = new HashMap<>();
        for (Map.Entry<Integer, String> entry : headerMap.entrySet()) {
            Cell cell = row.getCell(entry.getKey(), Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
            map.put(entry.getValue(), cell == null ? "" : getCellValueAsString(cell).trim());
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

    private void logExtraColumns(Map<Integer, String> headerMap, String filePath) {
        for (String header : headerMap.values()) {
            if (!ALLOWED_COLUMNS.contains(header)) {
                System.out.println("[FintechAdapter] Extra column ignored: '"
                        + header + "' in " + filePath);
            }
        }
    }

    private boolean isRowBlank(Row row) {
        for (Cell cell : row) {
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                if (!getCellValueAsString(cell).trim().isEmpty()) return false;
            }
        }
        return true;
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING:  return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getLocalDateTimeCellValue().format(DATE_TIME_FORMATTER);
                }
                double d = cell.getNumericCellValue();
                if (d == Math.floor(d) && !Double.isInfinite(d)) {
                    return String.valueOf((long) d);
                }
                return BigDecimal.valueOf(d).toPlainString();
            case BOOLEAN: return String.valueOf(cell.getBooleanCellValue());
            case FORMULA: return cell.getCellFormula();
            default:      return "";
        }
    }

    // =========================================================================
    // VALUE HELPERS
    // =========================================================================

    private String requireValue(Map<String, String> row, String key) throws AdapterException {
        String value = row.get(key);
        if (value == null || value.isBlank()) {
            throw new AdapterException(
                    "Missing required field '" + key + "' in Fintech row", getSourceSystemType());
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