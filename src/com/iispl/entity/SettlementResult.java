package com.iispl.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.iispl.enums.SettlementStatus;

public class SettlementResult {
	private String batchId;
	private SettlementStatus status;
	private int settledCount;
	private int failedCount;
	private BigDecimal totalSettledAmount;
	private LocalDateTime processedAt;

	public SettlementResult(String batchId, SettlementStatus status, int settledCount, int failedCount,
			BigDecimal totalSettledAmount, LocalDateTime processedAt) {
		this.batchId = batchId;
		this.status = status;
		this.settledCount = settledCount;
		this.failedCount = failedCount;
		this.totalSettledAmount = totalSettledAmount;
		this.processedAt = processedAt;
	}

	public String getBatchId() {
		return batchId;
	}

	public void setBatchId(String batchId) {
		this.batchId = batchId;
	}

	public SettlementStatus getStatus() {
		return status;
	}

	public void setStatus(SettlementStatus status) {
		this.status = status;
	}

	public int getSettledCount() {
		return settledCount;
	}

	public void setSettledCount(int settledCount) {
		this.settledCount = settledCount;
	}

	public int getFailedCount() {
		return failedCount;
	}

	public void setFailedCount(int failedCount) {
		this.failedCount = failedCount;
	}

	public BigDecimal getTotalSettledAmount() {
		return totalSettledAmount;
	}

	public void setTotalSettledAmount(BigDecimal totalSettledAmount) {
		this.totalSettledAmount = totalSettledAmount;
	}

	public LocalDateTime getProcessedAt() {
		return processedAt;
	}

	public void setProcessedAt(LocalDateTime processedAt) {
		this.processedAt = processedAt;
	}

}