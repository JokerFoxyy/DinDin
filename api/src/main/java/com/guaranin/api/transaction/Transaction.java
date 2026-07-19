package com.guaranin.api.transaction;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
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

	@Column(name = "recurring_id")
	private UUID recurringId;

	@Column(nullable = false)
	private boolean paid = true;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "installment_group_id")
	private UUID installmentGroupId;

	@Column(name = "installment_number")
	private Integer installmentNumber;

	@Column(name = "installment_count")
	private Integer installmentCount;

	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "transaction_tags", joinColumns = @JoinColumn(name = "transaction_id"))
	@Column(name = "tag")
	private Set<String> tags = new HashSet<>();

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

	/** Transação gerada por um fixo: vinculada ao recurring e nasce não-paga. */
	public static Transaction materialized(UUID userId, UUID accountId, UUID categoryId, UUID invoiceId,
			String description, BigDecimal amount, LocalDate date, TransactionType type, UUID recurringId) {
		Transaction transaction = new Transaction(userId, accountId, categoryId, invoiceId,
				description, amount, date, type);
		transaction.recurringId = recurringId;
		transaction.paid = false;
		return transaction;
	}

	/** Uma parcela (1..count) de uma compra parcelada; amount é o valor da própria parcela. */
	public static Transaction installment(UUID userId, UUID accountId, UUID categoryId, UUID invoiceId,
			String description, BigDecimal amount, LocalDate date, TransactionType type,
			UUID installmentGroupId, int installmentNumber, int installmentCount) {
		Transaction transaction = new Transaction(userId, accountId, categoryId, invoiceId,
				description, amount, date, type);
		transaction.installmentGroupId = installmentGroupId;
		transaction.installmentNumber = installmentNumber;
		transaction.installmentCount = installmentCount;
		return transaction;
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

	public void updateTags(Set<String> tags) {
		this.tags.clear();
		this.tags.addAll(tags);
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

	public UUID getRecurringId() {
		return recurringId;
	}

	public boolean isPaid() {
		return paid;
	}

	public void markPaid(boolean paid) {
		this.paid = paid;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Set<String> getTags() {
		return tags;
	}

	public UUID getInstallmentGroupId() {
		return installmentGroupId;
	}

	public Integer getInstallmentNumber() {
		return installmentNumber;
	}

	public Integer getInstallmentCount() {
		return installmentCount;
	}

}
