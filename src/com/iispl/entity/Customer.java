package com.iispl.entity;

import java.time.LocalDateTime;
import java.util.List;

public class Customer extends BaseEntity {
	private String firstName;
	private String lastName;
	private String emailId;
	private LocalDateTime onBoardingDate;
	private String bankId;
	private boolean isActive;
	private List<Account> accounts;

	public Customer(long id, LocalDateTime createdAt, LocalDateTime updatedAt, String firstName, String lastName,
			String emailId, LocalDateTime onBoardingDate, String bankId, boolean isActive, List<Account> accounts) {
		super(id, createdAt, updatedAt);
		this.firstName = firstName;
		this.lastName = lastName;
		this.emailId = emailId;
		this.onBoardingDate = onBoardingDate;
		this.bankId = bankId;
		this.isActive = isActive;
		this.accounts = accounts;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String getEmailId() {
		return emailId;
	}

	public void setEmailId(String emailId) {
		this.emailId = emailId;
	}

	public LocalDateTime getOnBoardingDate() {
		return onBoardingDate;
	}

	public void setOnBoardingDate(LocalDateTime onBoardingDate) {
		this.onBoardingDate = onBoardingDate;
	}

	public String getBankId() {
		return bankId;
	}

	public void setBankId(String bankId) {
		this.bankId = bankId;
	}

	public boolean isActive() {
		return isActive;
	}

	public void setActive(boolean isActive) {
		this.isActive = isActive;
	}

	public List<Account> getAccounts() {
		return accounts;
	}

	public void setAccounts(List<Account> accounts) {
		this.accounts = accounts;
	}

}