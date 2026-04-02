package com.iispl.entity;

import java.time.LocalDateTime;

public abstract class BaseEntity {

	private Long id;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;

	public BaseEntity(Long id, LocalDateTime createdAt, LocalDateTime updatedAt) {
		this.id = id;
		this.createdAt = createdAt;
		this.updatedAt = updatedAt;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(LocalDateTime updatedAt) {
		this.updatedAt = updatedAt;
	}
}