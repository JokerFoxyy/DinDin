package com.financeia.api.common.error;

public class EmailAlreadyUsedException extends RuntimeException {

	public EmailAlreadyUsedException() {
		super("Email já cadastrado");
	}

}
