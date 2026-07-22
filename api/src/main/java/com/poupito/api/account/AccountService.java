package com.poupito.api.account;

import com.poupito.api.account.dto.AccountRequest;
import com.poupito.api.account.dto.AccountResponse;
import com.poupito.api.common.error.BusinessException;
import com.poupito.api.common.error.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class AccountService {

	private final AccountRepository accountRepository;

	public AccountService(AccountRepository accountRepository) {
		this.accountRepository = accountRepository;
	}

	@Transactional(readOnly = true)
	public List<AccountResponse> list(UUID userId) {
		return accountRepository.findAllByUserIdOrderByNameAsc(userId).stream()
				.map(AccountResponse::from)
				.toList();
	}

	@Transactional
	public AccountResponse create(UUID userId, AccountRequest request) {
		validateInvoiceDays(request);
		Account account = new Account(userId, request.name().trim(), request.type(),
				invoiceDay(request, request.closingDay()), invoiceDay(request, request.dueDay()));
		return AccountResponse.from(accountRepository.save(account));
	}

	@Transactional
	public AccountResponse update(UUID userId, UUID accountId, AccountRequest request) {
		validateInvoiceDays(request);
		Account account = findOwned(userId, accountId);
		account.update(request.name().trim(), request.type(),
				invoiceDay(request, request.closingDay()), invoiceDay(request, request.dueDay()));
		return AccountResponse.from(account);
	}

	@Transactional
	public void delete(UUID userId, UUID accountId) {
		accountRepository.delete(findOwned(userId, accountId));
	}

	private Account findOwned(UUID userId, UUID accountId) {
		return accountRepository.findByIdAndUserId(accountId, userId)
				.orElseThrow(() -> new NotFoundException("Conta não encontrada"));
	}

	private void validateInvoiceDays(AccountRequest request) {
		if (request.type() == AccountType.CREDIT_CARD
				&& (request.closingDay() == null || request.dueDay() == null)) {
			throw new BusinessException("Cartão de crédito exige dia de fechamento e de vencimento");
		}
	}

	/** Dias de fatura só fazem sentido para cartão; para os demais tipos são normalizados para null. */
	private Integer invoiceDay(AccountRequest request, Integer day) {
		return request.type() == AccountType.CREDIT_CARD ? day : null;
	}

}
