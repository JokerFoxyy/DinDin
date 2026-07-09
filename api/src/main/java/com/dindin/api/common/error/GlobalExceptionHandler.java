package com.dindin.api.common.error;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(MethodArgumentNotValidException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public ApiError handleValidation(MethodArgumentNotValidException ex) {
		Map<String, String> fields = new LinkedHashMap<>();
		for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
			fields.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
		}
		return ApiError.validation(fields);
	}

	@ExceptionHandler(EmailAlreadyUsedException.class)
	@ResponseStatus(HttpStatus.CONFLICT)
	public ApiError handleEmailAlreadyUsed(EmailAlreadyUsedException ex) {
		return ApiError.of(HttpStatus.CONFLICT.value(), ex.getMessage());
	}

	@ExceptionHandler(InvalidCredentialsException.class)
	@ResponseStatus(HttpStatus.UNAUTHORIZED)
	public ApiError handleInvalidCredentials(InvalidCredentialsException ex) {
		return ApiError.of(HttpStatus.UNAUTHORIZED.value(), ex.getMessage());
	}

	@ExceptionHandler(Exception.class)
	@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
	public ApiError handleUnexpected(Exception ex) {
		log.error("Erro não tratado", ex);
		return ApiError.of(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Erro interno do servidor");
	}

}
