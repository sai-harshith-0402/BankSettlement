package com.iispl.entity;

import java.time.LocalDateTime;
import java.math.BigDecimal;

public class AuditLog {

	private long logId;
	private LocalDateTime timestamp;
	private String correlationId;
	private String transactionId;
	private String serviceName;
	private String operation;
	private long accountId;
	private String status;
	private BigDecimal amount;

	public AuditLog(long logId, LocalDateTime timestamp, String correlationId, String transactionId, String serviceName,
			String operation, long accountId, String status, BigDecimal amount) {

		this.logId = logId;
		this.timestamp = timestamp;
		this.correlationId = correlationId;
		this.transactionId = transactionId;
		this.serviceName = serviceName;
		this.operation = operation;
		this.accountId = accountId;
		this.status = status;
		this.amount = amount;
	}

	public long getLogId() {
		return logId;
	}

	public LocalDateTime getTimestamp() {
		return timestamp;
	}

	public String getCorrelationId() {
		return correlationId;
	}

	public String getTransactionId() {
		return transactionId;
	}

	public String getServiceName() {
		return serviceName;
	}

	public String getOperation() {
		return operation;
	}

	public long getAccountId() {
		return accountId;
	}

	public String getStatus() {
		return status;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	@Override
	public String toString() {
		return "AuditLog{" + "logId=" + logId + ", timestamp=" + timestamp + ", correlationId='" + correlationId + '\''
				+ ", transactionId='" + transactionId + '\'' + ", serviceName='" + serviceName + '\'' + ", operation='"
				+ operation + '\'' + ", accountId=" + accountId + ", status='" + status + '\'' + ", amount=" + amount
				+ '}';
	}
}