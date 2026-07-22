package com.poupito.api.account;

import com.poupito.api.account.dto.AccountRequest;
import com.poupito.api.account.dto.AccountResponse;
import com.poupito.api.common.error.BusinessException;
import com.poupito.api.common.error.NotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

	private final UUID userId = UUID.randomUUID();

	@Mock
	private AccountRepository accountRepository;

	@InjectMocks
	private AccountService accountService;

	@Test
	void shouldCreateCheckingAccount_whenRequestIsValid() {
		when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

		AccountResponse response = accountService.create(userId,
				new AccountRequest(" Uniclass ", AccountType.CHECKING, null, null));

		assertThat(response.name()).isEqualTo("Uniclass");
		assertThat(response.type()).isEqualTo(AccountType.CHECKING);
	}

	@Test
	void shouldNormalizeInvoiceDaysToNull_whenAccountIsNotCreditCard() {
		when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

		AccountResponse response = accountService.create(userId,
				new AccountRequest("Carteira", AccountType.CASH, 10, 20));

		assertThat(response.closingDay()).isNull();
		assertThat(response.dueDay()).isNull();
	}

	@Test
	void shouldCreateCreditCard_whenInvoiceDaysArePresent() {
		when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

		AccountResponse response = accountService.create(userId,
				new AccountRequest("Nubank", AccountType.CREDIT_CARD, 28, 7));

		assertThat(response.closingDay()).isEqualTo(28);
		assertThat(response.dueDay()).isEqualTo(7);
		ArgumentCaptor<Account> saved = ArgumentCaptor.forClass(Account.class);
		verify(accountRepository).save(saved.capture());
		assertThat(saved.getValue().getUserId()).isEqualTo(userId);
	}

	@Test
	void shouldThrowBusinessException_whenCreditCardHasNoInvoiceDays() {
		assertThatThrownBy(() -> accountService.create(userId,
				new AccountRequest("Nubank", AccountType.CREDIT_CARD, null, 7)))
				.isInstanceOf(BusinessException.class);
		verify(accountRepository, never()).save(any());
	}

	@Test
	void shouldUpdateAccount_whenOwnedByUser() {
		Account account = new Account(userId, "Nubank", AccountType.CREDIT_CARD, 28, 7);
		when(accountRepository.findByIdAndUserId(any(), any())).thenReturn(Optional.of(account));

		AccountResponse response = accountService.update(userId, UUID.randomUUID(),
				new AccountRequest("Nubank Ultravioleta", AccountType.CREDIT_CARD, 25, 4));

		assertThat(response.name()).isEqualTo("Nubank Ultravioleta");
		assertThat(response.closingDay()).isEqualTo(25);
	}

	@Test
	void shouldThrowNotFound_whenUpdatingAccountOfAnotherUser() {
		when(accountRepository.findByIdAndUserId(any(), any())).thenReturn(Optional.empty());

		assertThatThrownBy(() -> accountService.update(userId, UUID.randomUUID(),
				new AccountRequest("Conta", AccountType.CHECKING, null, null)))
				.isInstanceOf(NotFoundException.class);
	}

	@Test
	void shouldDeleteAccount_whenOwnedByUser() {
		Account account = new Account(userId, "Carteira", AccountType.CASH, null, null);
		when(accountRepository.findByIdAndUserId(any(), any())).thenReturn(Optional.of(account));

		accountService.delete(userId, UUID.randomUUID());

		verify(accountRepository).delete(account);
	}

	@Test
	void shouldListAccountsOfUser_whenCalled() {
		when(accountRepository.findAllByUserIdOrderByNameAsc(userId))
				.thenReturn(List.of(new Account(userId, "Uniclass", AccountType.CHECKING, null, null)));

		List<AccountResponse> accounts = accountService.list(userId);

		assertThat(accounts).hasSize(1);
		assertThat(accounts.getFirst().name()).isEqualTo("Uniclass");
	}

}
