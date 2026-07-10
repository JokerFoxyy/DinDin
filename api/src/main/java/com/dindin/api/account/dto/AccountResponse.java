package com.dindin.api.account.dto;

import com.dindin.api.account.Account;
import com.dindin.api.account.AccountType;

import java.util.UUID;

public record AccountResponse(UUID id, String name, AccountType type, Integer closingDay, Integer dueDay) {

	public static AccountResponse from(Account account) {
		return new AccountResponse(account.getId(), account.getName(), account.getType(),
				account.getClosingDay(), account.getDueDay());
	}

}
