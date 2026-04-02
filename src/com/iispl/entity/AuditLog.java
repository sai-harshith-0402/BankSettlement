package com.iispl.entity;

import com.iispl.enums.AuditAction;

import java.time.LocalDateTime;

public final class AuditLog extends BaseEntity {

	private final String entityType;
	private final Long entityId;
	private final AuditAction action;
	private final String changedBy;
	private final LocalDateTime changedAt;

	public AuditLog(Long id, LocalDateTime createdAt, LocalDateTime updatedAt, String entityType, Long entityId,
			AuditAction action, String changedBy, LocalDateTime changedAt) {
		super(id, createdAt, updatedAt);
		this.entityType = entityType;
		this.entityId = entityId;
		this.action = action;
		this.changedBy = changedBy;
		this.changedAt = changedAt;
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