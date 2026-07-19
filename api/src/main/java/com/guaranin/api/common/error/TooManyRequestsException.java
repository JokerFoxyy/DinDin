package com.guaranin.api.common.error;

public class TooManyRequestsException extends RuntimeException {

	public TooManyRequestsException() {
		super("Muitas tentativas. Aguarde alguns instantes e tente novamente.");
	}

}
