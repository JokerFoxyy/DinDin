package com.dindin.api.goal;

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
@Table(name = "goals")
public class Goal {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "user_id", nullable = false)
	private UUID userId;

	@Column(nullable = false)
	private String name;

	@Column(name = "target_amount", nullable = false)
	private BigDecimal targetAmount;

	@Column(name = "target_date", nullable = false)
	private LocalDate targetDate;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected Goal() {
	}

	public Goal(UUID userId, String name, BigDecimal targetAmount, LocalDate targetDate) {
		this.userId = userId;
		this.name = name;
		this.targetAmount = targetAmount;
		this.targetDate = targetDate;
	}

	@PrePersist
	void onCreate() {
		if (createdAt == null) {
			createdAt = Instant.now();
		}
	}

	public void update(String name, BigDecimal targetAmount, LocalDate targetDate) {
		this.name = name;
		this.targetAmount = targetAmount;
		this.targetDate = targetDate;
	}

	public UUID getId() {
		return id;
	}

	public UUID getUserId() {
		return userId;
	}

	public String getName() {
		return name;
	}

	public BigDecimal getTargetAmount() {
		return targetAmount;
	}

	public LocalDate getTargetDate() {
		return targetDate;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

}
