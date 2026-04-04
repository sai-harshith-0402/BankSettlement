package com.iispl.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.iispl.enums.BatchStatus;

public class Batch {
	private String batchId;
	private LocalDate batchDate;
	private BatchStatus batchStatus;
	private long totalTransactions;
	private BigDecimal totalAmount;
	private List<IncomingTransaction> transactionList;

	public Batch(String batchId, LocalDate batchDate, BatchStatus batchStatus, long totalTransactions,
			BigDecimal totalAmount, List<IncomingTransaction> transactionList) {
		this.batchId = batchId;
		this.batchDate = batchDate;
		this.batchStatus = batchStatus;
		this.totalTransactions = totalTransactions;
		this.totalAmount = totalAmount;
		this.transactionList = transactionList;
	}

	public String getBatchId() {
		return batchId;
	}

	public void setBatchId(String batchId) {
		this.batchId = batchId;
	}

	public LocalDate getBatchDate() {
		return batchDate;
	}

	public void setBatchDate(LocalDate batchDate) {
		this.batchDate = batchDate;
	}

	public BatchStatus getBatchStatus() {
		return batchStatus;
	}

	public void setBatchStatus(BatchStatus batchStatus) {
		this.batchStatus = batchStatus;
	}

	public long getTotalTransactions() {
		return totalTransactions;
	}

	public void setTotalTransactions(long totalTransactions) {
		this.totalTransactions = totalTransactions;
	}

	public BigDecimal getTotalAmount() {
		return totalAmount;
	}

	public void setTotalAmount(BigDecimal totalAmount) {
		this.totalAmount = totalAmount;
	}

	public List<IncomingTransaction> getTransactionList() {
		return transactionList;
	}

	public void setTransactionList(List<IncomingTransaction> transactionList) {
		this.transactionList = transactionList;
	}

}
