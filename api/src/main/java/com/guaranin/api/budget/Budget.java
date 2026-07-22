package com.guaranin.api.budget;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "budgets")
public class Budget {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "user_id", nullable = false)
	private UUID userId;

	@Column(name = "category_id", nullable = false)
	private UUID categoryId;

	@Column(nullable = false)
	private LocalDate month;

	@Column(nullable = false)
	private BigDecimal amount;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected Budget() {
	}

	public Budget(UUID userId, UUID categoryId, LocalDate month, BigDecimal amount) {
		this.userId = userId;
		this.categoryId = categoryId;
		this.month = month;
		this.amount = amount;
	}

	@PrePersist
	void onCreate() {
		if (createdAt == null) {
			createdAt = Instant.now();
		}
	}

	public void updateAmount(BigDecimal amount) {
		this.amount = amount;
	}

	public UUID getId() {
		return id;
	}

	public UUID getUserId() {
		return userId;
	}

	public UUID getCategoryId() {
		return categoryId;
	}

	public LocalDate getMonth() {
		return month;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

}
