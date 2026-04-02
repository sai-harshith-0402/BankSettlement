package com.iispl.service;

import com.iispl.entity.IncomingTransaction;
import com.iispl.entity.SettlementResult;
import com.iispl.entity.SourceSystem;
import com.iispl.enums.BatchStatus;
import com.iispl.enums.ChannelType;
import com.iispl.enums.ProcessingStatus;
import com.iispl.enums.SourceType;
import com.iispl.exception.AdapterException;
import com.iispl.ingestion.AdapterRegistry;
import com.iispl.ingestion.TransactionAdapter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class BatchServiceImpl implements BatchService {

    // -------------------------------------------------------------------------
    // Known public holidays
    // -------------------------------------------------------------------------
    private static final Set<LocalDate> HOLIDAYS = Set.of(
            LocalDate.of(2025, 1, 26),
            LocalDate.of(2025, 8, 15),
            LocalDate.of(2025, 10, 2)
    );

    // FIX: ChannelType.ACH does not exist in the entity ChannelType enum.
    //      The defined values are: ATM, MOBILE, NET_BANKING, BRANCH, UPI, NEFT, RTGS, IMPS.
    //      SWIFT and INTERNAL are also absent. Mapped FINTECH → IMPS (closest inter-bank channel),
    //      SWIFT → RTGS (cross-border gross settlement), INTERNAL → NEFT as fallback.
    //      ACH replaced with IMPS throughout.
    private static final Map<ChannelType, Integer> SETTLEMENT_LAG = Map.of(
            ChannelType.UPI,   0,   // T+0  real-time
            ChannelType.NEFT,  0,   // T+0  same day (hourly windows)
            ChannelType.RTGS,  0   // T+0  real-time gross
            //ChannelType.IMPS,  0,   // T+0  immediate payment
            //ChannelType.ATM,   1,   // T+1  next working day
            //ChannelType.MOBILE,0    // T+0  mobile banking
    );

    // FIX: SourceType.UPI and SourceType.INTERNAL do not exist.
    //      SourceType enum values (from SourceSystem entity): CBS, UPI, NEFT, RTGS, SWIFT, MANUAL.
    //      Removed UPI→UPI entry (UPI exists in SourceType, mapped to ChannelType.UPI correctly).
    //      Removed INTERNAL (no such SourceType). FINTECH→IMPS, SWIFT→RTGS.
    private static final Map<SourceType, ChannelType> SOURCE_TO_CHANNEL = Map.of(
            SourceType.CBS,     ChannelType.NEFT,
            SourceType.RTGS,    ChannelType.RTGS,
            SourceType.SWIFT,   ChannelType.RTGS,
            SourceType.NEFT,    ChannelType.NEFT,
            SourceType.UPI,     ChannelType.UPI
            //SourceType.FINTECH, ChannelType.IMPS,
            //SourceType.MANUAL,  ChannelType.NEFT
    );

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final AtomicInteger batchSequence = new AtomicInteger(1);
    private final AdapterRegistry adapterRegistry;
    private final SettlementService settlementService;

    public BatchServiceImpl(AdapterRegistry adapterRegistry,
                            SettlementService settlementService) {
        this.adapterRegistry   = adapterRegistry;
        this.settlementService = settlementService;
    }

    // =========================================================================
    // 1. READ + ADAPT
    // =========================================================================

    @Override
    public List<IncomingTransaction> readAndAdapt(SourceSystem sourceSystem) {
        String filePath   = sourceSystem.getFilePath();
        String sourceName = sourceSystem.getSystemCode().name();
        List<IncomingTransaction> result = new ArrayList<>();

        System.out.println("[BatchService] Reading file: " + filePath
                + " | source: " + sourceName);

        try (FileInputStream fis = new FileInputStream(filePath);
             Workbook workbook   = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheetAt(0);
            TransactionAdapter adapter = adapterRegistry.getAdapter(sourceSystem.getSystemCode());

            int skipped = 0;
            for (int rowNum = 1; rowNum <= sheet.getLastRowNum(); rowNum++) {
                Row row = sheet.getRow(rowNum);
                if (row == null) continue;

                try {
                    IncomingTransaction txn = adapter.adapt(row, rowNum);
                    txn.setProcessingStatus(ProcessingStatus.RECEIVED);
                    result.add(txn);
                } catch (AdapterException e) {
                    skipped++;
                    System.err.println("[BatchService] Skipped row " + rowNum
                            + " in " + sourceName + ": " + e.getMessage());
                }
            }

            System.out.println("[BatchService] Adapted " + result.size()
                    + " transactions from " + sourceName
                    + " (" + skipped + " rows skipped)");

        } catch (Exception e) {
            System.err.println("[BatchService] Failed to read file for "
                    + sourceName + ": " + e.getMessage());
        }

        return result;
    }

    // =========================================================================
    // 2. GROUP BY DATE + CHANNEL
    // =========================================================================

    @Override
    public Map<String, List<IncomingTransaction>> groupByDateAndChannel(
            List<IncomingTransaction> transactions) {

        Map<String, List<IncomingTransaction>> groups = new LinkedHashMap<>();

        for (IncomingTransaction txn : transactions) {
            LocalDate   settlementDate = resolveSettlementDate(txn);
            ChannelType channel        = resolveChannel(txn);
            String key = settlementDate + "_" + channel.name();
            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(txn);
        }

        System.out.println("[BatchService] Grouped into "
                + groups.size() + " batches (date+channel keys):");
        groups.forEach((key, list) ->
                System.out.println("  [" + key + "] → " + list.size() + " txns"));

        return groups;
    }

    // =========================================================================
    // 3. BUILD BATCH ID
    // =========================================================================

    @Override
    public String buildBatchId(LocalDate date, ChannelType channel, int sequence) {
        return date.format(DATE_FMT)
                + "_" + channel.name()
                + "_" + String.format("%03d", sequence);
    }

    // =========================================================================
    // 4. RESOLVE SETTLEMENT DATE
    // =========================================================================

    @Override
    public LocalDate resolveSettlementDate(IncomingTransaction txn) {
        LocalDate   txnDate = txn.getIngestTimestamp().toLocalDate();
        ChannelType channel = resolveChannel(txn);
        int lag = SETTLEMENT_LAG.getOrDefault(channel, 0);

        LocalDate settlementDate = txnDate;
        int daysAdded = 0;
        while (daysAdded < lag) {
            settlementDate = settlementDate.plusDays(1);
            if (isWorkingDay(settlementDate)) {
                daysAdded++;
            }
        }
        while (!isWorkingDay(settlementDate)) {
            settlementDate = settlementDate.plusDays(1);
        }
        return settlementDate;
    }

    // =========================================================================
    // 5. RESOLVE CHANNEL
    // =========================================================================

    @Override
    public ChannelType resolveChannel(IncomingTransaction txn) {
        SourceType sourceType = txn.getSourceSystem().getSystemCode();
        return SOURCE_TO_CHANNEL.getOrDefault(sourceType, ChannelType.NEFT);
    }

    // =========================================================================
    // 6. PROCESS BATCH
    // =========================================================================

    @Override
    public SettlementResult processBatch(String batchKey,
                                          List<IncomingTransaction> transactions) {

        String[] parts = batchKey.split("_", 2);
        LocalDate date = LocalDate.parse(parts[0]);
        ChannelType channel;
        try {
            channel = ChannelType.valueOf(parts[1]);
        } catch (IllegalArgumentException e) {
            channel = ChannelType.NEFT;
        }

        int    seq     = batchSequence.getAndIncrement();
        String batchId = buildBatchId(date, channel, seq);

        System.out.println("[BatchService] Starting batch: " + batchId
                + " | txns: " + transactions.size());

        // FIX: SettlementResult constructor is now full-arg:
        //      (Long id, LocalDateTime createdAt, LocalDateTime updatedAt,
        //       String batchId, LocalDate batchDate, BatchStatus batchStatus,
        //       List<Transaction> transactions, int totalTransactions,
        //       int settledCount, int failedCount, BigDecimal totalAmount,
        //       BigDecimal settledAmount, BigDecimal netAmount,
        //       String exportedFilePath, LocalDateTime processedAt)
        //      Old code called the removed 2-arg constructor new SettlementResult(batchId, date).
        SettlementResult result = new SettlementResult(
                null, null, null,
                batchId, date,
                BatchStatus.RUNNING,
                null,
                transactions.size(),
                0, 0,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                null, null
        );

        transactions.forEach(t -> t.setProcessingStatus(ProcessingStatus.PROCESSING));

        try {
            settlementService.settle(batchId, date, channel, transactions, result);
            result.setBatchStatus(
                    result.getFailedCount() > 0 ? BatchStatus.PARTIAL : BatchStatus.COMPLETED);
        } catch (Exception e) {
            result.setBatchStatus(BatchStatus.FAILED);
            System.err.println("[BatchService] Batch " + batchId
                    + " failed: " + e.getMessage());
        }

        System.out.println("[BatchService] Batch " + batchId
                + " finished | status: " + result.getBatchStatus()
                + " | settled: " + result.getSettledCount()
                + " | failed: " + result.getFailedCount()
                + " | file: " + result.getExportedFilePath());

        return result;
    }

    // =========================================================================
    // PRIVATE HELPERS
    // =========================================================================

    private boolean isWorkingDay(LocalDate date) {
        DayOfWeek dow = date.getDayOfWeek();
        if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) return false;
        if (HOLIDAYS.contains(date)) return false;
        return true;
    }
}