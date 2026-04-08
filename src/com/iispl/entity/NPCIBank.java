package com.iispl.entity;

import java.math.BigDecimal;

public class NPCIBank{
	private long bankId;
	private String bankName;
	private BigDecimal balanceAmount;
	
	public NPCIBank(long bankId, String bankName, BigDecimal balanceAmount) {
		this.bankId = bankId;
		this.bankName = bankName;
		this.balanceAmount = balanceAmount;
	}
	
	public long getBankId() {
		return bankId;
	}
	public void setBankId(long bankId) {
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
