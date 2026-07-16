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
@Table(name = "goal_contributions")
public class GoalContribution {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "goal_id", nullable = false)
	private UUID goalId;

	@Column(nullable = false)
	private LocalDate month;

	@Column(nullable = false)
	private BigDecimal amount;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected GoalContribution() {
	}

	public GoalContribution(UUID goalId, LocalDate month, BigDecimal amount) {
		this.goalId = goalId;
		this.month = month;
		this.amount = amount;
	}

	@PrePersist
	void onCreate() {
		if (createdAt == null) {
			createdAt = Instant.now();
		}
	}

	public UUID getId() {
		return id;
	}

	public UUID getGoalId() {
		return goalId;
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
