package com.iispl.entity;

import com.iispl.enums.SourceType;

public final class SourceSystem extends BaseEntity {

	private final SourceType systemCode;
	private final String filePath;
	private final boolean isActive;

	public SourceSystem(SourceType systemCode, String filePath, boolean isActive) {
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