package com.guaranin.api.cdi;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface CdiRateRepository extends JpaRepository<CdiRate, LocalDate> {

	List<CdiRate> findAllByDateBetweenOrderByDateAsc(LocalDate from, LocalDate to);

}
