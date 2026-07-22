package com.guaranin.api.common.error;

public class InvalidCredentialsException extends RuntimeException {

	public InvalidCredentialsException() {
		super("Email ou senha inválidos");
	}

}
