package com.iispl.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.iispl.enums.ChannelType;
import com.iispl.enums.ProcessingStatus;
import com.iispl.enums.TransactionType;

public class ReversalTransaction extends IncomingTransaction {

	private long originalTransactionId;
	private String reversalType;
	public ReversalTransaction(long incomingTnxId, SourceSystem sourceSystem, long sourceSystemId,
			TransactionType transactionType, ChannelType channelType, String fromBankName, String toBankName,
			BigDecimal amount, ProcessingStatus processingStatus, LocalDateTime ingestionTimeStamp, String batchId,
			long originalTransactionId, String reversalType) {
		super(incomingTnxId, sourceSystem, sourceSystemId, transactionType, channelType, fromBankName, toBankName,
				amount, processingStatus, ingestionTimeStamp, batchId);
		this.originalTransactionId = originalTransactionId;
		this.reversalType = reversalType;
	}
	public long getOriginalTransactionId() {
		return originalTransactionId;
	}
	public void setOriginalTransactionId(long originalTransactionId) {
		this.originalTransactionId = originalTransactionId;
	}
	public String getReversalType() {
		return reversalType;
	}
	public void setReversalType(String reversalType) {
		this.reversalType = reversalType;
	}

	
}