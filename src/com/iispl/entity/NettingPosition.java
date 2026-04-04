package com.iispl.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class NettingPosition {
	private long positiionId;
	private long counterpartyBankId;
	private BigDecimal grossDebitAmount;
	private BigDecimal grossCreditAmount;
	private BigDecimal netAmount;
	private LocalDateTime positionDate;
	public NettingPosition(long positiionId, long counterpartyBankId, BigDecimal grossDebitAmount,
			BigDecimal grossCreditAmount, BigDecimal netAmount, LocalDateTime positionDate) {
		this.positiionId = positiionId;
		this.counterpartyBankId = counterpartyBankId;
		this.grossDebitAmount = grossDebitAmount;
		this.grossCreditAmount = grossCreditAmount;
		this.netAmount = netAmount;
		this.positionDate = positionDate;
	}
	public long getPositiionId() {
		return positiionId;
	}
	public void setPositiionId(long positiionId) {
		this.positiionId = positiionId;
	}
	public long getCounterpartyBankId() {
		return counterpartyBankId;
	}
	public void setCounterpartyBankId(long counterpartyBankId) {
		this.counterpartyBankId = counterpartyBankId;
	}
	public BigDecimal getGrossDebitAmount() {
		return grossDebitAmount;
	}
	public void setGrossDebitAmount(BigDecimal grossDebitAmount) {
		this.grossDebitAmount = grossDebitAmount;
	}
	public BigDecimal getGrossCreditAmount() {
		return grossCreditAmount;
	}
	public void setGrossCreditAmount(BigDecimal grossCreditAmount) {
		this.grossCreditAmount = grossCreditAmount;
	}
	public BigDecimal getNetAmount() {
		return netAmount;
	}
	public void setNetAmount(BigDecimal netAmount) {
		this.netAmount = netAmount;
	}
	public LocalDateTime getPositionDate() {
		return positionDate;
	}
	public void setPositionDate(LocalDateTime positionDate) {
		this.positionDate = positionDate;
	}
	
	
}
