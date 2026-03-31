package com.iispl.entity;

import com.iispl.enums.BatchStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class SettlementResult extends BaseEntity {

	private final String batchId;
	private final LocalDate batchDate;
	private BatchStatus batchStatus;
	private List<Transaction> transactionsList;
	private int totalTransactions;
	private int settledCount;
	private int failedCount;
	private BigDecimal totalAmount;
	private BigDecimal settledAmount;
	private BigDecimal netAmount;
	private String exportedFilePath;
	private LocalDateTime processedAt;

	public SettlementResult(String batchId, LocalDate batchDate) {
		this.batchId = batchId;
		this.batchDate = batchDate;
		this.batchStatus = BatchStatus.RUNNING;
	}

	public String getBatchId() {
		return batchId;
	}

	public LocalDate getBatchDate() {
		return batchDate;
	}

	public BatchStatus getBatchStatus() {
		return batchStatus;
	}

	public List<Transaction> getTransactions() {
		return transactionsList;
	}

	public int getTotalTransactions() {
		return totalTransactions;
	}

	public int getSettledCount() {
		return settledCount;
	}

	public int getFailedCount() {
		return failedCount;
	}

	public BigDecimal getTotalAmount() {
		return totalAmount;
	}

	public BigDecimal getSettledAmount() {
		return settledAmount;
	}

	public BigDecimal getNetAmount() {
		return netAmount;
	}

	public String getExportedFilePath() {
		return exportedFilePath;
	}

	public LocalDateTime getProcessedAt() {
		return processedAt;
	}

	public void setBatchStatus(BatchStatus batchStatus) {
		this.batchStatus = batchStatus;
	}

	public void setTransactions(List<Transaction> transactions) {
		this.transactionsList = transactions;
	}

	public void setTotalTransactions(int totalTransactions) {
		this.totalTransactions = totalTransactions;
	}

	public void setSettledCount(int settledCount) {
		this.settledCount = settledCount;
	}

	public void setFailedCount(int failedCount) {
		this.failedCount = failedCount;
	}

	public void setTotalAmount(BigDecimal totalAmount) {
		this.totalAmount = totalAmount;
	}

	public void setSettledAmount(BigDecimal settledAmount) {
		this.settledAmount = settledAmount;
	}

	public void setNetAmount(BigDecimal netAmount) {
		this.netAmount = netAmount;
	}

	public void setExportedFilePath(String exportedFilePath) {
		this.exportedFilePath = exportedFilePath;
	}

	public void setProcessedAt(LocalDateTime processedAt) {
		this.processedAt = processedAt;
	}
}