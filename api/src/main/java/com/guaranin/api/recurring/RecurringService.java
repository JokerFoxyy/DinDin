package com.guaranin.api.recurring;

import com.guaranin.api.account.Account;
import com.guaranin.api.account.AccountRepository;
import com.guaranin.api.category.Category;
import com.guaranin.api.category.CategoryKind;
import com.guaranin.api.category.CategoryRepository;
import com.guaranin.api.common.error.BusinessException;
import com.guaranin.api.common.error.NotFoundException;
import com.guaranin.api.recurring.dto.RecurringRequest;
import com.guaranin.api.recurring.dto.RecurringResponse;
import com.guaranin.api.transaction.TransactionType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class RecurringService {

	private final RecurringTransactionRepository recurringRepository;
	private final AccountRepository accountRepository;
	private final CategoryRepository categoryRepository;

	public RecurringService(RecurringTransactionRepository recurringRepository,
			AccountRepository accountRepository, CategoryRepository categoryRepository) {
		this.recurringRepository = recurringRepository;
		this.accountRepository = accountRepository;
		this.categoryRepository = categoryRepository;
	}

	@Transactional(readOnly = true)
	public List<RecurringResponse> list(UUID userId) {
		return recurringRepository.findAllByUserIdOrderByDescriptionAsc(userId).stream()
				.map(recurring -> toResponse(userId, recurring))
				.toList();
	}

	@Transactional
	public RecurringResponse create(UUID userId, RecurringRequest request) {
		ValidatedRefs refs = validate(userId, request);
		RecurringTransaction recurring = new RecurringTransaction(userId, refs.account().getId(),
				refs.category().getId(), request.description().trim(), request.amount(), request.type(),
				request.dayOfMonth(), request.activeOrDefault(), request.endDate());
		recurringRepository.save(recurring);
		return RecurringResponse.from(recurring, refs.account(), refs.category());
	}

	@Transactional
	public RecurringResponse update(UUID userId, UUID recurringId, RecurringRequest request) {
		RecurringTransaction recurring = findOwned(userId, recurringId);
		ValidatedRefs refs = validate(userId, request);
		recurring.update(refs.account().getId(), refs.category().getId(), request.description().trim(),
				request.amount(), request.type(), request.dayOfMonth(), request.activeOrDefault(),
				request.endDate());
		return RecurringResponse.from(recurring, refs.account(), refs.category());
	}

	@Transactional
	public void delete(UUID userId, UUID recurringId) {
		recurringRepository.delete(findOwned(userId, recurringId));
	}

	private RecurringTransaction findOwned(UUID userId, UUID recurringId) {
		return recurringRepository.findByIdAndUserId(recurringId, userId)
				.orElseThrow(() -> new NotFoundException("Fixo não encontrado"));
	}

	private RecurringResponse toResponse(UUID userId, RecurringTransaction recurring) {
		Account account = accountRepository.findByIdAndUserId(recurring.getAccountId(), userId).orElse(null);
		Category category = categoryRepository.findByIdAndUserId(recurring.getCategoryId(), userId).orElse(null);
		return RecurringResponse.from(recurring, account, category);
	}

	private ValidatedRefs validate(UUID userId, RecurringRequest request) {
		if (request.type() == TransactionType.INVOICE_ADJUSTMENT) {
			throw new BusinessException("Fixo deve ser gasto ou entrada");
		}
		Account account = accountRepository.findByIdAndUserId(request.accountId(), userId)
				.orElseThrow(() -> new NotFoundException("Conta não encontrada"));
		Category category = categoryRepository.findByIdAndUserId(request.categoryId(), userId)
				.orElseThrow(() -> new NotFoundException("Categoria não encontrada"));
		CategoryKind expectedKind = request.type() == TransactionType.EXPENSE
				? CategoryKind.EXPENSE
				: CategoryKind.INCOME;
		if (category.getKind() != expectedKind) {
			throw new BusinessException("Categoria não é compatível com o tipo do fixo");
		}
		return new ValidatedRefs(account, category);
	}

	private record ValidatedRefs(Account account, Category category) {
	}

}
