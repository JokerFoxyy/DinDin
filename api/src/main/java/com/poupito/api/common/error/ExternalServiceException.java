package com.poupito.api.common.error;

public class ExternalServiceException extends RuntimeException {

	public ExternalServiceException(String message, Throwable cause) {
		super(message, cause);
	}

}
