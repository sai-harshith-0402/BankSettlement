package com.iispl.entity;

import com.iispl.enums.ChannelType;
import com.iispl.enums.ProcessingStatus;
import com.iispl.enums.TransactionStatus;
import com.iispl.enums.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class DebitTransaction extends IncomingTransaction {

	private final long debitAccountId;

	public DebitTransaction(long incomingTnxId, SourceSystem sourceSystem, long sourceSystemId,
			TransactionType transactionType, long fromAccountId, long toAccountId, String fromBankName,
			String toBankName, BigDecimal amount, ProcessingStatus processingStatus, LocalDateTime ingestionTimeStamp,
			String batchId, long debitAccountId) {
		super(incomingTnxId, sourceSystem, sourceSystemId, transactionType, fromAccountId, toAccountId, fromBankName,
				toBankName, amount, processingStatus, ingestionTimeStamp, batchId);
		this.debitAccountId = debitAccountId;
	}

	public long getDebitAccountId() {
		return debitAccountId;
	}

}