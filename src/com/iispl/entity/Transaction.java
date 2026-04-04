package com.iispl.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.iispl.enums.TransactionStatus;

public final class Transaction{
	private long tnxId;
	private long debitAccountId;
	private long creditAccountId;
	private BigDecimal amount;
	private LocalDateTime tnxTimeStamp;
	private TransactionStatus status;
	public Transaction(long tnxId, long debitAccountId, long creditAccountId, BigDecimal amount,
			LocalDateTime tnxTimeStamp, TransactionStatus status) {
		this.tnxId = tnxId;
		this.debitAccountId = debitAccountId;
		this.creditAccountId = creditAccountId;
		this.amount = amount;
		this.tnxTimeStamp = tnxTimeStamp;
		this.status = status;
	}
	public long getTnxId() {
		return tnxId;
	}

	public long getDebitAccountId() {
		return debitAccountId;
	}

	public long getCreditAccountId() {
		return creditAccountId;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public LocalDateTime getTnxTimeStamp() {
		return tnxTimeStamp;
	}

	public TransactionStatus getStatus() {
		return status;
	}

}