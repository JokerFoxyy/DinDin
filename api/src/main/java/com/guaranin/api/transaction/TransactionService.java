package com.guaranin.api.transaction;

import com.guaranin.api.account.Account;
import com.guaranin.api.account.AccountRepository;
import com.guaranin.api.account.AccountType;
import com.guaranin.api.category.Category;
import com.guaranin.api.category.CategoryKind;
import com.guaranin.api.category.CategoryRepository;
import com.guaranin.api.common.dto.PageResponse;
import com.guaranin.api.common.error.BusinessException;
import com.guaranin.api.common.error.NotFoundException;
import com.guaranin.api.invoice.CardInvoice;
import com.guaranin.api.invoice.CardInvoiceRepository;
import com.guaranin.api.invoice.CardInvoiceService;
import com.guaranin.api.transaction.dto.TransactionRequest;
import com.guaranin.api.transaction.dto.TransactionResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class TransactionService {

	private static final Sort DEFAULT_SORT = Sort.by(Sort.Order.desc("date"), Sort.Order.desc("createdAt"));

	private final TransactionRepository transactionRepository;
	private final AccountRepository accountRepository;
	private final CategoryRepository categoryRepository;
	private final CardInvoiceRepository cardInvoiceRepository;
	private final CardInvoiceService cardInvoiceService;

	public TransactionService(TransactionRepository transactionRepository, AccountRepository accountRepository,
			CategoryRepository categoryRepository, CardInvoiceRepository cardInvoiceRepository,
			CardInvoiceService cardInvoiceService) {
		this.transactionRepository = transactionRepository;
		this.accountRepository = accountRepository;
		this.categoryRepository = categoryRepository;
		this.cardInvoiceRepository = cardInvoiceRepository;
		this.cardInvoiceService = cardInvoiceService;
	}

	@Transactional(readOnly = true)
	public PageResponse<TransactionResponse> search(UUID userId, YearMonth month, UUID accountId,
			UUID categoryId, TransactionType type, String q, String tag, int page, int size) {
		Page<Transaction> transactions = transactionRepository.findAll(
				TransactionSpecifications.search(userId, month, accountId, categoryId, type, q, tag),
				PageRequest.of(page, Math.min(size, 200), DEFAULT_SORT));

		Map<UUID, Account> accounts = byId(transactions, Transaction::getAccountId, accountRepository::findAllById,
				Account::getId);
		Map<UUID, Category> categories = byId(transactions, Transaction::getCategoryId,
				categoryRepository::findAllById, Category::getId);
		Map<UUID, CardInvoice> invoices = byId(transactions, Transaction::getInvoiceId,
				cardInvoiceRepository::findAllById, CardInvoice::getId);

		return PageResponse.from(transactions, transaction -> TransactionResponse.from(transaction,
				accounts.get(transaction.getAccountId()),
				transaction.getCategoryId() != null ? categories.get(transaction.getCategoryId()) : null,
				transaction.getInvoiceId() != null && invoices.containsKey(transaction.getInvoiceId())
						? invoices.get(transaction.getInvoiceId()).getMonth()
						: null));
	}

	@Transactional
	public TransactionResponse create(UUID userId, TransactionRequest request) {
		ValidatedRefs refs = validate(userId, request);
		int installments = request.installments() != null ? request.installments() : 1;
		if (installments > 1 && request.type() != TransactionType.EXPENSE) {
			throw new BusinessException("Parcelamento só é permitido para gastos");
		}
		Set<String> tags = normalizeTags(request.tags());
		if (installments == 1) {
			Transaction transaction = new Transaction(userId, refs.account().getId(), refs.category().getId(),
					invoiceIdFor(refs.account(), request.date()), request.description().trim(),
					request.amount(), request.date(), request.type());
			transaction.updateTags(tags);
			transactionRepository.save(transaction);
			return toResponse(transaction, refs);
		}
		UUID groupId = UUID.randomUUID();
		Transaction firstInstallment = null;
		for (int i = 0; i < installments; i++) {
			LocalDate installmentDate = request.date().plusMonths(i);
			Transaction installment = Transaction.installment(userId, refs.account().getId(),
					refs.category().getId(), invoiceIdFor(refs.account(), installmentDate),
					request.description().trim(), request.amount(), installmentDate, request.type(),
					groupId, i + 1, installments);
			installment.updateTags(tags);
			transactionRepository.save(installment);
			if (i == 0) {
				firstInstallment = installment;
			}
		}
		return toResponse(firstInstallment, refs);
	}

	@Transactional
	public TransactionResponse update(UUID userId, UUID transactionId, TransactionRequest request) {
		Transaction transaction = findOwned(userId, transactionId);
		if (transaction.getType() == TransactionType.INVOICE_ADJUSTMENT) {
			throw new BusinessException("Ajuste de fatura não pode ser editado por aqui");
		}
		ValidatedRefs refs = validate(userId, request);
		transaction.update(refs.account().getId(), refs.category().getId(),
				invoiceIdFor(refs.account(), request.date()), request.description().trim(),
				request.amount(), request.date(), request.type());
		transaction.updateTags(normalizeTags(request.tags()));
		return toResponse(transaction, refs);
	}

	private Set<String> normalizeTags(List<String> tags) {
		if (tags == null) {
			return Set.of();
		}
		return tags.stream()
				.map(String::trim)
				.filter(tag -> !tag.isEmpty())
				.map(String::toLowerCase)
				.collect(Collectors.toSet());
	}

	@Transactional
	public void delete(UUID userId, UUID transactionId, String scope) {
		Transaction transaction = findOwned(userId, transactionId);
		if ("group".equals(scope) && transaction.getInstallmentGroupId() != null) {
			transactionRepository.deleteAll(
					transactionRepository.findAllByUserIdAndInstallmentGroupIdAndDateGreaterThanEqual(
							userId, transaction.getInstallmentGroupId(), transaction.getDate()));
			return;
		}
		transactionRepository.delete(transaction);
	}

	/** Marca/desmarca o pagamento (usado no checkbox "pago?" dos fixos). */
	@Transactional
	public TransactionResponse setPaid(UUID userId, UUID transactionId, boolean paid) {
		Transaction transaction = findOwned(userId, transactionId);
		transaction.markPaid(paid);
		Account account = accountRepository.findByIdAndUserId(transaction.getAccountId(), userId).orElse(null);
		Category category = transaction.getCategoryId() != null
				? categoryRepository.findByIdAndUserId(transaction.getCategoryId(), userId).orElse(null)
				: null;
		return toResponse(transaction, new ValidatedRefs(account, category));
	}

	private Transaction findOwned(UUID userId, UUID transactionId) {
		return transactionRepository.findByIdAndUserId(transactionId, userId)
				.orElseThrow(() -> new NotFoundException("Transação não encontrada"));
	}

	private ValidatedRefs validate(UUID userId, TransactionRequest request) {
		if (request.type() == TransactionType.INVOICE_ADJUSTMENT) {
			throw new BusinessException("Tipo reservado ao fechamento de fatura");
		}
		Account account = accountRepository.findByIdAndUserId(request.accountId(), userId)
				.orElseThrow(() -> new NotFoundException("Conta não encontrada"));
		Category category = categoryRepository.findByIdAndUserId(request.categoryId(), userId)
				.orElseThrow(() -> new NotFoundException("Categoria não encontrada"));
		CategoryKind expectedKind = request.type() == TransactionType.EXPENSE
				? CategoryKind.EXPENSE
				: CategoryKind.INCOME;
		if (category.getKind() != expectedKind) {
			throw new BusinessException("Categoria de "
					+ (category.getKind() == CategoryKind.EXPENSE ? "gasto" : "entrada")
					+ " não pode ser usada em lançamento de "
					+ (request.type() == TransactionType.EXPENSE ? "gasto" : "entrada"));
		}
		return new ValidatedRefs(account, category);
	}

	private UUID invoiceIdFor(Account account, LocalDate date) {
		if (account.getType() != AccountType.CREDIT_CARD) {
			return null;
		}
		return cardInvoiceService.getOrCreateInvoiceFor(account, date).getId();
	}

	private TransactionResponse toResponse(Transaction transaction, ValidatedRefs refs) {
		LocalDate invoiceMonth = transaction.getInvoiceId() != null
				? cardInvoiceRepository.findById(transaction.getInvoiceId()).map(CardInvoice::getMonth).orElse(null)
				: null;
		return TransactionResponse.from(transaction, refs.account(), refs.category(), invoiceMonth);
	}

	private <E> Map<UUID, E> byId(Page<Transaction> transactions, Function<Transaction, UUID> idExtractor,
			Function<Iterable<UUID>, Iterable<E>> loader, Function<E, UUID> entityId) {
		var ids = transactions.getContent().stream()
				.map(idExtractor)
				.filter(Objects::nonNull)
				.collect(Collectors.toSet());
		if (ids.isEmpty()) {
			return Map.of();
		}
		var result = new java.util.HashMap<UUID, E>();
		loader.apply(ids).forEach(entity -> result.put(entityId.apply(entity), entity));
		return result;
	}

	private record ValidatedRefs(Account account, Category category) {
	}

}
