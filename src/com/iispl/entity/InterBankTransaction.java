package com.iispl.entity;

import com.iispl.enums.ChannelType;
import com.iispl.enums.TransactionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class InterBankTransaction extends Transaction {

	private final long nostroAccountId;

	public InterBankTransaction(SourceSystem sourceSystem, long sourceSystemId, ChannelType channel, Bank fromBank,
			Bank toBank, BigDecimal amount, LocalDateTime txnDate, TransactionStatus status, long fromBankId,
			long toBankId, long nostroAccountId) {
		super(sourceSystem, sourceSystemId, channel, fromBank, toBank, amount, txnDate, status, fromBankId, toBankId);
		this.nostroAccountId = nostroAccountId;
	}

	public Long getNostroAccountId() {
		return nostroAccountId;
	}
}