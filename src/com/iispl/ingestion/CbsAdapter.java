package com.iispl.ingestion;

import com.iispl.entity.CreditTransaction;
import com.iispl.entity.DebitTransaction;
import com.iispl.entity.IncomingTransaction;
import com.iispl.entity.ReversalTransaction;
import com.iispl.entity.SourceSystem;
import com.iispl.enums.ChannelType;
import com.iispl.enums.ProcessingStatus;
import com.iispl.enums.SourceType;
import com.iispl.enums.TransactionType;
import com.iispl.exception.AdapterException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * CbsAdapter — CBS (Core Banking System) ingestion adapter.
 *
 * Implements TransactionAdapter interface.
 *
 * Two responsibilities:
 *
 *  1. ingest(String filePath)
 *     Reads the CBS XML file, converts every <transaction> element into a
 *     Map<String, String> (tag name → text value), then delegates each map
 *     to adapt() to produce the final IncomingTransaction object.
 *
 *  2. adapt(Map<String, String> row)
 *     Converts one flat row-map into the correct IncomingTransaction subtype:
 *       CREDIT   → CreditTransaction
 *       DEBIT    → DebitTransaction
 *       REVERSAL → ReversalTransaction
 *       INTRABANK→ IncomingTransaction (base)
 *
 * Expected XML structure (resources/cbs_transactions.xml):
 * ─────────────────────────────────────────────────────────
 * <cbsTransactions>
 *   <transaction>
 *     <incomingTnxId>1001</incomingTnxId>
 *     <sourceSystemId>1</sourceSystemId>
 *     <transactionType>CREDIT</transactionType>
 *     <channelType>NEFT</channelType>
 *     <fromBankName>SBI</fromBankName>
 *     <toBankName>HDFC</toBankName>
 *     <amount>75000.00</amount>
 *     <processingStatus>RECEIVED</processingStatus>
 *     <ingestionTimeStamp>2025-04-07T09:00:00</ingestionTimeStamp>
 *     <batchId>BATCH-CBS-001</batchId>
 *     <creditAccountId>2001</creditAccountId>   <!-- CREDIT only -->
 *     <debitAccountId>3001</debitAccountId>     <!-- DEBIT only  -->
 *     <originalTransactionId>999</originalTransactionId> <!-- REVERSAL only -->
 *     <reversalType>FULL</reversalType>         <!-- REVERSAL only -->
 *   </transaction>
 * </cbsTransactions>
 */
public class CbsAdapter implements TransactionAdapter {

    // =========================================================================
    // CONSTANTS
    // =========================================================================

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    // XML tag names — must match cbs_transactions.xml exactly
    private static final String TAG_INCOMING_TNX_ID        = "incomingTnxId";
    private static final String TAG_SOURCE_SYSTEM_ID       = "sourceSystemId";
    private static final String TAG_TRANSACTION_TYPE       = "transactionType";
    private static final String TAG_CHANNEL_TYPE           = "channelType";
    private static final String TAG_FROM_BANK_NAME         = "fromBankName";
    private static final String TAG_TO_BANK_NAME           = "toBankName";
    private static final String TAG_AMOUNT                 = "amount";
    private static final String TAG_PROCESSING_STATUS      = "processingStatus";
    private static final String TAG_INGESTION_TIME_STAMP   = "ingestionTimeStamp";
    private static final String TAG_BATCH_ID               = "batchId";
    private static final String TAG_CREDIT_ACCOUNT_ID      = "creditAccountId";
    private static final String TAG_DEBIT_ACCOUNT_ID       = "debitAccountId";
    private static final String TAG_ORIGINAL_TRANSACTION_ID = "originalTransactionId";
    private static final String TAG_REVERSAL_TYPE          = "reversalType";

    // =========================================================================
    // TransactionAdapter — getSourceSystemType()
    // =========================================================================

    /**
     * Used by AdapterRegistry to register and look up this adapter.
     * Must match SourceType.CBS.name() exactly.
     */
    @Override
    public String getSourceSystemType() {
        return SourceType.CBS.name(); // "CBS"
    }

    // =========================================================================
    // INGEST — reads the XML file, produces List<IncomingTransaction>
    // =========================================================================

    /**
     * Reads the CBS XML file at filePath, converts each <transaction> element
     * into a Map<String, String>, and delegates to adapt() for entity creation.
     *
     * Called by IngestionWorker / PipelineOrchestrator.
     *
     * @param filePath  absolute or relative path to cbs_transactions.xml
     * @return          list of parsed IncomingTransaction objects
     * @throws AdapterException if the file is missing, malformed, or any row fails
     */
    public List<IncomingTransaction> ingest(String filePath) throws AdapterException {

        List<IncomingTransaction> results = new ArrayList<>();

        try {
            // 1. Parse XML into DOM Document (XXE-safe)
            Document doc = parseXmlFile(filePath);
            doc.getDocumentElement().normalize();

            // 2. Select all <transaction> elements
            NodeList txNodes = doc.getElementsByTagName("transaction");

            if (txNodes.getLength() == 0) {
                System.out.println("[CbsAdapter] No <transaction> elements found in: " + filePath);
                return results;
            }

            System.out.println("[CbsAdapter] Parsing " + txNodes.getLength()
                    + " transactions from: " + filePath);

            // 3. Convert each element to Map, then adapt() to entity
            for (int i = 0; i < txNodes.getLength(); i++) {
                Element txElement = (Element) txNodes.item(i);
                try {
                    Map<String, String> row = elementToMap(txElement);
                    IncomingTransaction txn = adapt(row);
                    results.add(txn);
                } catch (AdapterException ae) {
                    // Log bad rows and continue — one bad row must not stop the batch
                    System.err.println("[CbsAdapter] Skipping row " + i
                            + " — " + ae.getMessage());
                }
            }

            System.out.println("[CbsAdapter] Successfully ingested "
                    + results.size() + " transactions from CBS XML.");

        } catch (AdapterException ae) {
            throw ae; // re-throw already-wrapped exceptions
        } catch (Exception e) {
            throw new AdapterException(
                    "CBS XML ingestion failed for file [" + filePath + "]: " + e.getMessage()
            );
        }

        return results;
    }

    // =========================================================================
    // TransactionAdapter — adapt(Map<String, String> row)
    // =========================================================================

    /**
     * Converts one flat row-map (produced from a single <transaction> element)
     * into the correct IncomingTransaction subtype.
     *
     * Map keys are XML tag names; values are their trimmed text content.
     *
     * @param row  key-value pairs extracted from one <transaction> element
     * @return     typed IncomingTransaction (Credit / Debit / Reversal / base)
     * @throws AdapterException if any required field is missing or invalid
     */
    @Override
    public IncomingTransaction adapt(Map<String, String> row) throws AdapterException {

        try {
            // ── Required common fields ────────────────────────────────────────
            long incomingTnxId = parseLong(row, TAG_INCOMING_TNX_ID);
            long sourceSystemId = parseLong(row, TAG_SOURCE_SYSTEM_ID);

            TransactionType transactionType =
                    parseEnum(row, TAG_TRANSACTION_TYPE, TransactionType.class);
            ChannelType channelType =
                    parseEnum(row, TAG_CHANNEL_TYPE, ChannelType.class);

            String fromBankName = requireValue(row, TAG_FROM_BANK_NAME);
            String toBankName   = requireValue(row, TAG_TO_BANK_NAME);

            BigDecimal amount = parseDecimal(row, TAG_AMOUNT);

            ProcessingStatus processingStatus =
                    parseEnum(row, TAG_PROCESSING_STATUS, ProcessingStatus.class);

            LocalDateTime ingestionTimeStamp =
                    parseDateTime(row, TAG_INGESTION_TIME_STAMP);

            String batchId = row.getOrDefault(TAG_BATCH_ID, null);

            // ── Build SourceSystem shell (filePath not in XML row; set by caller) ─
            com.iispl.entity.SourceSystem sourceSystem =
                    new com.iispl.entity.SourceSystem(sourceSystemId, SourceType.CBS, "");

            // ── Route to correct subtype ──────────────────────────────────────
            switch (transactionType) {

                case CREDIT: {
                    long creditAccountId = parseLong(row, TAG_CREDIT_ACCOUNT_ID);
                    return new CreditTransaction(
                            incomingTnxId, sourceSystem, sourceSystemId,
                            transactionType, channelType,
                            fromBankName, toBankName,
                            amount, processingStatus, ingestionTimeStamp, batchId,
                            creditAccountId
                    );
                }

                case DEBIT: {
                    long debitAccountId = parseLong(row, TAG_DEBIT_ACCOUNT_ID);
                    return new DebitTransaction(
                            incomingTnxId, sourceSystem, sourceSystemId,
                            transactionType, channelType,
                            fromBankName, toBankName,
                            amount, processingStatus, ingestionTimeStamp, batchId,
                            debitAccountId
                    );
                }

                case REVERSAL: {
                    long originalTransactionId = parseLong(row, TAG_ORIGINAL_TRANSACTION_ID);
                    String reversalType        = requireValue(row, TAG_REVERSAL_TYPE);
                    return new ReversalTransaction(
                            incomingTnxId, sourceSystem, sourceSystemId,
                            transactionType, channelType,
                            fromBankName, toBankName,
                            amount, processingStatus, ingestionTimeStamp, batchId,
                            originalTransactionId, reversalType
                    );
                }

                case INTRABANK:
                default: {
                    return new IncomingTransaction(
                            incomingTnxId, sourceSystem, sourceSystemId,
                            transactionType, channelType,
                            fromBankName, toBankName,
                            amount, processingStatus, ingestionTimeStamp, batchId
                    );
                }
            }

        } catch (AdapterException ae) {
            throw ae; // already wrapped — re-throw as-is
        } catch (Exception e) {
            throw new AdapterException(
                    "CBS adapt() failed for row " + row + ": " + e.getMessage()
            );
        }
    }

    // =========================================================================
    // XML PARSING HELPERS
    // =========================================================================

    /**
     * Parse an XML file into a DOM Document with XXE protection enabled.
     */
    private Document parseXmlFile(String filePath) throws Exception {
        File file = new File(filePath);
        if (!file.exists()) {
            throw new AdapterException("CBS XML file not found: " + filePath);
        }
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        // XXE security: disallow DOCTYPE declarations
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(file);
    }

    /**
     * Convert a single <transaction> Element into a flat Map<String, String>.
     * Each direct child tag name becomes a key; its trimmed text becomes the value.
     *
     * Example:
     *   <amount>75000.00</amount>  →  "amount" → "75000.00"
     */
    private Map<String, String> elementToMap(Element element) {
        Map<String, String> row = new HashMap<>();
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i).getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
                Element child = (Element) children.item(i);
                row.put(child.getTagName(), child.getTextContent().trim());
            }
        }
        return row;
    }

    // =========================================================================
    // ROW-MAP VALUE HELPERS
    // =========================================================================

    /** Returns the value for key, throws AdapterException if absent or blank. */
    private String requireValue(Map<String, String> row, String key) throws AdapterException {
        String value = row.get(key);
        if (value == null || value.isBlank()) {
            throw new AdapterException(
                    "Missing required field <" + key + "> in CBS transaction row"
            );
        }
        return value;
    }

    /** Parse a required long field. */
    private long parseLong(Map<String, String> row, String key) throws AdapterException {
        try {
            return Long.parseLong(requireValue(row, key));
        } catch (NumberFormatException e) {
            throw new AdapterException(
                    "Invalid numeric value for <" + key + ">: " + row.get(key)
            );
        }
    }

    /** Parse a required BigDecimal field. */
    private BigDecimal parseDecimal(Map<String, String> row, String key) throws AdapterException {
        try {
            return new BigDecimal(requireValue(row, key));
        } catch (NumberFormatException e) {
            throw new AdapterException(
                    "Invalid decimal value for <" + key + ">: " + row.get(key)
            );
        }
    }

    /** Parse a required LocalDateTime field (format: yyyy-MM-dd'T'HH:mm:ss). */
    private LocalDateTime parseDateTime(Map<String, String> row, String key) throws AdapterException {
        try {
            return LocalDateTime.parse(requireValue(row, key), DATE_TIME_FORMATTER);
        } catch (Exception e) {
            throw new AdapterException(
                    "Invalid dateTime value for <" + key + ">: " + row.get(key)
                            + " (expected yyyy-MM-dd'T'HH:mm:ss)"
            );
        }
    }

    /** Parse a required Enum field. */
    private <E extends Enum<E>> E parseEnum(Map<String, String> row, String key,
                                             Class<E> enumClass) throws AdapterException {
        String value = requireValue(row, key);
        try {
            return Enum.valueOf(enumClass, value);
        } catch (IllegalArgumentException e) {
            throw new AdapterException(
                    "Invalid value for <" + key + ">: '" + value
                            + "' is not a valid " + enumClass.getSimpleName()
            );
        }
    }
}