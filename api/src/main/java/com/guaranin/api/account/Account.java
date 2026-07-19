package com.guaranin.api.account;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "accounts")
public class Account {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "user_id", nullable = false)
	private UUID userId;

	@Column(nullable = false)
	private String name;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private AccountType type;

	@Column(name = "closing_day")
	private Integer closingDay;

	@Column(name = "due_day")
	private Integer dueDay;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected Account() {
	}

	public Account(UUID userId, String name, AccountType type, Integer closingDay, Integer dueDay) {
		this.userId = userId;
		this.name = name;
		this.type = type;
		this.closingDay = closingDay;
		this.dueDay = dueDay;
	}

	@PrePersist
	void onCreate() {
		if (createdAt == null) {
			createdAt = Instant.now();
		}
	}

	public void update(String name, AccountType type, Integer closingDay, Integer dueDay) {
		this.name = name;
		this.type = type;
		this.closingDay = closingDay;
		this.dueDay = dueDay;
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

	public AccountType getType() {
		return type;
	}

	public Integer getClosingDay() {
		return closingDay;
	}

	public Integer getDueDay() {
		return dueDay;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

}
