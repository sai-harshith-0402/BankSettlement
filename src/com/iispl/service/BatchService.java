package com.iispl.service;

import com.iispl.entity.IncomingTransaction;
import com.iispl.entity.SettlementResult;
import com.iispl.entity.SourceSystem;
import com.iispl.enums.ChannelType;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface BatchService {

    /**
     * Reads the source file and adapts every row into an IncomingTransaction.
     * Called by IngestionWorker (producer thread) for each active SourceSystem.
     */
    List<IncomingTransaction> readAndAdapt(SourceSystem sourceSystem);

    /**
     * Groups a flat list of incoming transactions into batches keyed by
     * (settlementDate + channel).  Returns a map whose key is a composite
     * string "YYYY-MM-DD_CHANNEL" and whose value is the sub-list for that key.
     */
    Map<String, List<IncomingTransaction>> groupByDateAndChannel(
            List<IncomingTransaction> transactions);

    /**
     * Builds a batchId string from date + channel, e.g. "20250401_NEFT_001".
     */
    String buildBatchId(LocalDate date, ChannelType channel, int sequence);

    /**
     * Returns the settlement date for a transaction.
     * Applies channel-specific T+N rules and skips weekends / holidays.
     */
    LocalDate resolveSettlementDate(IncomingTransaction txn);

    /**
     * Determines the ChannelType for an IncomingTransaction by inspecting
     * its SourceSystem's SourceType.
     */
    ChannelType resolveChannel(IncomingTransaction txn);

    /**
     * Processes a single date+channel batch end-to-end:
     * validates → maps → settles → writes result file.
     * Called by SettlementProcessor (consumer thread).
     */
    SettlementResult processBatch(String batchKey,
                                  List<IncomingTransaction> transactions);
}