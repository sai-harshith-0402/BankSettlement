package com.iispl.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.iispl.enums.ProcessingStatus;
import com.iispl.enums.TransactionType;

public class ReversalTransaction extends IncomingTransaction {

	private long originalTransactionId;
	private String reversalReason;
	private String reversalType;

	public ReversalTransaction(long incomingTnxId, SourceSystem sourceSystem, long sourceSystemId,
			TransactionType transactionType, long fromAccountId, long toAccountId, String fromBankName,
			String toBankName, BigDecimal amount, ProcessingStatus processingStatus, LocalDateTime ingestionTimeStamp,
			String batchId, long originalTransactionId, String reversalReason, String reversalType) {
		super(incomingTnxId, sourceSystem, sourceSystemId, transactionType, fromAccountId, toAccountId, fromBankName,
				toBankName, amount, processingStatus, ingestionTimeStamp, batchId);
		this.originalTransactionId = originalTransactionId;
		this.reversalReason = reversalReason;
		this.reversalType = reversalType;
	}

	public long getOriginalTransactionId() {
		return originalTransactionId;
	}

	public String getReversalReason() {
		return reversalReason;
	}

	public String getReversalType() {
		return reversalType;
	}

	@Override
	public String toString() {
		return "ReversalTransaction{" + "originalTransactionId=" + originalTransactionId + ", reversalReason='"
				+ reversalReason + '\'' + ", reversalType='" + reversalType + '\'' + '}';
	}
}