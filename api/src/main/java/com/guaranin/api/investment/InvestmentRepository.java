package com.guaranin.api.investment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InvestmentRepository extends JpaRepository<Investment, UUID> {

	List<Investment> findAllByUserIdOrderByCreatedAtAsc(UUID userId);

	Optional<Investment> findByIdAndUserId(UUID id, UUID userId);

	void deleteByUserId(UUID userId);

}
