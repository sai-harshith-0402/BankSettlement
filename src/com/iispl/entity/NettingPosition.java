package com.iispl.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class NettingPosition {

	private long positionId;
	private String batchId; // links position to its batch
	private String bankName; // stored directly — no NPCIBank lookup needed
	private long counterpartyBankId; // 0 if bank not in npci_bank table
	private BigDecimal grossDebitAmount;
	private BigDecimal grossCreditAmount;
	private BigDecimal netAmount;
	private LocalDateTime positionDate;

	// 8-param constructor — batchId and bankName are new
	public NettingPosition(long positionId, String batchId, String bankName, long counterpartyBankId,
			BigDecimal grossDebitAmount, BigDecimal grossCreditAmount, BigDecimal netAmount,
			LocalDateTime positionDate) {
		this.positionId = positionId;
		this.batchId = batchId;
		this.bankName = bankName;
		this.counterpartyBankId = counterpartyBankId;
		this.grossDebitAmount = grossDebitAmount;
		this.grossCreditAmount = grossCreditAmount;
		this.netAmount = netAmount;
		this.positionDate = positionDate;
	}

	public long getPositionId() {
		return positionId;
	}

	public void setPositionId(long positionId) {
		this.positionId = positionId;
	}

	public String getBatchId() {
		return batchId;
	}

	public void setBatchId(String batchId) {
		this.batchId = batchId;
	}

	public String getBankName() {
		return bankName;
	}

	public void setBankName(String bankName) {
		this.bankName = bankName;
	}

	public long getCounterpartyBankId() {
		return counterpartyBankId;
	}

	public void setCounterpartyBankId(long id) {
		this.counterpartyBankId = id;
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