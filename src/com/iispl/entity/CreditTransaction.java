package com.iispl.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.iispl.enums.ChannelType;
import com.iispl.enums.ProcessingStatus;
import com.iispl.enums.TransactionType;

public class CreditTransaction extends IncomingTransaction {

	private long creditAccountId;

	public CreditTransaction(long incomingTnxId, SourceSystem sourceSystem, long sourceSystemId,
			TransactionType transactionType, ChannelType channelType, String fromBankName, String toBankName,
			BigDecimal amount, ProcessingStatus processingStatus, LocalDateTime ingestionTimeStamp, String batchId,
			long creditAccountId) {
		super(incomingTnxId, sourceSystem, sourceSystemId, transactionType, channelType, fromBankName, toBankName,
				amount, processingStatus, ingestionTimeStamp, batchId);
		this.creditAccountId = creditAccountId;
	}

	public long getCreditAccountId() {
		return creditAccountId;
	}

	
}