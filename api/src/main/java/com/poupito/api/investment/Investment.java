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

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "investments")
public class Investment {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "user_id", nullable = false)
	private UUID userId;

	@Column(nullable = false)
	private String name;

	@Enumerated(EnumType.STRING)
	@Column(name = "class", nullable = false)
	private AssetClass assetClass;

	@Column(nullable = false)
	private String institution;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected Investment() {
	}

	public Investment(UUID userId, String name, AssetClass assetClass, String institution) {
		this.userId = userId;
		this.name = name;
		this.assetClass = assetClass;
		this.institution = institution;
	}

	@PrePersist
	void onCreate() {
		if (createdAt == null) {
			createdAt = Instant.now();
		}
	}

	public void update(String name, String institution) {
		this.name = name;
		this.institution = institution;
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

	public AssetClass getAssetClass() {
		return assetClass;
	}

	public String getInstitution() {
		return institution;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

}
