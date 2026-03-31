package com.iispl.entity;

public final class Bank {

	private final String bankCode;
	private final String bankName;
	private final String ifscCode;
	private final boolean isActive;

	public Bank(String bankCode, String bankName, String ifscCode, boolean isActive) {
		this.bankCode = bankCode;
		this.bankName = bankName;
		this.ifscCode = ifscCode;
		this.isActive = isActive;
	}

	public String getBankCode() {
		return bankCode;
	}

	public String getBankName() {
		return bankName;
	}

	public String getIfscCode() {
		return ifscCode;
	}

	public boolean isActive() {
		return isActive;
	}
}