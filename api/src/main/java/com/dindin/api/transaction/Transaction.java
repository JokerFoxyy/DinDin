package com.dindin.api.transaction;

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
@Table(name = "transactions")
public class Transaction {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "user_id", nullable = false)
	private UUID userId;

	@Column(name = "account_id", nullable = false)
	private UUID accountId;

	@Column(name = "category_id")
	private UUID categoryId;

	@Column(name = "invoice_id")
	private UUID invoiceId;

	@Column(nullable = false)
	private String description;

	@Column(nullable = false)
	private BigDecimal amount;

	@Column(nullable = false)
	private LocalDate date;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private TransactionType type;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected Transaction() {
	}

	public Transaction(UUID userId, UUID accountId, UUID categoryId, UUID invoiceId,
			String description, BigDecimal amount, LocalDate date, TransactionType type) {
		this.userId = userId;
		this.accountId = accountId;
		this.categoryId = categoryId;
		this.invoiceId = invoiceId;
		this.description = description;
		this.amount = amount;
		this.date = date;
		this.type = type;
	}

	@PrePersist
	void onCreate() {
		if (createdAt == null) {
			createdAt = Instant.now();
		}
	}

	public void update(UUID accountId, UUID categoryId, UUID invoiceId,
			String description, BigDecimal amount, LocalDate date, TransactionType type) {
		this.accountId = accountId;
		this.categoryId = categoryId;
		this.invoiceId = invoiceId;
		this.description = description;
		this.amount = amount;
		this.date = date;
		this.type = type;
	}

	public UUID getId() {
		return id;
	}

	public UUID getUserId() {
		return userId;
	}

	public UUID getAccountId() {
		return accountId;
	}

	public UUID getCategoryId() {
		return categoryId;
	}

	public UUID getInvoiceId() {
		return invoiceId;
	}

	public String getDescription() {
		return description;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public LocalDate getDate() {
		return date;
	}

	public TransactionType getType() {
		return type;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

}
