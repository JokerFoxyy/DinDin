package com.guaranin.api.importer;

import com.guaranin.api.account.Account;
import com.guaranin.api.account.AccountRepository;
import com.guaranin.api.account.AccountType;
import com.guaranin.api.category.Category;
import com.guaranin.api.category.CategoryKind;
import com.guaranin.api.category.CategoryRepository;
import com.guaranin.api.importer.dto.AccountMappingChoice;
import com.guaranin.api.importer.dto.CategoryMappingChoice;
import com.guaranin.api.importer.dto.ImportCommitResponse;
import com.guaranin.api.importer.dto.ImportMappingRequest;
import com.guaranin.api.importer.dto.ImportPreviewResponse;
import com.guaranin.api.importer.dto.ImportRowResponse;
import com.guaranin.api.transaction.Transaction;
import com.guaranin.api.transaction.TransactionRepository;
import com.guaranin.api.transaction.TransactionType;
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
	private final CategoryRepository categoryRepository;
	private final TransactionRepository transactionRepository;

	public ImportService(SpreadsheetParser parser, AccountRepository accountRepository,
			CategoryRepository categoryRepository, TransactionRepository transactionRepository) {
		this.parser = parser;
		this.accountRepository = accountRepository;
		this.categoryRepository = categoryRepository;
		this.transactionRepository = transactionRepository;
	}

	@Transactional(readOnly = true)
	public ImportPreviewResponse preview(UUID userId, InputStream file, int year) throws IOException {
		List<ImportRow> rows = parser.parse(file, year);

		Map<String, Account> existingAccounts = byNormalizedName(accountRepository.findAllByUserIdOrderByNameAsc(userId),
				Account::getName);
		Map<String, Category> existingCategories = byNormalizedName(
				categoryRepository.findAllByUserIdOrderByNameAsc(userId), Category::getName);

		List<String> unmatchedAccounts = rows.stream()
				.map(ImportRow::accountNameRaw)
				.distinct()
				.filter(name -> !existingAccounts.containsKey(normalize(name)))
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
		Map<String, Category> categories = new HashMap<>(
				byNormalizedName(categoryRepository.findAllByUserIdOrderByNameAsc(userId), Category::getName));

		int accountsCreated = 0;
		int categoriesCreated = 0;
		int created = 0;
		int skipped = 0;

		for (ImportRow row : rows) {
			Account account = accounts.get(normalize(row.accountNameRaw()));
			if (account == null) {
				AccountMappingChoice accountChoice = mapping.accounts().get(row.accountNameRaw());
				boolean creatingAccount = accountChoice == null || accountChoice.existingAccountId() == null;
				account = resolveAccount(userId, row.accountNameRaw(), mapping);
				accounts.put(normalize(row.accountNameRaw()), account);
				if (creatingAccount) {
					accountsCreated++;
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

			boolean duplicate = transactionRepository.existsByUserIdAndAccountIdAndDescriptionAndAmountAndDateAndType(
					userId, account.getId(), row.description(), row.amount(), row.date(), row.type());
			if (duplicate) {
				skipped++;
				continue;
			}

			transactionRepository.save(new Transaction(userId, account.getId(),
					category != null ? category.getId() : null, null, row.description(), row.amount(), row.date(),
					row.type()));
			created++;
		}

		return new ImportCommitResponse(created, skipped, accountsCreated, categoriesCreated);
	}

	private Account resolveAccount(UUID userId, String rawName, ImportMappingRequest mapping) {
		AccountMappingChoice choice = mapping.accounts().get(rawName);
		if (choice != null && choice.existingAccountId() != null) {
			return accountRepository.findByIdAndUserId(choice.existingAccountId(), userId)
					.orElseThrow(() -> new IllegalArgumentException("Conta não encontrada: " + choice.existingAccountId()));
		}
		AccountType type = choice != null && choice.createType() != null ? choice.createType() : AccountType.CHECKING;
		return accountRepository.save(new Account(userId, rawName, type, null, null));
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

}
