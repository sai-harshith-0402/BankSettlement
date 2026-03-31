package com.iispl.entity;

import com.iispl.enums.ChannelType;
import com.iispl.enums.TransactionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public abstract class Transaction extends BaseEntity {

	private SourceSystem sourceSystem;
	private ChannelType channel;
	private Bank fromBank;
	private Bank toBank;
	private BigDecimal amount;
	private LocalDateTime txnDate;
	private TransactionStatus status;

	public Transaction(SourceSystem sourceSystem, ChannelType channel, Bank fromBank, Bank toBank, BigDecimal amount,
			LocalDateTime txnDate) {
		super();
		this.sourceSystem = sourceSystem;
		this.channel = channel;
		this.fromBank = fromBank;
		this.toBank = toBank;
		this.amount = amount;
		this.txnDate = txnDate;
		this.status = TransactionStatus.INITIATED;
	}

	public SourceSystem getSourceSystem() {
		return sourceSystem;
	}

	public ChannelType getChannel() {
		return channel;
	}

	public Bank getFromBank() {
		return fromBank;
	}

	public Bank getToBank() {
		return toBank;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public LocalDateTime getTxnDate() {
		return txnDate;
	}

	public TransactionStatus getStatus() {
		return status;
	}

	public void setStatus(TransactionStatus status) {
		this.status = status;
	}
}