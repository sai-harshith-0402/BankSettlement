package com.iispl.entity;

import java.time.LocalDateTime;

import com.iispl.enums.SourceType;

public class SourceSystem {
	private long sourceSystemId;
	private SourceType sourceType;
	private String filePath;



	public SourceSystem(long sourceSystemId, SourceType sourceType, String filePath) {

		this.sourceSystemId = sourceSystemId;
		this.sourceType = sourceType;
		this.filePath = filePath;
	}

	public long getSourceSystemId() {
		return sourceSystemId;
	}

	public void setSourceSystemId(long sourceSystemId) {
		this.sourceSystemId = sourceSystemId;
	}

	public SourceType getSourceType() {
		return sourceType;
	}

	public void setSourceType(SourceType sourceType) {
		this.sourceType = sourceType;
	}

	public String getFilePath() {
		return filePath;
	}

	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}

}