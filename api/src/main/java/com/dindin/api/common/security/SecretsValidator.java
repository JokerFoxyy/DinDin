package com.dindin.api.common.security;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * Falha o startup em produção se segredos ainda forem os defaults de desenvolvimento
 * ou fracos demais. Em dev não interfere (permite subir com os defaults do compose).
 */
@Component
public class SecretsValidator {

	static final String DEV_JWT_SECRET = "dindin-dev-secret-do-not-use-in-production-0123456789";
	static final String DEV_DB_PASSWORD = "dindin-dev";
	private static final int MIN_SECRET_BYTES = 32;

	private final Environment environment;
	private final String jwtSecret;
	private final String dbPassword;

	public SecretsValidator(Environment environment,
			@Value("${app.jwt.secret}") String jwtSecret,
			@Value("${spring.datasource.password}") String dbPassword) {
		this.environment = environment;
		this.jwtSecret = jwtSecret;
		this.dbPassword = dbPassword;
	}

	@PostConstruct
	void validate() {
		boolean prod = Arrays.asList(environment.getActiveProfiles()).contains("prod");
		if (!prod) {
			return;
		}
		if (DEV_JWT_SECRET.equals(jwtSecret) || jwtSecret.getBytes().length < MIN_SECRET_BYTES) {
			throw new IllegalStateException(
					"JWT_SECRET inseguro em produção: defina um segredo forte (>= 32 bytes) via variável de ambiente");
		}
		if (DEV_DB_PASSWORD.equals(dbPassword) || dbPassword.isBlank()) {
			throw new IllegalStateException(
					"DB_PASSWORD inseguro em produção: defina a senha do banco via variável de ambiente");
		}
	}

}
