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

            String fromBankCode  = AdapterUtil.readString(row.getCell(2));
            String toBankCode    = AdapterUtil.readString(row.getCell(3));
            String accountNumber = AdapterUtil.readString(row.getCell(4));

            return new IncomingTransaction(
                    sourceSystem.getId(),
                    txnType,
                    amount,
                    LocalDateTime.now(),
                    ProcessingStatus.RECEIVED,
                    sourceSystem
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