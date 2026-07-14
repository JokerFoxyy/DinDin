package com.dindin.api.transaction;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID>, JpaSpecificationExecutor<Transaction> {

	Optional<Transaction> findByIdAndUserId(UUID id, UUID userId);

	List<Transaction> findAllByUserIdOrderByDateAsc(UUID userId);

	List<Transaction> findAllByInvoiceIdOrderByDateAsc(UUID invoiceId);

	void deleteByUserId(UUID userId);

	@Query("select new com.dindin.api.transaction.CategorySpent(t.categoryId, sum(t.amount)) "
			+ "from Transaction t "
			+ "where t.userId = :userId and t.type = com.dindin.api.transaction.TransactionType.EXPENSE "
			+ "and t.categoryId in :categoryIds and t.date between :start and :end "
			+ "group by t.categoryId")
	List<CategorySpent> sumExpensesByCategory(@Param("userId") UUID userId,
			@Param("categoryIds") Collection<UUID> categoryIds,
			@Param("start") LocalDate start, @Param("end") LocalDate end);

}
