package com.poupito.api.recurring;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RecurringTransactionRepository extends JpaRepository<RecurringTransaction, UUID> {

	List<RecurringTransaction> findAllByUserIdOrderByDescriptionAsc(UUID userId);

	Optional<RecurringTransaction> findByIdAndUserId(UUID id, UUID userId);

	List<RecurringTransaction> findAllByActiveTrue();

	void deleteByUserId(UUID userId);

}
