package com.poupito.api.card;

import com.poupito.api.account.Account;
import com.poupito.api.account.AccountRepository;
import com.poupito.api.card.dto.CardRequest;
import com.poupito.api.card.dto.CardResponse;
import com.poupito.api.common.error.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CardService {

	private final CardRepository cardRepository;
	private final AccountRepository accountRepository;

	public CardService(CardRepository cardRepository, AccountRepository accountRepository) {
		this.cardRepository = cardRepository;
		this.accountRepository = accountRepository;
	}

	@Transactional(readOnly = true)
	public List<CardResponse> list(UUID userId) {
		List<Card> cards = cardRepository.findAllByUserIdOrderByNameAsc(userId);
		Map<UUID, Account> accounts = accountRepository.findAllByUserIdOrderByNameAsc(userId).stream()
				.collect(Collectors.toMap(Account::getId, Function.identity()));
		return cards.stream()
				.map(card -> CardResponse.from(card, accounts.get(card.getAccountId())))
				.toList();
	}

	@Transactional
	public CardResponse create(UUID userId, CardRequest request) {
		Account account = ownedAccount(userId, request.accountId());
		Card card = new Card(userId, account.getId(), request.name().trim(),
				request.closingDay(), request.dueDay());
		return CardResponse.from(cardRepository.save(card), account);
	}

	@Transactional
	public CardResponse update(UUID userId, UUID cardId, CardRequest request) {
		Card card = findOwned(userId, cardId);
		Account account = ownedAccount(userId, request.accountId());
		card.update(account.getId(), request.name().trim(), request.closingDay(), request.dueDay());
		return CardResponse.from(card, account);
	}

	/** Delete falha com 409 se houver transações/faturas no cartão (FK), igual a contas/categorias. */
	@Transactional
	public void delete(UUID userId, UUID cardId) {
		cardRepository.delete(findOwned(userId, cardId));
	}

	private Card findOwned(UUID userId, UUID cardId) {
		return cardRepository.findByIdAndUserId(cardId, userId)
				.orElseThrow(() -> new NotFoundException("Cartão não encontrado"));
	}

	private Account ownedAccount(UUID userId, UUID accountId) {
		return accountRepository.findByIdAndUserId(accountId, userId)
				.orElseThrow(() -> new NotFoundException("Conta não encontrada"));
	}

}
