package com.iispl.entity;

import com.iispl.enums.ChannelType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class InterBankTransaction extends Transaction {

	private final Account nostroAccount;

	public InterBankTransaction(SourceSystem sourceSystem, ChannelType channel, Bank fromBank, Bank toBank,
			BigDecimal amount, LocalDateTime txnDate, Account nostroAccount) {
		super(sourceSystem, channel, fromBank, toBank, amount, txnDate);
		this.nostroAccount = nostroAccount;
	}

	public Account getNostroAccount() {
		return nostroAccount;
	}
}