package com.poupito.api.invoice;

import com.poupito.api.account.Account;
import com.poupito.api.account.AccountRepository;
import com.poupito.api.account.AccountType;
import com.poupito.api.category.Category;
import com.poupito.api.category.CategoryRepository;
import com.poupito.api.common.error.BusinessException;
import com.poupito.api.common.error.NotFoundException;
import com.poupito.api.invoice.dto.InvoiceDetailResponse;
import com.poupito.api.invoice.dto.InvoiceLine;
import com.poupito.api.invoice.dto.InvoiceSummaryResponse;
import com.poupito.api.transaction.Transaction;
import com.poupito.api.transaction.TransactionRepository;
import com.poupito.api.transaction.TransactionType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class InvoiceService {

	private static final String ADJUSTMENT_DESCRIPTION = "Ajuste de fatura";

	private final CardInvoiceRepository cardInvoiceRepository;
	private final TransactionRepository transactionRepository;
	private final AccountRepository accountRepository;
	private final CategoryRepository categoryRepository;

	public InvoiceService(CardInvoiceRepository cardInvoiceRepository, TransactionRepository transactionRepository,
			AccountRepository accountRepository, CategoryRepository categoryRepository) {
		this.cardInvoiceRepository = cardInvoiceRepository;
		this.transactionRepository = transactionRepository;
		this.accountRepository = accountRepository;
		this.categoryRepository = categoryRepository;
	}

	@Transactional
	public List<InvoiceSummaryResponse> list(UUID userId, YearMonth month) {
		List<Account> cards = accountRepository.findAllByUserIdOrderByNameAsc(userId).stream()
				.filter(account -> account.getType() == AccountType.CREDIT_CARD)
				.toList();
		if (cards.isEmpty()) {
			return List.of();
		}
		Map<UUID, Account> byId = cards.stream().collect(Collectors.toMap(Account::getId, Function.identity()));
		return cardInvoiceRepository
				.findByAccountIdInAndMonthOrderByCreatedAtAsc(byId.keySet(), month.atDay(1)).stream()
				.map(invoice -> {
					// mantém o ajuste consistente conforme os gastos são detalhados
					if (invoice.getStatus() == InvoiceStatus.CLOSED && invoice.getDeclaredTotal() != null) {
						recomputeAdjustment(invoice, byId.get(invoice.getAccountId()).getUserId());
					}
					return summary(invoice, byId.get(invoice.getAccountId()));
				})
				.toList();
	}

	@Transactional
	public InvoiceDetailResponse getDetail(UUID userId, UUID invoiceId) {
		Account card = ownedCard(userId, invoiceId);
		CardInvoice invoice = cardInvoiceRepository.findById(invoiceId).orElseThrow(this::notFound);
		// reconcilia o ajuste: ao detalhar gastos, a diferença (e o ajuste) diminui
		if (invoice.getStatus() == InvoiceStatus.CLOSED && invoice.getDeclaredTotal() != null) {
			recomputeAdjustment(invoice, card.getUserId());
		}
		return detail(invoice, card);
	}

	@Transactional
	public InvoiceDetailResponse close(UUID userId, UUID invoiceId, BigDecimal declaredTotal) {
		Account card = ownedCard(userId, invoiceId);
		CardInvoice invoice = cardInvoiceRepository.findById(invoiceId).orElseThrow(this::notFound);
		if (invoice.getStatus() != InvoiceStatus.OPEN) {
			throw new BusinessException("Só uma fatura aberta pode ser fechada");
		}
		invoice.close(declaredTotal);
		recomputeAdjustment(invoice, card.getUserId());
		return detail(invoice, card);
	}

	@Transactional
	public InvoiceDetailResponse pay(UUID userId, UUID invoiceId) {
		Account card = ownedCard(userId, invoiceId);
		CardInvoice invoice = cardInvoiceRepository.findById(invoiceId).orElseThrow(this::notFound);
		if (invoice.getStatus() != InvoiceStatus.CLOSED) {
			throw new BusinessException("Só uma fatura fechada pode ser paga");
		}
		invoice.pay();
		return detail(invoice, card);
	}

	@Transactional
	public InvoiceDetailResponse reopen(UUID userId, UUID invoiceId) {
		Account card = ownedCard(userId, invoiceId);
		CardInvoice invoice = cardInvoiceRepository.findById(invoiceId).orElseThrow(this::notFound);
		if (invoice.getStatus() == InvoiceStatus.OPEN) {
			throw new BusinessException("A fatura já está aberta");
		}
		invoice.reopen();
		return detail(invoice, card);
	}

	private void recomputeAdjustment(CardInvoice invoice, UUID userId) {
		List<Transaction> transactions = transactionRepository.findAllByInvoiceIdOrderByDateAsc(invoice.getId());
		BigDecimal launched = transactions.stream()
				.filter(transaction -> transaction.getType() != TransactionType.INVOICE_ADJUSTMENT)
				.map(Transaction::getAmount)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		Transaction adjustment = transactions.stream()
				.filter(transaction -> transaction.getType() == TransactionType.INVOICE_ADJUSTMENT)
				.findFirst()
				.orElse(null);
		BigDecimal diff = invoice.getDeclaredTotal().subtract(launched);

		if (diff.compareTo(BigDecimal.ZERO) > 0) {
			if (adjustment != null) {
				adjustment.update(invoice.getAccountId(), null, invoice.getId(), ADJUSTMENT_DESCRIPTION,
						diff, invoice.getClosingDate(), TransactionType.INVOICE_ADJUSTMENT);
			} else {
				transactionRepository.save(new Transaction(userId, invoice.getAccountId(), null, invoice.getId(),
						ADJUSTMENT_DESCRIPTION, diff, invoice.getClosingDate(), TransactionType.INVOICE_ADJUSTMENT));
			}
		} else if (adjustment != null) {
			transactionRepository.delete(adjustment);
		}
	}

	private Account ownedCard(UUID userId, UUID invoiceId) {
		CardInvoice invoice = cardInvoiceRepository.findById(invoiceId).orElseThrow(this::notFound);
		return accountRepository.findByIdAndUserId(invoice.getAccountId(), userId).orElseThrow(this::notFound);
	}

	private InvoiceSummaryResponse summary(CardInvoice invoice, Account card) {
		List<Transaction> transactions = transactionRepository.findAllByInvoiceIdOrderByDateAsc(invoice.getId());
		BigDecimal launched = transactions.stream()
				.filter(transaction -> transaction.getType() != TransactionType.INVOICE_ADJUSTMENT)
				.map(Transaction::getAmount)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal adjustment = transactions.stream()
				.filter(transaction -> transaction.getType() == TransactionType.INVOICE_ADJUSTMENT)
				.map(Transaction::getAmount)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		return new InvoiceSummaryResponse(invoice.getId(), invoice.getAccountId(),
				card != null ? card.getName() : null, invoice.getMonth(), invoice.getClosingDate(),
				invoice.getDueDate(), launched, invoice.getDeclaredTotal(), adjustment, invoice.getStatus());
	}

	private InvoiceDetailResponse detail(CardInvoice invoice, Account card) {
		List<Transaction> transactions = transactionRepository.findAllByInvoiceIdOrderByDateAsc(invoice.getId());
		Map<UUID, Category> categories = loadCategories(transactions);
		List<InvoiceLine> lines = transactions.stream().map(transaction -> {
			Category category = transaction.getCategoryId() != null
					? categories.get(transaction.getCategoryId())
					: null;
			return new InvoiceLine(transaction.getId(), transaction.getDate(), transaction.getDescription(),
					transaction.getAmount(), transaction.getType(),
					category != null ? category.getName() : null,
					category != null ? category.getIcon() : null,
					category != null ? category.getColor() : null);
		}).toList();
		return new InvoiceDetailResponse(summary(invoice, card), lines);
	}

	private Map<UUID, Category> loadCategories(List<Transaction> transactions) {
		var ids = transactions.stream()
				.map(Transaction::getCategoryId)
				.filter(Objects::nonNull)
				.collect(Collectors.toSet());
		if (ids.isEmpty()) {
			return Map.of();
		}
		return categoryRepository.findAllById(ids).stream()
				.collect(Collectors.toMap(Category::getId, Function.identity()));
	}

	private NotFoundException notFound() {
		return new NotFoundException("Fatura não encontrada");
	}

}
