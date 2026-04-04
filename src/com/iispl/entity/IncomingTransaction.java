package com.iispl.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.iispl.enums.ProcessingStatus;
import com.iispl.enums.TransactionType;

public final class IncomingTransaction {
	private long incomingTnxId;
	private SourceSystem sourceSystem;
	private long sourceSystemId;
	private TransactionType transactionType;
	private long debitAccountId;
	private long creditAccountId;
	private BigDecimal amount;
	private ProcessingStatus processingStatus;
	private LocalDateTime ingestionTimeStamp;

	public IncomingTransaction(long incomingTnxId, SourceSystem sourceSystem, long sourceSystemId,
			TransactionType transactionType, long debitAccountId, long creditAccountId, BigDecimal amount,
			ProcessingStatus processingStatus, LocalDateTime ingestionTimeStamp) {
		this.incomingTnxId = incomingTnxId;
		this.sourceSystem = sourceSystem;
		this.sourceSystemId = sourceSystemId;
		this.transactionType = transactionType;
		this.debitAccountId = debitAccountId;
		this.creditAccountId = creditAccountId;
		this.amount = amount;
		this.processingStatus = processingStatus;
		this.ingestionTimeStamp = ingestionTimeStamp;
	}

	public long getIncomingTnxId() {
		return incomingTnxId;
	}

	public SourceSystem getSourceSystem() {
		return sourceSystem;
	}

	public long getSourceSystemId() {
		return sourceSystemId;
	}

	public TransactionType getTransactionType() {
		return transactionType;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public ProcessingStatus getProcessingStatus() {
		return processingStatus;
	}

	public LocalDateTime getIngestionTimeStamp() {
		return ingestionTimeStamp;
	}

	public long getDebitAccountId() {
		return debitAccountId;
	}

	public long getCreditAccountId() {
		return creditAccountId;
	}

}