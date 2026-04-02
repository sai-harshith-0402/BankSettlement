package com.iispl.ingestion;

import com.iispl.entity.IncomingTransaction;
import com.iispl.entity.SourceSystem;
import com.iispl.enums.ProcessingStatus;
import com.iispl.enums.SourceType;
import com.iispl.enums.TransactionType;
import com.iispl.exception.AdapterException;
import org.apache.poi.ss.usermodel.Row;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * CBS flat-file column layout:
 * Col 0 — txnType       (String  e.g. CREDIT)
 * Col 1 — amount        (Numeric)
 * Col 2 — fromBankCode  (String)
 * Col 3 — toBankCode    (String)
 * Col 4 — accountNumber (String)
 */
public class CbsAdapter implements TransactionAdapter {

    private static final String SOURCE = SourceType.CBS.name();

    private final SourceSystem sourceSystem;

    public CbsAdapter(SourceSystem sourceSystem) {
        this.sourceSystem = sourceSystem;
    }

    @Override
    public IncomingTransaction adapt(Row row, int rowNumber) throws AdapterException {
        try {
            TransactionType txnType      = AdapterUtil.readTransactionType(row.getCell(0), SOURCE, rowNumber);
            BigDecimal      amount       = AdapterUtil.readAmount(row.getCell(1), SOURCE, rowNumber);

            AdapterUtil.requireCell(row.getCell(2), SOURCE, "fromBankCode",  rowNumber);
            AdapterUtil.requireCell(row.getCell(3), SOURCE, "toBankCode",    rowNumber);
            AdapterUtil.requireCell(row.getCell(4), SOURCE, "accountNumber", rowNumber);

            // These are parsed but not yet stored on IncomingTransaction —
            // they will be used downstream during the mapping phase.
            String fromBankCode  = AdapterUtil.readString(row.getCell(2));
            String toBankCode    = AdapterUtil.readString(row.getCell(3));
            String accountNumber = AdapterUtil.readString(row.getCell(4));

            // FIX: IncomingTransaction constructor is now:
            //      (Long id, LocalDateTime createdAt, LocalDateTime updatedAt,
            //       Long sourceSystemId, TransactionType txnType, BigDecimal amount,
            //       LocalDateTime ingestTimestamp, ProcessingStatus processingStatus,
            //       SourceSystem sourceSystem, String batchId)
            //      Old code called the removed 6-arg constructor.
            //      id/createdAt/updatedAt are null at ingestion time (not yet persisted).
            //      batchId is null at ingestion time (assigned during grouping).
            return new IncomingTransaction(
                    null,                       // id — assigned by DB on save
                    null,                       // createdAt — set by DB
                    null,                       // updatedAt — set by DB
                    sourceSystem.getId(),       // sourceSystemId (FK)
                    txnType,
                    amount,
                    LocalDateTime.now(),        // ingestTimestamp
                    ProcessingStatus.RECEIVED,
                    sourceSystem,               // in-memory object reference
                    null                        // batchId — assigned during grouping
            );

        } catch (AdapterException e) {
            throw e;
        } catch (Exception e) {
            throw new AdapterException(SOURCE, rowNumber, e.getMessage(), e);
        }
    }

    @Override
    public SourceType getSourceType() {
        return SourceType.CBS;
    }
}