package com.guaranin.api.budget;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BudgetRepository extends JpaRepository<Budget, UUID> {

	List<Budget> findAllByUserIdAndMonthOrderByCreatedAtAsc(UUID userId, LocalDate month);

	Optional<Budget> findByIdAndUserId(UUID id, UUID userId);

	boolean existsByUserIdAndCategoryIdAndMonth(UUID userId, UUID categoryId, LocalDate month);

	void deleteByUserId(UUID userId);

}
