package com.iispl.ingestion;

import com.iispl.entity.IncomingTransaction;
import com.iispl.enums.SourceType;
import com.iispl.exception.AdapterException;
import org.apache.poi.ss.usermodel.Row;

public interface TransactionAdapter {

    /**
     * Parses a single .xlsx Row into a canonical IncomingTransaction.
     * Throws AdapterException if the row is malformed — caller stops the batch.
     */
    IncomingTransaction adapt(Row row, int rowNumber) throws AdapterException;

    SourceType getSourceType();
}