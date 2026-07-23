package com.poupito.api.investment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "investment_entries")
public class InvestmentEntry {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "investment_id", nullable = false)
	private UUID investmentId;

	@Column(nullable = false)
	private LocalDate date;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private EntryType type;

	@Column(nullable = false)
	private BigDecimal amount;

	@Column(name = "balance_after")
	private BigDecimal balanceAfter;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected InvestmentEntry() {
	}

	public InvestmentEntry(UUID investmentId, LocalDate date, EntryType type, BigDecimal amount,
			BigDecimal balanceAfter) {
		this.investmentId = investmentId;
		this.date = date;
		this.type = type;
		this.amount = amount;
		this.balanceAfter = balanceAfter;
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

	public UUID getInvestmentId() {
		return investmentId;
	}

	public LocalDate getDate() {
		return date;
	}

	public EntryType getType() {
		return type;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public BigDecimal getBalanceAfter() {
		return balanceAfter;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

}
