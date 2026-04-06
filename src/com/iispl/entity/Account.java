package com.iispl.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class Account extends BaseEntity {
	private String customerId;
	private String accountNumber;
	private String accountType;
	private String bankId;
	private BigDecimal amount;
	private String status;
	private List<IncomingTransaction> transactions;

	public Account(long id, LocalDateTime createdAt, LocalDateTime updatedAt, String customerId, String accountNumber,
			String accountType, String bankId, BigDecimal amount, String status,
			List<IncomingTransaction> transactions) {
		super(id, createdAt, updatedAt);
		this.customerId = customerId;
		this.accountNumber = accountNumber;
		this.accountType = accountType;
		this.bankId = bankId;
		this.amount = amount;
		this.status = status;
		this.transactions = transactions;
	}

	public String getCustomerId() {
		return customerId;
	}

	public void setCustomerId(String customerId) {
		this.customerId = customerId;
	}

	public String getAccountNumber() {
		return accountNumber;
	}

	public void setAccountNumber(String accountNumber) {
		accountNumber = accountNumber;
	}

	public String getAccountType() {
		return accountType;
	}

	public void setAccountType(String accountType) {
		this.accountType = accountType;
	}

	public String getBankId() {
		return bankId;
	}

	public void setBankId(String bankId) {
		this.bankId = bankId;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public void setAmount(BigDecimal amount) {
		this.amount = amount;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public List<IncomingTransaction> getTransactions() {
		return transactions;
	}

	public void setTransactions(List<IncomingTransaction> transactions) {
		this.transactions = transactions;
	}

}