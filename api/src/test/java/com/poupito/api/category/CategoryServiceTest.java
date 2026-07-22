package com.poupito.api.category;

import com.poupito.api.category.dto.CategoryRequest;
import com.poupito.api.category.dto.CategoryResponse;
import com.poupito.api.common.error.DuplicateResourceException;
import com.poupito.api.common.error.NotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

	private final UUID userId = UUID.randomUUID();

	@Mock
	private CategoryRepository categoryRepository;

	@InjectMocks
	private CategoryService categoryService;

	@Test
	void shouldCreateCategory_whenNameIsNewForUser() {
		when(categoryRepository.existsByUserIdAndNameIgnoreCaseAndKind(userId, "Mercado", CategoryKind.EXPENSE))
				.thenReturn(false);
		when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));

		CategoryResponse response = categoryService.create(userId,
				new CategoryRequest(" Mercado ", "🛒", "#3fb950", CategoryKind.EXPENSE));

		assertThat(response.name()).isEqualTo("Mercado");
		assertThat(response.icon()).isEqualTo("🛒");
		assertThat(response.color()).isEqualTo("#3fb950");
	}

	@Test
	void shouldThrowDuplicate_whenCategoryAlreadyExists() {
		when(categoryRepository.existsByUserIdAndNameIgnoreCaseAndKind(userId, "Mercado", CategoryKind.EXPENSE))
				.thenReturn(true);

		assertThatThrownBy(() -> categoryService.create(userId,
				new CategoryRequest("Mercado", null, null, CategoryKind.EXPENSE)))
				.isInstanceOf(DuplicateResourceException.class);
		verify(categoryRepository, never()).save(any());
	}

	@Test
	void shouldUpdateCategory_whenRenamingToFreeName() {
		Category category = new Category(userId, "Mercado", null, null, CategoryKind.EXPENSE);
		when(categoryRepository.findByIdAndUserId(any(), eq(userId))).thenReturn(Optional.of(category));
		when(categoryRepository.existsByUserIdAndNameIgnoreCaseAndKind(userId, "Supermercado", CategoryKind.EXPENSE))
				.thenReturn(false);

		CategoryResponse response = categoryService.update(userId, UUID.randomUUID(),
				new CategoryRequest("Supermercado", "🛒", "#3fb950", CategoryKind.EXPENSE));

		assertThat(response.name()).isEqualTo("Supermercado");
	}

	@Test
	void shouldNotCheckDuplicate_whenOnlyChangingIconAndColor() {
		Category category = new Category(userId, "Mercado", null, null, CategoryKind.EXPENSE);
		when(categoryRepository.findByIdAndUserId(any(), eq(userId))).thenReturn(Optional.of(category));

		CategoryResponse response = categoryService.update(userId, UUID.randomUUID(),
				new CategoryRequest("mercado", "🛒", "#d29922", CategoryKind.EXPENSE));

		assertThat(response.color()).isEqualTo("#d29922");
		verify(categoryRepository, never())
				.existsByUserIdAndNameIgnoreCaseAndKind(any(), any(), any());
	}

	@Test
	void shouldThrowDuplicate_whenRenamingToExistingName() {
		Category category = new Category(userId, "Mercado", null, null, CategoryKind.EXPENSE);
		when(categoryRepository.findByIdAndUserId(any(), eq(userId))).thenReturn(Optional.of(category));
		when(categoryRepository.existsByUserIdAndNameIgnoreCaseAndKind(userId, "Saúde", CategoryKind.EXPENSE))
				.thenReturn(true);

		assertThatThrownBy(() -> categoryService.update(userId, UUID.randomUUID(),
				new CategoryRequest("Saúde", null, null, CategoryKind.EXPENSE)))
				.isInstanceOf(DuplicateResourceException.class);
	}

	@Test
	void shouldThrowNotFound_whenUpdatingCategoryOfAnotherUser() {
		when(categoryRepository.findByIdAndUserId(any(), eq(userId))).thenReturn(Optional.empty());

		assertThatThrownBy(() -> categoryService.update(userId, UUID.randomUUID(),
				new CategoryRequest("Mercado", null, null, CategoryKind.EXPENSE)))
				.isInstanceOf(NotFoundException.class);
	}

	@Test
	void shouldDeleteCategory_whenOwnedByUser() {
		Category category = new Category(userId, "Mercado", null, null, CategoryKind.EXPENSE);
		when(categoryRepository.findByIdAndUserId(any(), eq(userId))).thenReturn(Optional.of(category));

		categoryService.delete(userId, UUID.randomUUID());

		verify(categoryRepository).delete(category);
	}

	@Test
	void shouldListCategoriesOfUser_whenCalled() {
		when(categoryRepository.findAllByUserIdOrderByNameAsc(userId))
				.thenReturn(List.of(new Category(userId, "Salário", "💰", "#3fb950", CategoryKind.INCOME)));

		List<CategoryResponse> categories = categoryService.list(userId);

		assertThat(categories).hasSize(1);
		assertThat(categories.getFirst().kind()).isEqualTo(CategoryKind.INCOME);
	}

}
