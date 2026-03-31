package com.iispl.entity;

import com.iispl.enums.ChannelType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class DebitTransaction extends Transaction {

	private final Account debitAccount;

	public DebitTransaction(SourceSystem sourceSystem, ChannelType channel, Bank fromBank, Bank toBank,
			BigDecimal amount, LocalDateTime txnDate, Account debitAccount) {
		super(sourceSystem, channel, fromBank, toBank, amount, txnDate);
		this.debitAccount = debitAccount;
	}

	public Account getDebitAccount() {
		return debitAccount;
	}
}