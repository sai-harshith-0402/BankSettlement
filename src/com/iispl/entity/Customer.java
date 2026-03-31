package com.iispl.entity;

import java.time.LocalDate;
import java.util.List;

public final class Customer extends BaseEntity {

	private final String firstName;
	private final String lastName;
	private final String email;
	private final LocalDate onboardingDate;
	private final List<Account> accountList;

	public Customer(String firstName, String lastName, String email, LocalDate onboardingDate,
			List<Account> accountList) {
		this.firstName = firstName;
		this.lastName = lastName;
		this.email = email;
		this.onboardingDate = onboardingDate;
		this.accountList = accountList;
	}

	public String getFirstName() {
		return firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public String getEmail() {
		return email;
	}

	public LocalDate getOnboardingDate() {
		return onboardingDate;
	}

	public List<Account> getAccountList() {
		return accountList;
	}

}