package com.iispl.entity;

import com.iispl.enums.AccountType;

import java.math.BigDecimal;

public final class Account extends BaseEntity {

	private final String accountNumber;
	private final AccountType accountType;
	private final Long customerId;
	private final Long bankId;
	private BigDecimal balance;
	private String status;

	public Account(String accountNumber, AccountType accountType, Long customerId, Long bankId, BigDecimal balance) {
		this.accountNumber = accountNumber;
		this.accountType = accountType;
		this.customerId = customerId;
		this.bankId = bankId;
		this.balance = balance;
		this.status = "ACTIVE";
	}

	public String getAccountNumber() {
		return accountNumber;
	}

	public AccountType getAccountType() {
		return accountType;
	}

	public Long getCustomerId() {
		return customerId;
	}

	public Long getBankId() {
		return bankId;
	}

	public BigDecimal getBalance() {
		return balance;
	}

	public String getStatus() {
		return status;
	}

	public void setBalance(BigDecimal balance) {
		this.balance = balance;
	}

	public void setStatus(String status) {
		this.status = status;
	}
}