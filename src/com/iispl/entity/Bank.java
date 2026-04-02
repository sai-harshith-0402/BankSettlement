package com.iispl.entity;

import java.time.LocalDateTime;

public final class Bank extends BaseEntity {

	private final String bankCode;
	private final String bankName;
	private final String ifscCode;
	private final boolean isActive;

	public Bank(Long id, LocalDateTime createdAt, LocalDateTime updatedAt, String bankCode, String bankName,
			String ifscCode, boolean isActive) {
		super(id, createdAt, updatedAt);
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