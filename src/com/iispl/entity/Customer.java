package com.iispl.entity;
import java.time.LocalDate;

public final class Customer extends BaseEntity {

	private final String firstName;
	private final String lastName;
	private final String email;
	private final LocalDate onboardingDate;

	public Customer(String firstName, String lastName, String email, LocalDate onboardingDate) {
		super();
		this.firstName = firstName;
		this.lastName = lastName;
		this.email = email;
		this.onboardingDate = onboardingDate;
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
}