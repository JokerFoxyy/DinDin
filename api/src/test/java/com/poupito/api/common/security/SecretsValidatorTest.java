package com.poupito.api.common.security;

import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SecretsValidatorTest {

	private static final String STRONG_SECRET = "a-really-strong-secret-value-with-32b+";
	private static final String STRONG_DB = "strong-db-password";

	private Environment envWithProfiles(String... profiles) {
		Environment env = mock(Environment.class);
		when(env.getActiveProfiles()).thenReturn(profiles);
		return env;
	}

	@Test
	void shouldSkipValidation_whenNotProdProfile() {
		SecretsValidator validator = new SecretsValidator(envWithProfiles(),
				SecretsValidator.DEV_JWT_SECRET, SecretsValidator.DEV_DB_PASSWORD);

		assertThatCode(validator::validate).doesNotThrowAnyException();
	}

	@Test
	void shouldFail_whenProdUsesDevJwtSecret() {
		SecretsValidator validator = new SecretsValidator(envWithProfiles("prod"),
				SecretsValidator.DEV_JWT_SECRET, STRONG_DB);

		assertThatThrownBy(validator::validate)
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("JWT_SECRET");
	}

	@Test
	void shouldFail_whenProdSecretTooShort() {
		SecretsValidator validator = new SecretsValidator(envWithProfiles("prod"), "curto", STRONG_DB);

		assertThatThrownBy(validator::validate).isInstanceOf(IllegalStateException.class);
	}

	@Test
	void shouldFail_whenProdUsesDevDbPassword() {
		SecretsValidator validator = new SecretsValidator(envWithProfiles("prod"),
				STRONG_SECRET, SecretsValidator.DEV_DB_PASSWORD);

		assertThatThrownBy(validator::validate)
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("DB_PASSWORD");
	}

	@Test
	void shouldPass_whenProdHasStrongSecrets() {
		SecretsValidator validator = new SecretsValidator(envWithProfiles("prod"), STRONG_SECRET, STRONG_DB);

		assertThatCode(validator::validate).doesNotThrowAnyException();
	}

}
