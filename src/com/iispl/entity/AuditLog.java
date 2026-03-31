package com.iispl.entity;

import com.iispl.enums.AuditAction;

import java.time.LocalDateTime;

public final class AuditLog {

	private final String entityType;
	private final Long entityId;
	private final AuditAction action;
	private final String changedBy;
	private final LocalDateTime changedAt;

	public AuditLog(String entityType, Long entityId, AuditAction action, String changedBy) {
		this.entityType = entityType;
		this.entityId = entityId;
		this.action = action;
		this.changedBy = changedBy;
		this.changedAt = LocalDateTime.now();
	}

	public String getEntityType() {
		return entityType;
	}

	public Long getEntityId() {
		return entityId;
	}

	public AuditAction getAction() {
		return action;
	}

	public String getChangedBy() {
		return changedBy;
	}

	public LocalDateTime getChangedAt() {
		return changedAt;
	}
}