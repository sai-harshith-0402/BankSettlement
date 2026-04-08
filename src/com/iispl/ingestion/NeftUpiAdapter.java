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
 * NeftUpiAdapter — NEFT/UPI ingestion adapter.
 * File format : XML
 * SourceType  : NEFTUPI
 *
 * Expected XML structure (resources/neftupi_transactions.xml):
 * ─────────────────────────────────────────────────────────────
 * <neftUpiTransactions>
 *   <transaction>
 *     <incomingTnxId>2001</incomingTnxId>
 *     <sourceSystemId>2</sourceSystemId>
 *     <transactionType>CREDIT</transactionType>
 *     <channelType>UPI</channelType>
 *     <fromBankName>SBI</fromBankName>
 *     <toBankName>ICICI</toBankName>
 *     <amount>5000.00</amount>
 *     <processingStatus>RECEIVED</processingStatus>
 *     <ingestionTimeStamp>2025-04-07T10:00:00</ingestionTimeStamp>
 *     <batchId>BATCH-NEFTUPI-001</batchId>
 *     <creditAccountId>2001</creditAccountId>      <!-- CREDIT only   -->
 *     <debitAccountId>3001</debitAccountId>         <!-- DEBIT only    -->
 *     <originalTransactionId>888</originalTransactionId> <!-- REVERSAL only -->
 *     <reversalType>PARTIAL</reversalType>          <!-- REVERSAL only -->
 *     <nostroAccountId>4001</nostroAccountId>       <!-- INTRABANK only -->
 *     <vostroAccountId>5001</vostroAccountId>       <!-- INTRABANK only -->
 *   </transaction>
 * </neftUpiTransactions>
 */
public class NeftUpiAdapter implements TransactionAdapter {

    // =========================================================================
    // CONSTANTS
    // =========================================================================

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private static final String TAG_INCOMING_TNX_ID          = "incomingTnxId";
    private static final String TAG_SOURCE_SYSTEM_ID         = "sourceSystemId";
    private static final String TAG_TRANSACTION_TYPE         = "transactionType";
    private static final String TAG_CHANNEL_TYPE             = "channelType";
    private static final String TAG_FROM_BANK_NAME           = "fromBankName";
    private static final String TAG_TO_BANK_NAME             = "toBankName";
    private static final String TAG_AMOUNT                   = "amount";
    private static final String TAG_PROCESSING_STATUS        = "processingStatus";
    private static final String TAG_INGESTION_TIME_STAMP     = "ingestionTimeStamp";
    private static final String TAG_BATCH_ID                 = "batchId";
    private static final String TAG_CREDIT_ACCOUNT_ID        = "creditAccountId";
    private static final String TAG_DEBIT_ACCOUNT_ID         = "debitAccountId";
    private static final String TAG_ORIGINAL_TRANSACTION_ID  = "originalTransactionId";
    private static final String TAG_REVERSAL_TYPE            = "reversalType";
    private static final String TAG_NOSTRO_ACCOUNT_ID        = "nostroAccountId";
    private static final String TAG_VOSTRO_ACCOUNT_ID        = "vostroAccountId";

    // =========================================================================
    // getSourceSystemType
    // =========================================================================

    @Override
    public String getSourceSystemType() {
        return SourceType.NEFTUPI.name();
    }

    // =========================================================================
    // INGEST
    // =========================================================================

    public List<IncomingTransaction> ingest(String filePath) throws AdapterException {
        List<IncomingTransaction> results = new ArrayList<>();

        try {
            Document doc = parseXmlFile(filePath);
            doc.getDocumentElement().normalize();

            NodeList txNodes = doc.getElementsByTagName("transaction");

            if (txNodes.getLength() == 0) {
                System.out.println("[NeftUpiAdapter] No <transaction> elements found in: " + filePath);
                return results;
            }

            System.out.println("[NeftUpiAdapter] Parsing " + txNodes.getLength()
                    + " transactions from: " + filePath);

            for (int i = 0; i < txNodes.getLength(); i++) {
                Element txElement = (Element) txNodes.item(i);
                try {
                    Map<String, String> row = elementToMap(txElement);
                    results.add(adapt(row));
                } catch (AdapterException ae) {
                    System.err.println("[NeftUpiAdapter] Skipping row " + i + " — " + ae.getMessage());
                }
            }

            System.out.println("[NeftUpiAdapter] Successfully ingested "
                    + results.size() + " transactions.");

        } catch (AdapterException ae) {
            throw ae;
        } catch (Exception e) {
            throw new AdapterException(
                    "NEFTUPI XML ingestion failed for [" + filePath + "]: " + e.getMessage(),
                    getSourceSystemType(), e);
        }

        return results;
    }

    // =========================================================================
    // ADAPT
    // =========================================================================

    @Override
    public IncomingTransaction adapt(Map<String, String> row) throws AdapterException {
        try {
            long             incomingTnxId      = parseLong(row,    TAG_INCOMING_TNX_ID);
            long             sourceSystemId     = parseLong(row,    TAG_SOURCE_SYSTEM_ID);
            TransactionType  transactionType    = parseEnum(row,    TAG_TRANSACTION_TYPE,  TransactionType.class);
            ChannelType      channelType        = parseEnum(row,    TAG_CHANNEL_TYPE,       ChannelType.class);
            String           fromBankName       = requireValue(row, TAG_FROM_BANK_NAME);
            String           toBankName         = requireValue(row, TAG_TO_BANK_NAME);
            BigDecimal       amount             = parseDecimal(row, TAG_AMOUNT);
            ProcessingStatus processingStatus   = parseEnum(row,    TAG_PROCESSING_STATUS, ProcessingStatus.class);
            LocalDateTime    ingestionTimeStamp = parseDateTime(row,TAG_INGESTION_TIME_STAMP);
            String           batchId            = row.getOrDefault(TAG_BATCH_ID, null);

            SourceSystem sourceSystem = new SourceSystem(sourceSystemId, SourceType.NEFTUPI, "");

            switch (transactionType) {

                case CREDIT: {
                    long creditAccountId = parseLong(row, TAG_CREDIT_ACCOUNT_ID);
                    return new CreditTransaction(
                            incomingTnxId, sourceSystem, sourceSystemId,
                            transactionType, channelType, fromBankName, toBankName,
                            amount, processingStatus, ingestionTimeStamp, batchId,
                            creditAccountId);
                }

                case DEBIT: {
                    long debitAccountId = parseLong(row, TAG_DEBIT_ACCOUNT_ID);
                    return new DebitTransaction(
                            incomingTnxId, sourceSystem, sourceSystemId,
                            transactionType, channelType, fromBankName, toBankName,
                            amount, processingStatus, ingestionTimeStamp, batchId,
                            debitAccountId);
                }

                case REVERSAL: {
                    long   originalTransactionId = parseLong(row,    TAG_ORIGINAL_TRANSACTION_ID);
                    String reversalType          = requireValue(row, TAG_REVERSAL_TYPE);
                    return new ReversalTransaction(
                            incomingTnxId, sourceSystem, sourceSystemId,
                            transactionType, channelType, fromBankName, toBankName,
                            amount, processingStatus, ingestionTimeStamp, batchId,
                            originalTransactionId, reversalType);
                }

                case INTRABANK: {
                    long nostroAccountId = parseLong(row, TAG_NOSTRO_ACCOUNT_ID);
                    long vostroAccountId = parseLong(row, TAG_VOSTRO_ACCOUNT_ID);
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
                    "NeftUpi adapt() failed: " + e.getMessage(), getSourceSystemType(), e);
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

        System.out.println("[NeftUpiAdapter] Segregation — "
                + "Credits: "    + segregated.get(TransactionType.CREDIT).size()
                + ", Debits: "   + segregated.get(TransactionType.DEBIT).size()
                + ", Reversals: " + segregated.get(TransactionType.REVERSAL).size()
                + ", Intrabank: " + segregated.get(TransactionType.INTRABANK).size());

        return segregated;
    }

    // =========================================================================
    // XML HELPERS
    // =========================================================================

    private Document parseXmlFile(String filePath) throws Exception {
        File file = new File(filePath);
        if (!file.exists()) {
            throw new AdapterException("NEFTUPI XML file not found: " + filePath);
        }
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(file);
    }

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
    // VALUE HELPERS
    // =========================================================================

    private String requireValue(Map<String, String> row, String key) throws AdapterException {
        String value = row.get(key);
        if (value == null || value.isBlank()) {
            throw new AdapterException(
                    "Missing required field <" + key + "> in NEFTUPI row", getSourceSystemType());
        }
        return value;
    }

    private long parseLong(Map<String, String> row, String key) throws AdapterException {
        try {
            return Long.parseLong(requireValue(row, key));
        } catch (NumberFormatException e) {
            throw new AdapterException(
                    "Invalid numeric value for <" + key + ">: " + row.get(key), getSourceSystemType());
        }
    }

    private BigDecimal parseDecimal(Map<String, String> row, String key) throws AdapterException {
        try {
            return new BigDecimal(requireValue(row, key));
        } catch (NumberFormatException e) {
            throw new AdapterException(
                    "Invalid decimal value for <" + key + ">: " + row.get(key), getSourceSystemType());
        }
    }

    private LocalDateTime parseDateTime(Map<String, String> row, String key) throws AdapterException {
        try {
            return LocalDateTime.parse(requireValue(row, key), DATE_TIME_FORMATTER);
        } catch (Exception e) {
            throw new AdapterException(
                    "Invalid dateTime for <" + key + ">: " + row.get(key)
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
                    "Invalid value for <" + key + ">: '" + value
                    + "' is not a valid " + enumClass.getSimpleName(), getSourceSystemType());
        }
    }
}