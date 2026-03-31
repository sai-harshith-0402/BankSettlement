package com.iispl.entity;

import com.iispl.enums.SourceType;

public final class SourceSystem {

	private final Long sourceId;
	private final SourceType systemCode;
	private final String filePath;
	private final boolean isActive;

	public SourceSystem(Long sourceId, SourceType systemCode, String filePath, boolean isActive) {
		this.sourceId = sourceId;
		this.systemCode = systemCode;
		this.filePath = filePath;
		this.isActive = isActive;
	}

	public Long getSourceId() {
		return sourceId;
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