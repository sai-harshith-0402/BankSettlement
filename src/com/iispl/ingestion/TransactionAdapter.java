package com.iispl.ingestion;

import com.iispl.entity.IncomingTransaction;
import com.iispl.enums.TransactionType;

import java.util.List;
import java.util.Map;

public interface TransactionAdapter {

    /**
     * Converts one raw row (column/tag name → raw string value)
     * into a validated IncomingTransaction subtype.
     *
     * @param row  one row from the source file as a Map
     * @return     parsed and validated IncomingTransaction
     * @throws com.iispl.exception.AdapterException if parsing or validation fails
     */
    IncomingTransaction adapt(Map<String, String> row) throws com.iispl.exception.AdapterException;

    /**
     * Returns the SourceType name this adapter handles.
     * Used by AdapterRegistry for lookup.
     */
    String getSourceSystemType();

    /**
     * Segregates a mixed list of IncomingTransactions by TransactionType.
     * Every adapter must implement this — returns Map<TransactionType, List<IncomingTransaction>>.
     * Keys are pre-populated for all TransactionType values so callers never get NPE.
     */
    Map<TransactionType, List<IncomingTransaction>> segregate(List<IncomingTransaction> transactions);
    
    public List<IncomingTransaction> ingest(String filePath);
}