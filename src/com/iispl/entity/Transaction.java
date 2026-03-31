package com.iispl.entity;

import com.iispl.enums.ChannelType;
import com.iispl.enums.TransactionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public abstract class Transaction extends BaseEntity {

	private SourceSystem sourceSystem;
	private long sourceSystemId;
	private ChannelType channel;
	private Bank fromBank;
	private Bank toBank;
	private BigDecimal amount;
	private LocalDateTime txnDate;
	private TransactionStatus status;
	private long fromBankId;
	private long toBankId;

	public Transaction(SourceSystem sourceSystem, long sourceSystemId, ChannelType channel, Bank fromBank, Bank toBank,
			BigDecimal amount, LocalDateTime txnDate, TransactionStatus status, long fromBankId, long toBankId) {
		this.sourceSystem = sourceSystem;
		this.sourceSystemId = sourceSystemId;
		this.channel = channel;
		this.fromBank = fromBank;
		this.toBank = toBank;
		this.amount = amount;
		this.txnDate = txnDate;
		this.status = status;
		this.fromBankId = fromBankId;
		this.toBankId = toBankId;
	}

	public SourceSystem getSourceSystem() {
		return sourceSystem;
	}

	public void setSourceSystem(SourceSystem sourceSystem) {
		this.sourceSystem = sourceSystem;
	}

	public long getSourceSystemId() {
		return sourceSystemId;
	}

	public void setSourceSystemId(long sourceSystemId) {
		this.sourceSystemId = sourceSystemId;
	}

	public ChannelType getChannel() {
		return channel;
	}

	public void setChannel(ChannelType channel) {
		this.channel = channel;
	}

	public Bank getFromBank() {
		return fromBank;
	}

	public void setFromBank(Bank fromBank) {
		this.fromBank = fromBank;
	}

	public Bank getToBank() {
		return toBank;
	}

	public void setToBank(Bank toBank) {
		this.toBank = toBank;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public void setAmount(BigDecimal amount) {
		this.amount = amount;
	}

	public LocalDateTime getTxnDate() {
		return txnDate;
	}

	public void setTxnDate(LocalDateTime txnDate) {
		this.txnDate = txnDate;
	}

	public TransactionStatus getStatus() {
		return status;
	}

	public void setStatus(TransactionStatus status) {
		this.status = status;
	}

	public long getFromBankId() {
		return fromBankId;
	}

	public void setFromBankId(long fromBankId) {
		this.fromBankId = fromBankId;
	}

	public long getToBankId() {
		return toBankId;
	}

	public void setToBankId(long toBankId) {
		this.toBankId = toBankId;
	}

}