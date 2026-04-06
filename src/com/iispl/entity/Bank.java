package com.iispl.entity;

import java.time.LocalDateTime;

import java.util.List;




public class Bank extends BaseEntity {
	private String bankCode;
	private String bankName;
	private String ifscCode;
	private List<Customer> customers;

	public Bank(long id, LocalDateTime createdAt, LocalDateTime updatedAt, String bankCode, String bankName,
			String ifscCode, List<Customer> customers) {

		super(id, createdAt, updatedAt);
		this.bankCode = bankCode;
		this.bankName = bankName;
		this.ifscCode = ifscCode;
		this.customers = customers;
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

	public String getIsfcCode() {
		return ifscCode;
	}

	public void setIsfcCode(String isfcCode) {
		this.ifscCode = ifscCode;
	}

	public List<Customer> getCustomers() {
		return customers;
	}

	public void setCustomers(List<Customer> customers) {
		this.customers = customers;
	}

}