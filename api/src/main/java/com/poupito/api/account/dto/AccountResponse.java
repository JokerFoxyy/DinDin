package com.poupito.api.account.dto;

import com.poupito.api.account.Account;
import com.poupito.api.account.AccountType;

import java.util.UUID;

public record AccountResponse(UUID id, String name, AccountType type) {

	public static AccountResponse from(Account account) {
		return new AccountResponse(account.getId(), account.getName(), account.getType());
	}

}
