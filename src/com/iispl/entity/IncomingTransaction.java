package com.iispl.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.iispl.enums.ChannelType;
import com.iispl.enums.ProcessingStatus;
import com.iispl.enums.TransactionType;

public class IncomingTransaction {
	private long incomingTnxId;
	private SourceSystem sourceSystem;
	private long sourceSystemId;
	private TransactionType transactionType;
	private ChannelType channelType;
	private String fromBankName;
	private String toBankName;
	private BigDecimal amount;
	private ProcessingStatus processingStatus;
	private LocalDateTime ingestionTimeStamp;
	private String batchId;
	public IncomingTransaction(long incomingTnxId, SourceSystem sourceSystem, long sourceSystemId,
			TransactionType transactionType, ChannelType channelType, String fromBankName, String toBankName, BigDecimal amount, ProcessingStatus processingStatus,
			LocalDateTime ingestionTimeStamp, String batchId) {
		this.incomingTnxId = incomingTnxId;
		this.sourceSystem = sourceSystem;
		this.sourceSystemId = sourceSystemId;
		this.transactionType = transactionType;
		this.channelType = channelType;
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
	public ChannelType getChannelType() {
		return channelType;
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