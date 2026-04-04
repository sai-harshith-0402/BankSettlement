package com.iispl.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.iispl.enums.ReconStatus;

public class ReconciliationEntry {
	private long entryId;
	private LocalDate reconciliationDate;
	private long accountId;
	private BigDecimal expectedAmount;
	private BigDecimal actualAmount;
	private BigDecimal variance;
	private ReconStatus reconStatus;

	public ReconciliationEntry(long entryId, LocalDate reconciliationDate, long accountId, BigDecimal expectedAmount,
			BigDecimal actualAmount, BigDecimal variance, ReconStatus reconStatus) {
		this.entryId = entryId;
		this.reconciliationDate = reconciliationDate;
		this.accountId = accountId;
		this.expectedAmount = expectedAmount;
		this.actualAmount = actualAmount;
		this.variance = variance;
		this.reconStatus = reconStatus;
	}

	public long getEntryId() {
		return entryId;
	}

	public void setEntryId(long entryId) {
		this.entryId = entryId;
	}

	public LocalDate getReconciliationDate() {
		return reconciliationDate;
	}

	public void setReconciliationDate(LocalDate reconciliationDate) {
		this.reconciliationDate = reconciliationDate;
	}

	public long getAccountId() {
		return accountId;
	}

	public void setAccountId(long accountId) {
		this.accountId = accountId;
	}

	public BigDecimal getExpectedAmount() {
		return expectedAmount;
	}

	public void setExpectedAmount(BigDecimal expectedAmount) {
		this.expectedAmount = expectedAmount;
	}

	public BigDecimal getActualAmount() {
		return actualAmount;
	}

	public void setActualAmount(BigDecimal actualAmount) {
		this.actualAmount = actualAmount;
	}

	public BigDecimal getVariance() {
		return variance;
	}

	public void setVariance(BigDecimal variance) {
		this.variance = variance;
	}

	public ReconStatus getReconStatus() {
		return reconStatus;
	}

	public void setReconStatus(ReconStatus reconStatus) {
		this.reconStatus = reconStatus;
	}

}
