package com.iispl.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.iispl.enums.ChannelType;
import com.iispl.enums.ProcessingStatus;
import com.iispl.enums.TransactionType;

public class InterBankTransaction extends IncomingTransaction {

	private long nostroAccountId;
	private long vostroAccountId;
	public InterBankTransaction(long incomingTnxId, SourceSystem sourceSystem, long sourceSystemId,
			TransactionType transactionType, ChannelType channelType, String fromBankName, String toBankName,
			BigDecimal amount, ProcessingStatus processingStatus, LocalDateTime ingestionTimeStamp, String batchId,
			long nostroAccountId, long vostroAccountId) {
		super(incomingTnxId, sourceSystem, sourceSystemId, transactionType, channelType, fromBankName, toBankName,
				amount, processingStatus, ingestionTimeStamp, batchId);
		this.nostroAccountId = nostroAccountId;
		this.vostroAccountId = vostroAccountId;
	}
	public long getNostroAccountId() {
		return nostroAccountId;
	}
	public void setNostroAccountId(long nostroAccountId) {
		this.nostroAccountId = nostroAccountId;
	}
	public long getVostroAccountId() {
		return vostroAccountId;
	}
	public void setVostroAccountId(long vostroAccountId) {
		this.vostroAccountId = vostroAccountId;
	}
}