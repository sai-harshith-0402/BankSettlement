package com.iispl.entity;

import com.iispl.enums.ChannelType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class CreditTransaction extends Transaction {

	private final Account creditAccount;

	public CreditTransaction(SourceSystem sourceSystem, ChannelType channel, Bank fromBank, Bank toBank,
			BigDecimal amount, LocalDateTime txnDate, Account creditAccount) {
		super(sourceSystem, channel, fromBank, toBank, amount, txnDate);
		this.creditAccount = creditAccount;
	}

	public Account getCreditAccount() {
		return creditAccount;
	}
}