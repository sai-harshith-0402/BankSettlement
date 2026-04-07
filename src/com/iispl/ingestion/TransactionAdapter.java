package com.iispl.ingestion;

import com.iispl.entity.IncomingTransaction;
import java.util.List;
import java.util.Map;

public interface TransactionAdapter {

    /**
     * Converts a single raw row (column name w string value)
     * into a validated IncomingTransaction.
     *
     * @param row  one row from the .xlsx file as a Map
     * @return     parsed and validated IncomingTransaction
     * @throws com.iispl.exception.AdapterException if parsing or validation fails
     */
    IncomingTransaction adapt(Map<String, String> row);

    /**
     * Returns the SourceSystemType this adapter handles.
     * Used by AdapterRegistry for lookup.
     */
    String getSourceSystemType();
}