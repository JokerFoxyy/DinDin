package com.guaranin.api.category;

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
@Table(name = "categories")
public class Category {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "user_id", nullable = false)
	private UUID userId;

	@Column(nullable = false)
	private String name;

	private String icon;

	private String color;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private CategoryKind kind;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected Category() {
	}

	public Category(UUID userId, String name, String icon, String color, CategoryKind kind) {
		this.userId = userId;
		this.name = name;
		this.icon = icon;
		this.color = color;
		this.kind = kind;
	}

	@PrePersist
	void onCreate() {
		if (createdAt == null) {
			createdAt = Instant.now();
		}
	}

	public void update(String name, String icon, String color, CategoryKind kind) {
		this.name = name;
		this.icon = icon;
		this.color = color;
		this.kind = kind;
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

	public String getIcon() {
		return icon;
	}

	public String getColor() {
		return color;
	}

	public CategoryKind getKind() {
		return kind;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

}
