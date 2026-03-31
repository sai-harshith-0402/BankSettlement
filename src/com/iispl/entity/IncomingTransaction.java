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
	private SourceSystem sourceSystem;

	public IncomingTransaction(Long sourceSystemId, TransactionType txnType, BigDecimal amount,
			LocalDateTime ingestTimestamp, ProcessingStatus processingStatus, SourceSystem sourceSystem) {
		this.sourceSystemId = sourceSystemId;
		this.txnType = txnType;
		this.amount = amount;
		this.ingestTimestamp = ingestTimestamp;
		this.processingStatus = processingStatus;
		this.sourceSystem = sourceSystem;
	}

	public ProcessingStatus getProcessingStatus() {
		return processingStatus;
	}

	public void setProcessingStatus(ProcessingStatus processingStatus) {
		this.processingStatus = processingStatus;
	}

	public SourceSystem getSourceSystem() {
		return sourceSystem;
	}

	public void setSourceSystem(SourceSystem sourceSystem) {
		this.sourceSystem = sourceSystem;
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

}