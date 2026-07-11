package com.dindin.api.common.error;

public class InvalidRefreshTokenException extends RuntimeException {

	public InvalidRefreshTokenException() {
		super("Sessão inválida ou expirada");
	}

}
