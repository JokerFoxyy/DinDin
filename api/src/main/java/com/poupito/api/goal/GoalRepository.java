package com.poupito.api.goal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GoalRepository extends JpaRepository<Goal, UUID> {

	List<Goal> findAllByUserIdOrderByCreatedAtAsc(UUID userId);

	Optional<Goal> findByIdAndUserId(UUID id, UUID userId);

	void deleteByUserId(UUID userId);

}
