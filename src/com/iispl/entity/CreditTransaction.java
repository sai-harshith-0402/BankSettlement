package com.iispl.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.iispl.enums.ProcessingStatus;
import com.iispl.enums.TransactionType;

public class CreditTransaction extends IncomingTransaction {

	private final long creditAccountId;

	public CreditTransaction(long incomingTnxId, SourceSystem sourceSystem, long sourceSystemId,
			TransactionType transactionType, long fromAccountId, long toAccountId, String fromBankName,
			String toBankName, BigDecimal amount, ProcessingStatus processingStatus, LocalDateTime ingestionTimeStamp,
			String batchId, long creditAccountId) {
		super(incomingTnxId, sourceSystem, sourceSystemId, transactionType, fromAccountId, toAccountId, fromBankName,
				toBankName, amount, processingStatus, ingestionTimeStamp, batchId);
		this.creditAccountId = creditAccountId;
	}

	public Long getCreditAccountId() {
		return creditAccountId;
	}
}