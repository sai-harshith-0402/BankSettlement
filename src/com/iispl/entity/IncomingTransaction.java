package com.iispl.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.iispl.enums.ProcessingStatus;
import com.iispl.enums.TransactionType;

public class IncomingTransaction {
	private long incomingTnxId;
	private SourceSystem sourceSystem;
	private long sourceSystemId;
	private TransactionType transactionType;
	private long fromAccountId;
	private long toAccountId;
	private String fromBankName;
	private String toBankName;
	private BigDecimal amount;
	private ProcessingStatus processingStatus;
	private LocalDateTime ingestionTimeStamp;
	private String batchId;

	public IncomingTransaction(long incomingTnxId, SourceSystem sourceSystem, long sourceSystemId,
			TransactionType transactionType, long fromAccountId, long toAccountId, String fromBankName,
			String toBankName, BigDecimal amount, ProcessingStatus processingStatus, LocalDateTime ingestionTimeStamp,
			String batchId) {
		super();
		this.incomingTnxId = incomingTnxId;
		this.sourceSystem = sourceSystem;
		this.sourceSystemId = sourceSystemId;
		this.transactionType = transactionType;
		this.fromAccountId = fromAccountId;
		this.toAccountId = toAccountId;
		this.fromBankName = fromBankName;
		this.toBankName = toBankName;
		this.amount = amount;
		this.processingStatus = processingStatus;
		this.ingestionTimeStamp = ingestionTimeStamp;
		this.batchId = batchId;
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

	public long getFromAccountId() {
		return fromAccountId;
	}

	public long getToAccountId() {
		return toAccountId;
	}

	public String getFromBankName() {
		return fromBankName;
	}

	public String getToBankName() {
		return toBankName;
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

	public String getBatchId() {
		return batchId;
	}

}