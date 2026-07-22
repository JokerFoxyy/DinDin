package com.guaranin.api.account;

import com.guaranin.api.auth.refresh.RefreshTokenRepository;
import com.guaranin.api.category.CategoryRepository;
import com.guaranin.api.common.error.NotFoundException;
import com.guaranin.api.goal.GoalRepository;
import com.guaranin.api.invoice.CardInvoiceRepository;
import com.guaranin.api.investment.InvestmentRepository;
import com.guaranin.api.recurring.RecurringTransactionRepository;
import com.guaranin.api.transaction.TransactionRepository;
import com.guaranin.api.user.User;
import com.guaranin.api.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Direitos do titular (LGPD Art. 18): exportação (portabilidade) e eliminação de todos
 * os dados pessoais vinculados ao usuário autenticado.
 */
@Service
public class UserDataService {

	private final UserRepository userRepository;
	private final AccountRepository accountRepository;
	private final CategoryRepository categoryRepository;
	private final TransactionRepository transactionRepository;
	private final CardInvoiceRepository cardInvoiceRepository;
	private final RecurringTransactionRepository recurringRepository;
	private final RefreshTokenRepository refreshTokenRepository;
	private final InvestmentRepository investmentRepository;
	private final GoalRepository goalRepository;

	public UserDataService(UserRepository userRepository, AccountRepository accountRepository,
			CategoryRepository categoryRepository, TransactionRepository transactionRepository,
			CardInvoiceRepository cardInvoiceRepository, RecurringTransactionRepository recurringRepository,
			RefreshTokenRepository refreshTokenRepository, InvestmentRepository investmentRepository,
			GoalRepository goalRepository) {
		this.userRepository = userRepository;
		this.accountRepository = accountRepository;
		this.categoryRepository = categoryRepository;
		this.transactionRepository = transactionRepository;
		this.cardInvoiceRepository = cardInvoiceRepository;
		this.recurringRepository = recurringRepository;
		this.refreshTokenRepository = refreshTokenRepository;
		this.investmentRepository = investmentRepository;
		this.goalRepository = goalRepository;
	}

	@Transactional(readOnly = true)
	public Map<String, Object> export(UUID userId) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new NotFoundException("Usuário não encontrado"));
		List<Account> accounts = accountRepository.findAllByUserIdOrderByNameAsc(userId);
		List<UUID> accountIds = accounts.stream().map(Account::getId).toList();

		Map<String, Object> export = new LinkedHashMap<>();
		export.put("exportedAt", java.time.Instant.now().toString());
		export.put("user", Map.of("id", user.getId(), "email", user.getEmail(),
				"createdAt", user.getCreatedAt().toString()));
		export.put("accounts", accounts.stream().map(a -> Map.of(
				"id", a.getId(), "name", a.getName(), "type", a.getType(),
				"closingDay", nullSafe(a.getClosingDay()), "dueDay", nullSafe(a.getDueDay()))).toList());
		export.put("categories", categoryRepository.findAllByUserIdOrderByNameAsc(userId).stream().map(c -> Map.of(
				"id", c.getId(), "name", c.getName(), "kind", c.getKind(),
				"icon", nullSafe(c.getIcon()), "color", nullSafe(c.getColor()))).toList());
		export.put("transactions", transactionRepository.findAllByUserIdOrderByDateAsc(userId).stream().map(t -> Map.of(
				"id", t.getId(), "date", t.getDate().toString(), "description", t.getDescription(),
				"amount", t.getAmount(), "type", t.getType(), "accountId", t.getAccountId(),
				"categoryId", nullSafe(t.getCategoryId()), "invoiceId", nullSafe(t.getInvoiceId()))).toList());
		export.put("cardInvoices", cardInvoiceRepository.findByAccountIdIn(accountIds).stream().map(i -> Map.of(
				"id", i.getId(), "accountId", i.getAccountId(), "month", i.getMonth().toString(),
				"status", i.getStatus())).toList());
		export.put("recurringTransactions", recurringRepository.findAllByUserIdOrderByDescriptionAsc(userId).stream()
				.map(r -> Map.of("id", r.getId(), "description", r.getDescription(), "amount", r.getAmount(),
						"type", r.getType(), "dayOfMonth", r.getDayOfMonth(), "active", r.isActive(),
						"endDate", nullSafe(r.getEndDate() == null ? null : r.getEndDate().toString()))).toList());
		export.put("investments", investmentRepository.findAllByUserIdOrderByCreatedAtAsc(userId).stream()
				.map(i -> Map.of("id", i.getId(), "name", i.getName(), "class", i.getAssetClass(),
						"institution", i.getInstitution())).toList());
		export.put("goals", goalRepository.findAllByUserIdOrderByCreatedAtAsc(userId).stream()
				.map(g -> Map.of("id", g.getId(), "name", g.getName(), "targetAmount", g.getTargetAmount(),
						"targetDate", g.getTargetDate().toString())).toList());
		return export;
	}

	@Transactional
	public void deleteAccount(UUID userId) {
		if (!userRepository.existsById(userId)) {
			throw new NotFoundException("Usuário não encontrado");
		}
		List<UUID> accountIds = accountRepository.findAllByUserIdOrderByNameAsc(userId).stream()
				.map(Account::getId).toList();
		// transações referenciam recurring_transactions e faturas; apagar transações primeiro
		transactionRepository.deleteByUserId(userId);
		recurringRepository.deleteByUserId(userId);
		if (!accountIds.isEmpty()) {
			cardInvoiceRepository.deleteByAccountIdIn(accountIds);
		}
		categoryRepository.deleteByUserId(userId);
		accountRepository.deleteByUserId(userId);
		investmentRepository.deleteByUserId(userId);
		goalRepository.deleteByUserId(userId);
		refreshTokenRepository.deleteByUserId(userId);
		userRepository.deleteById(userId);
	}

	private Object nullSafe(Object value) {
		return value == null ? "" : value;
	}

}
