package com.poupito.api.invoice;

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
@Table(name = "card_invoices")
public class CardInvoice {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "account_id", nullable = false)
	private UUID accountId;

	/** Primeiro dia do mês da fatura. */
	@Column(nullable = false)
	private LocalDate month;

	@Column(name = "closing_date", nullable = false)
	private LocalDate closingDate;

	@Column(name = "due_date", nullable = false)
	private LocalDate dueDate;

	@Column(name = "declared_total")
	private BigDecimal declaredTotal;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private InvoiceStatus status = InvoiceStatus.OPEN;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected CardInvoice() {
	}

	public CardInvoice(UUID accountId, LocalDate month, LocalDate closingDate, LocalDate dueDate) {
		this.accountId = accountId;
		this.month = month;
		this.closingDate = closingDate;
		this.dueDate = dueDate;
	}

	@PrePersist
	void onCreate() {
		if (createdAt == null) {
			createdAt = Instant.now();
		}
	}

	public void close(BigDecimal declaredTotal) {
		this.declaredTotal = declaredTotal;
		this.status = InvoiceStatus.CLOSED;
	}

	public void pay() {
		this.status = InvoiceStatus.PAID;
	}

	public void reopen() {
		this.status = InvoiceStatus.OPEN;
	}

	public UUID getId() {
		return id;
	}

	public UUID getAccountId() {
		return accountId;
	}

	public LocalDate getMonth() {
		return month;
	}

	public LocalDate getClosingDate() {
		return closingDate;
	}

	public LocalDate getDueDate() {
		return dueDate;
	}

	public BigDecimal getDeclaredTotal() {
		return declaredTotal;
	}

	public InvoiceStatus getStatus() {
		return status;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

}
