package com.guaranin.api.common.error;

public class ExternalServiceException extends RuntimeException {

	public ExternalServiceException(String message, Throwable cause) {
		super(message, cause);
	}

}
