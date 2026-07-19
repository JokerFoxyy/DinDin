package com.guaranin.api.category;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, UUID> {

	List<Category> findAllByUserIdOrderByNameAsc(UUID userId);

	Optional<Category> findByIdAndUserId(UUID id, UUID userId);

	boolean existsByUserIdAndNameIgnoreCaseAndKind(UUID userId, String name, CategoryKind kind);

	void deleteByUserId(UUID userId);

}
