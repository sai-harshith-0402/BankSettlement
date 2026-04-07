package com.iispl.entity;

import java.time.LocalDateTime;

import java.util.List;




public class Bank extends BaseEntity {
	private String bankCode;
	private String bankName;
	private String ifscCode;
	private boolean activeStatus;
	private List<Customer> customers;

	

	public Bank(long id, LocalDateTime createdAt, LocalDateTime updatedAt, String bankCode, String bankName,
			String ifscCode, boolean activeStatus) {
		super(id, createdAt, updatedAt);
		this.bankCode = bankCode;
		this.bankName = bankName;
		this.ifscCode = ifscCode;
		this.activeStatus = activeStatus;
	}

	public String getBankCode() {
		return bankCode;
	}

	public void setBankCode(String bankCode) {
		this.bankCode = bankCode;
	}

	public String getBankName() {
		return bankName;
	}

	public void setBankName(String bankName) {
		this.bankName = bankName;
	}

	public String getIfscCode() {
		return ifscCode;
	}

	public void setIfscCode(String ifscCode) {
		this.ifscCode = ifscCode;
	}

	public List<Customer> getCustomers() {
		return customers;
	}

	public void setCustomers(List<Customer> customers) {
		this.customers = customers;
	}

	public boolean isActive() {
		return activeStatus;
	}

	public void setActiveStatus(boolean activeStatus) {
		this.activeStatus = activeStatus;
	}
	
	

}