package com.iispl.service;

import com.iispl.entity.Batch;
import com.iispl.entity.CreditTransaction;
import com.iispl.entity.DebitTransaction;
import com.iispl.entity.IncomingTransaction;
import com.iispl.entity.InterBankTransaction;
import com.iispl.entity.ReversalTransaction;
import com.iispl.entity.SourceSystem;
import com.iispl.enums.BatchStatus;
import com.iispl.enums.ChannelType;
import com.iispl.enums.SourceType;
import com.iispl.exception.AdapterException;
import com.iispl.ingestion.AdapterRegistry;
import com.iispl.ingestion.TransactionAdapter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class BatchServiceImpl implements BatchService {

    private static final Logger logger = Logger.getLogger(BatchServiceImpl.class.getName());

    private final AdapterRegistry adapterRegistry;

    // Holds all batches created during this run — threading layer will consume these
    private final List<Batch> allBatches = new ArrayList<>();

    public BatchServiceImpl(AdapterRegistry adapterRegistry) {
        this.adapterRegistry = adapterRegistry;
    }

    // =========================================================================
    // CREATE BATCHES
    // One Batch per ChannelType per date found in the ingested transactions
    // BatchId format: SOURCETYPE-CHANNEL-YYYY-MM-DD
    // =========================================================================

    @Override
    public List<Batch> createBatches(SourceSystem sourceSystem) throws AdapterException {

        SourceType sourceType = sourceSystem.getSourceType();
        String filePath       = sourceSystem.getFilePath();

        logger.info("[BatchService] Starting ingestion | SourceType: " + sourceType
                + " | FilePath: " + filePath);

        // ── 1. Get the correct adapter ─────────────────────────────────────────
        TransactionAdapter adapter = adapterRegistry.getAdapter(sourceType);

        // ── 2. Ingest raw transactions from the file ───────────────────────────
        List<IncomingTransaction> rawTransactions = adapter.ingest(filePath);
        logger.info("[BatchService] Ingested " + rawTransactions.size()
                + " raw transactions from " + sourceType);

        // ── 3. Validate each transaction ───────────────────────────────────────
        List<IncomingTransaction> validTransactions = validate(rawTransactions);
        logger.info("[BatchService] Valid transactions: " + validTransactions.size()
                + " | Skipped (invalid): " + (rawTransactions.size() - validTransactions.size()));

        if (validTransactions.isEmpty()) {
            logger.warning("[BatchService] No valid transactions found for "
                    + sourceType + " — no batches created.");
            return new ArrayList<>();
        }

        // ── 4. Group valid transactions by ChannelType ─────────────────────────
        Map<ChannelType, List<IncomingTransaction>> grouped = groupByChannel(validTransactions);
        logger.info("[BatchService] Grouped into " + grouped.size() + " channel(s): "
                + grouped.keySet());

        // ── 5. Create one Batch per channel ────────────────────────────────────
        LocalDate today = LocalDate.now();
        List<Batch> batches = new ArrayList<>();

        for (Map.Entry<ChannelType, List<IncomingTransaction>> entry : grouped.entrySet()) {
            ChannelType channel      = entry.getKey();
            List<IncomingTransaction> txns = entry.getValue();

            String batchId = buildBatchId(sourceType, channel, today);

            BigDecimal totalAmount = txns.stream()
                    .map(IncomingTransaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            Batch batch = new Batch(
                    batchId,
                    today,
                    BatchStatus.SCHEDULED,
                    txns.size(),
                    totalAmount,
                    txns
            );

            batches.add(batch);
            allBatches.add(batch);

            logger.info("[BatchService] Batch created | BatchId: " + batchId
                    + " | Transactions: " + txns.size()
                    + " | TotalAmount: " + totalAmount);
        }

        logger.info("[BatchService] Created " + batches.size()
                + " batch(es) for source: " + sourceType);

        return batches;
    }

    // =========================================================================
    // GET ALL BATCHES
    // =========================================================================

    @Override
    public List<Batch> getAllBatches() {
        return allBatches;
    }

    // =========================================================================
    // VALIDATE
    // Rules:
    //   - No null fields (checked per subtype)
    //   - amount > 0
    //   - subtype-specific account ID must be present (> 0)
    // =========================================================================

    private List<IncomingTransaction> validate(List<IncomingTransaction> transactions) {
        List<IncomingTransaction> valid = new ArrayList<>();

        for (IncomingTransaction txn : transactions) {
            try {
                validateTransaction(txn);
                valid.add(txn);
            } catch (IllegalArgumentException e) {
                logger.warning("[BatchService] Invalid transaction [id="
                        + txn.getIncomingTnxId() + "] — " + e.getMessage() + " — skipped.");
            }
        }

        return valid;
    }

    private void validateTransaction(IncomingTransaction txn) {
        // ── Common fields ──────────────────────────────────────────────────────
        if (txn.getTransactionType() == null) {
            throw new IllegalArgumentException("transactionType is null");
        }
        if (txn.getChannelType() == null) {
            throw new IllegalArgumentException("channelType is null");
        }
        if (txn.getFromBankName() == null || txn.getFromBankName().isBlank()) {
            throw new IllegalArgumentException("fromBankName is null/blank");
        }
        if (txn.getToBankName() == null || txn.getToBankName().isBlank()) {
            throw new IllegalArgumentException("toBankName is null/blank");
        }
        if (txn.getAmount() == null || txn.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("amount is null or <= 0: " + txn.getAmount());
        }
        if (txn.getProcessingStatus() == null) {
            throw new IllegalArgumentException("processingStatus is null");
        }
        if (txn.getIngestionTimeStamp() == null) {
            throw new IllegalArgumentException("ingestionTimeStamp is null");
        }
        if (txn.getSourceSystem() == null) {
            throw new IllegalArgumentException("sourceSystem is null");
        }

        // ── Subtype-specific account ID ────────────────────────────────────────
        if (txn instanceof CreditTransaction) {
            CreditTransaction ct = (CreditTransaction) txn;
            if (ct.getCreditAccountId() <= 0) {
                throw new IllegalArgumentException("creditAccountId is invalid: "
                        + ct.getCreditAccountId());
            }
        } else if (txn instanceof DebitTransaction) {
            DebitTransaction dt = (DebitTransaction) txn;
            if (dt.getDebitAccountId() <= 0) {
                throw new IllegalArgumentException("debitAccountId is invalid: "
                        + dt.getDebitAccountId());
            }
        } else if (txn instanceof ReversalTransaction) {
            ReversalTransaction rt = (ReversalTransaction) txn;
            if (rt.getOriginalTransactionId() <= 0) {
                throw new IllegalArgumentException("originalTransactionId is invalid: "
                        + rt.getOriginalTransactionId());
            }
            if (rt.getReversalType() == null || rt.getReversalType().isBlank()) {
                throw new IllegalArgumentException("reversalType is null/blank");
            }
        } else if (txn instanceof InterBankTransaction) {
            InterBankTransaction it = (InterBankTransaction) txn;
            if (it.getNostroAccountId() <= 0) {
                throw new IllegalArgumentException("nostroAccountId is invalid: "
                        + it.getNostroAccountId());
            }
            if (it.getVostroAccountId() <= 0) {
                throw new IllegalArgumentException("vostroAccountId is invalid: "
                        + it.getVostroAccountId());
            }
        }
    }

    // =========================================================================
    // GROUP BY CHANNEL
    // =========================================================================

    private Map<ChannelType, List<IncomingTransaction>> groupByChannel(
            List<IncomingTransaction> transactions) {

        Map<ChannelType, List<IncomingTransaction>> grouped = new HashMap<>();

        for (IncomingTransaction txn : transactions) {
            ChannelType channel = txn.getChannelType();
            grouped.computeIfAbsent(channel, k -> new ArrayList<>()).add(txn);
        }

        return grouped;
    }

    // =========================================================================
    // BUILD BATCH ID
    // Format: SOURCETYPE-CHANNEL-YYYY-MM-DD
    // Example: NEFTUPI-UPI-2025-04-07
    // =========================================================================

    private String buildBatchId(SourceType sourceType, ChannelType channel, LocalDate date) {
        return sourceType.name() + "-" + channel.name() + "-" + date.toString();
    }
}