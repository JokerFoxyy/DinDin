package com.guaranin.api.cdi;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "cdi_rates")
public class CdiRate {

	@Id
	private LocalDate date;

	@Column(name = "daily_rate", nullable = false)
	private BigDecimal dailyRate;

	protected CdiRate() {
	}

	public CdiRate(LocalDate date, BigDecimal dailyRate) {
		this.date = date;
		this.dailyRate = dailyRate;
	}

	public LocalDate getDate() {
		return date;
	}

	public BigDecimal getDailyRate() {
		return dailyRate;
	}

}
