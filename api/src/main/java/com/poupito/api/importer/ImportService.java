package com.poupito.api.importer;

import com.poupito.api.account.Account;
import com.poupito.api.account.AccountRepository;
import com.poupito.api.account.AccountType;
import com.poupito.api.card.Card;
import com.poupito.api.card.CardRepository;
import com.poupito.api.category.Category;
import com.poupito.api.category.CategoryKind;
import com.poupito.api.category.CategoryRepository;
import com.poupito.api.common.error.BusinessException;
import com.poupito.api.importer.dto.AccountMappingChoice;
import com.poupito.api.importer.dto.CategoryMappingChoice;
import com.poupito.api.importer.dto.ImportCommitResponse;
import com.poupito.api.importer.dto.ImportMappingRequest;
import com.poupito.api.importer.dto.ImportPreviewResponse;
import com.poupito.api.importer.dto.ImportRowResponse;
import com.poupito.api.invoice.CardInvoiceService;
import com.poupito.api.transaction.Transaction;
import com.poupito.api.transaction.TransactionRepository;
import com.poupito.api.transaction.TransactionType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class ImportService {

	private final SpreadsheetParser parser;
	private final AccountRepository accountRepository;
	private final CardRepository cardRepository;
	private final CategoryRepository categoryRepository;
	private final TransactionRepository transactionRepository;
	private final CardInvoiceService cardInvoiceService;

	public ImportService(SpreadsheetParser parser, AccountRepository accountRepository, CardRepository cardRepository,
			CategoryRepository categoryRepository, TransactionRepository transactionRepository,
			CardInvoiceService cardInvoiceService) {
		this.parser = parser;
		this.accountRepository = accountRepository;
		this.cardRepository = cardRepository;
		this.categoryRepository = categoryRepository;
		this.transactionRepository = transactionRepository;
		this.cardInvoiceService = cardInvoiceService;
	}

	@Transactional(readOnly = true)
	public ImportPreviewResponse preview(UUID userId, InputStream file, int year) throws IOException {
		List<ImportRow> rows = parser.parse(file, year);

		Map<String, Account> existingAccounts = byNormalizedName(accountRepository.findAllByUserIdOrderByNameAsc(userId),
				Account::getName);
		Map<String, Card> existingCards = byNormalizedName(cardRepository.findAllByUserIdOrderByNameAsc(userId),
				Card::getName);
		Map<String, Category> existingCategories = byNormalizedName(
				categoryRepository.findAllByUserIdOrderByNameAsc(userId), Category::getName);

		List<String> unmatchedAccounts = rows.stream()
				.map(ImportRow::accountNameRaw)
				.distinct()
				.filter(name -> !existingAccounts.containsKey(normalize(name))
						&& !existingCards.containsKey(normalize(name)))
				.sorted()
				.toList();
		List<String> unmatchedCategories = rows.stream()
				.map(ImportRow::categoryNameRaw)
				.filter(java.util.Objects::nonNull)
				.distinct()
				.filter(name -> !existingCategories.containsKey(normalize(name)))
				.sorted()
				.toList();

		return new ImportPreviewResponse(rows.stream().map(ImportRowResponse::from).toList(),
				unmatchedAccounts, unmatchedCategories);
	}

	@Transactional
	public ImportCommitResponse commit(UUID userId, InputStream file, int year, ImportMappingRequest mapping)
			throws IOException {
		List<ImportRow> rows = parser.parse(file, year);

		Map<String, Account> accounts = new HashMap<>(
				byNormalizedName(accountRepository.findAllByUserIdOrderByNameAsc(userId), Account::getName));
		Map<String, Card> cards = new HashMap<>(
				byNormalizedName(cardRepository.findAllByUserIdOrderByNameAsc(userId), Card::getName));
		Map<String, Category> categories = new HashMap<>(
				byNormalizedName(categoryRepository.findAllByUserIdOrderByNameAsc(userId), Category::getName));
		Map<String, PaymentTarget> resolvedTargets = new HashMap<>();

		int accountsCreated = 0;
		int cardsCreated = 0;
		int categoriesCreated = 0;
		int created = 0;
		int skipped = 0;

		for (ImportRow row : rows) {
			PaymentTarget target = resolvedTargets.get(normalize(row.accountNameRaw()));
			if (target == null) {
				target = resolveTarget(userId, row.accountNameRaw(), mapping, accounts, cards);
				resolvedTargets.put(normalize(row.accountNameRaw()), target);
				if (target.createdAccount()) {
					accountsCreated++;
				}
				if (target.createdCard()) {
					cardsCreated++;
				}
			}

			Category category = null;
			if (row.categoryNameRaw() != null) {
				category = categories.get(normalize(row.categoryNameRaw()));
				if (category == null) {
					CategoryMappingChoice categoryChoice = mapping.categories().get(row.categoryNameRaw());
					boolean creatingCategory = categoryChoice == null || categoryChoice.existingCategoryId() == null;
					category = resolveCategory(userId, row.categoryNameRaw(), row.type(), mapping);
					categories.put(normalize(row.categoryNameRaw()), category);
					if (creatingCategory) {
						categoriesCreated++;
					}
				}
			}

			UUID categoryId = category != null ? category.getId() : null;
			if (target.card() != null) {
				// cartão só recebe gastos (entrada não existe no crédito) — cai na conta vinculada
				if (row.type() == TransactionType.INCOME) {
					if (saveAccountRow(userId, resolveCardAccount(userId, target.card()), categoryId, row)) {
						created++;
					} else {
						skipped++;
					}
				} else if (saveCardRow(userId, target.card(), categoryId, row)) {
					created++;
				} else {
					skipped++;
				}
			} else if (saveAccountRow(userId, target.account(), categoryId, row)) {
				created++;
			} else {
				skipped++;
			}
		}

		return new ImportCommitResponse(created, skipped, accountsCreated + cardsCreated, categoriesCreated);
	}

	private boolean saveAccountRow(UUID userId, Account account, UUID categoryId, ImportRow row) {
		if (transactionRepository.existsByUserIdAndAccountIdAndDescriptionAndAmountAndDateAndType(
				userId, account.getId(), row.description(), row.amount(), row.date(), row.type())) {
			return false;
		}
		transactionRepository.save(Transaction.forAccount(userId, account.getId(), categoryId,
				row.description(), row.amount(), row.date(), row.type()));
		return true;
	}

	private boolean saveCardRow(UUID userId, Card card, UUID categoryId, ImportRow row) {
		if (transactionRepository.existsByUserIdAndCardIdAndDescriptionAndAmountAndDateAndType(
				userId, card.getId(), row.description(), row.amount(), row.date(), row.type())) {
			return false;
		}
		UUID invoiceId = cardInvoiceService.getOrCreateInvoiceFor(card, row.date()).getId();
		transactionRepository.save(Transaction.forCard(userId, card.getId(), categoryId, invoiceId,
				row.description(), row.amount(), row.date(), row.type()));
		return true;
	}

	private Account resolveCardAccount(UUID userId, Card card) {
		return accountRepository.findByIdAndUserId(card.getAccountId(), userId)
				.orElseThrow(() -> new IllegalStateException("Conta vinculada do cartão não encontrada"));
	}

	/** Resolve para onde vai um nome de "conta" da planilha: conta existente/nova ou cartão existente/novo. */
	private PaymentTarget resolveTarget(UUID userId, String rawName, ImportMappingRequest mapping,
			Map<String, Account> accounts, Map<String, Card> cards) {
		String key = normalize(rawName);
		Account existingAccount = accounts.get(key);
		if (existingAccount != null) {
			return PaymentTarget.account(existingAccount, false);
		}
		Card existingCard = cards.get(key);
		if (existingCard != null) {
			return PaymentTarget.card(existingCard, false);
		}

		AccountMappingChoice choice = mapping.accounts().get(rawName);
		if (choice != null && choice.existingAccountId() != null) {
			Account account = accountRepository.findByIdAndUserId(choice.existingAccountId(), userId)
					.orElseThrow(() -> new IllegalArgumentException("Conta não encontrada: " + choice.existingAccountId()));
			accounts.put(key, account);
			return PaymentTarget.account(account, false);
		}
		if (choice != null && choice.existingCardId() != null) {
			Card card = cardRepository.findByIdAndUserId(choice.existingCardId(), userId)
					.orElseThrow(() -> new IllegalArgumentException("Cartão não encontrado: " + choice.existingCardId()));
			cards.put(key, card);
			return PaymentTarget.card(card, false);
		}
		if (choice != null && choice.createCard() != null) {
			AccountMappingChoice.CreateCardChoice cc = choice.createCard();
			if (cc.accountId() == null || cc.closingDay() == null || cc.dueDay() == null) {
				throw new BusinessException("Cartão exige conta vinculada, dia de fechamento e de vencimento: " + rawName);
			}
			Account linked = accountRepository.findByIdAndUserId(cc.accountId(), userId)
					.orElseThrow(() -> new IllegalArgumentException("Conta vinculada não encontrada: " + cc.accountId()));
			Card card = cardRepository.save(new Card(userId, linked.getId(), rawName, cc.closingDay(), cc.dueDay()));
			cards.put(key, card);
			return PaymentTarget.card(card, true);
		}

		AccountType type = choice != null && choice.createType() != null ? choice.createType() : AccountType.CHECKING;
		Account account = accountRepository.save(new Account(userId, rawName, type));
		accounts.put(key, account);
		return PaymentTarget.account(account, true);
	}

	private Category resolveCategory(UUID userId, String rawName, TransactionType type, ImportMappingRequest mapping) {
		CategoryMappingChoice choice = mapping.categories().get(rawName);
		if (choice != null && choice.existingCategoryId() != null) {
			return categoryRepository.findByIdAndUserId(choice.existingCategoryId(), userId)
					.orElseThrow(() -> new IllegalArgumentException("Categoria não encontrada: " + choice.existingCategoryId()));
		}
		CategoryKind kind;
		if (choice != null && choice.createKind() != null) {
			kind = choice.createKind();
		} else {
			kind = type == TransactionType.INCOME ? CategoryKind.INCOME : CategoryKind.EXPENSE;
		}
		return categoryRepository.save(new Category(userId, rawName, null, null, kind));
	}

	private <T> Map<String, T> byNormalizedName(List<T> items, java.util.function.Function<T, String> nameExtractor) {
		Map<String, T> map = new HashMap<>();
		for (T item : items) {
			map.put(normalize(nameExtractor.apply(item)), item);
		}
		return map;
	}

	private String normalize(String value) {
		return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
	}

	private record PaymentTarget(Account account, Card card, boolean createdAccount, boolean createdCard) {
		static PaymentTarget account(Account account, boolean created) {
			return new PaymentTarget(account, null, created, false);
		}

		static PaymentTarget card(Card card, boolean created) {
			return new PaymentTarget(null, card, false, created);
		}
	}

}
