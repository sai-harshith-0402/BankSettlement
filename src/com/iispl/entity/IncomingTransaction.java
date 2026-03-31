package com.iispl.entity;

import com.iispl.enums.ProcessingStatus;
import com.iispl.enums.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class IncomingTransaction extends BaseEntity {

	private final Long sourceSystemId;
	private final TransactionType txnType;
	private final BigDecimal amount;
	private final LocalDateTime ingestTimestamp;
	private ProcessingStatus processingStatus;

	public IncomingTransaction(Long sourceSystemId, TransactionType txnType, BigDecimal amount) {
		this.sourceSystemId = sourceSystemId;
		this.txnType = txnType;
		this.amount = amount;
		this.ingestTimestamp = LocalDateTime.now();
		this.processingStatus = ProcessingStatus.RECEIVED;
	}

	public Long getSourceSystemId() {
		return sourceSystemId;
	}

	public TransactionType getTxnType() {
		return txnType;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public LocalDateTime getIngestTimestamp() {
		return ingestTimestamp;
	}

	public ProcessingStatus getProcessingStatus() {
		return processingStatus;
	}

	public void setProcessingStatus(ProcessingStatus processingStatus) {
		this.processingStatus = processingStatus;
	}
}