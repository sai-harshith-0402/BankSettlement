package com.iispl.entity;

import com.iispl.enums.ChannelType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class ReversalTransaction extends Transaction {

	private final Transaction originalTransaction;
	private final String reversalReason;

	public ReversalTransaction(SourceSystem sourceSystem, ChannelType channel, Bank fromBank, Bank toBank,
			BigDecimal amount, LocalDateTime txnDate, Transaction originalTransaction, String reversalReason) {
		super(sourceSystem, channel, fromBank, toBank, amount, txnDate);
		this.originalTransaction = originalTransaction;
		this.reversalReason = reversalReason;
	}

	public Transaction getOriginalTransaction() {
		return originalTransaction;
	}

	public String getReversalReason() {
		return reversalReason;
	}
}