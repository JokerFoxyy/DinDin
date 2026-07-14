package com.dindin.api.transaction;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID>, JpaSpecificationExecutor<Transaction> {

	Optional<Transaction> findByIdAndUserId(UUID id, UUID userId);

	List<Transaction> findAllByUserIdOrderByDateAsc(UUID userId);

	void deleteByUserId(UUID userId);

	boolean existsByRecurringIdAndDateBetween(UUID recurringId, LocalDate start, LocalDate end);

	Optional<Transaction> findByRecurringIdAndDateBetween(UUID recurringId, LocalDate start, LocalDate end);

}
