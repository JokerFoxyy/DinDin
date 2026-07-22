package com.guaranin.api.common.error;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(int status, String message, Map<String, String> fieldErrors, Instant timestamp) {

	public static ApiError of(int status, String message) {
		return new ApiError(status, message, null, Instant.now());
	}

	public static ApiError validation(Map<String, String> fieldErrors) {
		return new ApiError(400, "Erro de validação", fieldErrors, Instant.now());
	}

}
