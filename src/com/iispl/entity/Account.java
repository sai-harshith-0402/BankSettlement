package com.iispl.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.iispl.enums.AccountType;

public class Account extends BaseEntity {
	private long customerId;
	private String accountNumber;
	private AccountType accountType;
	private long bankId;
	private BigDecimal balance;
	private String status;
	private List<IncomingTransaction> transactions;

	public Account(long id, LocalDateTime createdAt, LocalDateTime updatedAt, long customerId, String accountNumber,
			AccountType accountType, long bankId, BigDecimal balance, String status) {
		super(id, createdAt, updatedAt);
		this.customerId = customerId;
		this.accountNumber = accountNumber;
		this.accountType = accountType;
		this.bankId = bankId;
		this.balance = balance;
		this.status = status;

	}

	public long getCustomerId() {
		return customerId;
	}

	public void setCustomerId(long customerId) {
		this.customerId = customerId;
	}

	public String getAccountNumber() {
		return accountNumber;
	}

	public void setAccountNumber(String accountNumber) {
		this.accountNumber = accountNumber;
	}

	public AccountType getAccountType() {
		return accountType;
	}

	public void setAccountType(AccountType accountType) {
		this.accountType = accountType;
	}

	public long getBankId() {
		return bankId;
	}

	public void setBankId(long bankId) {
		this.bankId = bankId;
	}

	public BigDecimal getBalance() {
		return balance;
	}

	public void setBalance(BigDecimal balance) {
		this.balance = balance;
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