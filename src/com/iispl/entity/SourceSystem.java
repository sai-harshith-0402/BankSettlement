package com.iispl.entity;

import java.time.LocalDateTime;

import com.iispl.enums.SourceType;

public final class SourceSystem extends BaseEntity {

	private final SourceType systemCode;
	private final String filePath;
	private final boolean isActive;

	public SourceSystem(Long id, LocalDateTime createdAt, LocalDateTime updatedAt, SourceType systemCode,
			String filePath, boolean isActive) {
		super(id, createdAt, updatedAt);
		this.systemCode = systemCode;
		this.filePath = filePath;
		this.isActive = isActive;
	}

	public SourceType getSystemCode() {
		return systemCode;
	}

	public String getFilePath() {
		return filePath;
	}

	public boolean isActive() {
		return isActive;
	}
}