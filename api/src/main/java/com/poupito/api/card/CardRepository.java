package com.poupito.api.card;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CardRepository extends JpaRepository<Card, UUID> {

	List<Card> findAllByUserIdOrderByNameAsc(UUID userId);

	Optional<Card> findByIdAndUserId(UUID id, UUID userId);

	List<Card> findAllByUserId(UUID userId);

	boolean existsByAccountIdIn(Collection<UUID> accountIds);

	void deleteByUserId(UUID userId);

}
