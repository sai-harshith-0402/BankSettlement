package com.iispl.entity;

import java.math.BigDecimal;

public class NPCIBank{
	private String bankId;
	private String bankName;
	private BigDecimal balanceAmount;
	
	public NPCIBank(String bankId, String bankName, BigDecimal balanceAmount) {
		this.bankId = bankId;
		this.bankName = bankName;
		this.balanceAmount = balanceAmount;
	}
	
	public String getBankId() {
		return bankId;
	}
	public void setBankId(String bankId) {
		this.bankId = bankId;
	}
	public String getBankName() {
		return bankName;
	}
	public void setBankName(String bankName) {
		this.bankName = bankName;
	}
	public BigDecimal getBalanceAmount() {
		return balanceAmount;
	}
	public void setBalanceAmount(BigDecimal balanceAmount) {
		this.balanceAmount = balanceAmount;
	}
	
	
}
