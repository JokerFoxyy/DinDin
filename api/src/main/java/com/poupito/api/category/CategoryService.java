package com.poupito.api.category;

import com.poupito.api.category.dto.CategoryRequest;
import com.poupito.api.category.dto.CategoryResponse;
import com.poupito.api.common.error.DuplicateResourceException;
import com.poupito.api.common.error.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class CategoryService {

	private final CategoryRepository categoryRepository;

	public CategoryService(CategoryRepository categoryRepository) {
		this.categoryRepository = categoryRepository;
	}

	@Transactional(readOnly = true)
	public List<CategoryResponse> list(UUID userId) {
		return categoryRepository.findAllByUserIdOrderByNameAsc(userId).stream()
				.map(CategoryResponse::from)
				.toList();
	}

	@Transactional
	public CategoryResponse create(UUID userId, CategoryRequest request) {
		String name = request.name().trim();
		if (categoryRepository.existsByUserIdAndNameIgnoreCaseAndKind(userId, name, request.kind())) {
			throw new DuplicateResourceException("Categoria já existe");
		}
		Category category = new Category(userId, name, request.icon(), request.color(), request.kind());
		return CategoryResponse.from(categoryRepository.save(category));
	}

	@Transactional
	public CategoryResponse update(UUID userId, UUID categoryId, CategoryRequest request) {
		Category category = findOwned(userId, categoryId);
		String name = request.name().trim();
		boolean renamed = !category.getName().equalsIgnoreCase(name) || category.getKind() != request.kind();
		if (renamed && categoryRepository.existsByUserIdAndNameIgnoreCaseAndKind(userId, name, request.kind())) {
			throw new DuplicateResourceException("Categoria já existe");
		}
		category.update(name, request.icon(), request.color(), request.kind());
		return CategoryResponse.from(category);
	}

	@Transactional
	public void delete(UUID userId, UUID categoryId) {
		categoryRepository.delete(findOwned(userId, categoryId));
	}

	private Category findOwned(UUID userId, UUID categoryId) {
		return categoryRepository.findByIdAndUserId(categoryId, userId)
				.orElseThrow(() -> new NotFoundException("Categoria não encontrada"));
	}

}
