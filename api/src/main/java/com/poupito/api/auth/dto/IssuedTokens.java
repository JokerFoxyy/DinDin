package com.poupito.api.auth.dto;

import java.util.UUID;

/** Resultado interno da autenticação: tokens a colocar em cookies + identidade do usuário. */
public record IssuedTokens(String accessToken, String refreshToken, UUID userId, String email) {

	public UserResponse user() {
		return new UserResponse(userId, email);
	}

}
