package com.iispl.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.iispl.enums.ProcessingStatus;
import com.iispl.enums.TransactionType;

public class InterBankTransaction extends IncomingTransaction {

	private long nostroAccountId;
	private String clearingHouse;

	public InterBankTransaction(long incomingTnxId, SourceSystem sourceSystem, long sourceSystemId,
			TransactionType transactionType, long fromAccountId, long toAccountId, String fromBankName,
			String toBankName, BigDecimal amount, ProcessingStatus processingStatus, LocalDateTime ingestionTimeStamp,
			String batchId, long nostroAccountId, String clearingHouse) {
		super(incomingTnxId, sourceSystem, sourceSystemId, transactionType, fromAccountId, toAccountId, fromBankName,
				toBankName, amount, processingStatus, ingestionTimeStamp, batchId);
		this.nostroAccountId = nostroAccountId;
		this.clearingHouse = clearingHouse;
	}

	public long getNostroAccountId() {
		return nostroAccountId;
	}

	public String getClearingHouse() {
		return clearingHouse;
	}

	@Override
	public String toString() {
		return "InterBankTransaction{" + "nostroAccountId=" + nostroAccountId + ", clearingHouse='" + clearingHouse
				+ '\'' + '}';
	}
}