package com.iispl.entity;

import com.iispl.enums.ChannelType;
import com.iispl.enums.TransactionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class DebitTransaction extends Transaction {

	private final long debitAccountId;

	public DebitTransaction(SourceSystem sourceSystem, long sourceSystemId, ChannelType channel, Bank fromBank,
			Bank toBank, BigDecimal amount, LocalDateTime txnDate, TransactionStatus status, long fromBankId,
			long toBankId, long debitAccountId) {
		super(sourceSystem, sourceSystemId, channel, fromBank, toBank, amount, txnDate, status, fromBankId, toBankId);
		this.debitAccountId = debitAccountId;
	}

	public long getDebitAccountId() {
		return debitAccountId;
	}

}