package com.poupito.api.auth;

import com.poupito.api.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

	private static final String SECRET = "test-secret-with-at-least-32-bytes-0123456789";

	private JwtService jwtService;
	private User user;

	@BeforeEach
	void setUp() {
		jwtService = new JwtService(SECRET, Duration.ofHours(1));
		user = new User("victor@poupito.com", "hash");
		ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
	}

	@Test
	void shouldExtractUserId_whenTokenIsValid() {
		String token = jwtService.generateToken(user);

		Optional<UUID> userId = jwtService.extractUserId(token);

		assertThat(userId).contains(user.getId());
	}

	@Test
	void shouldReturnEmpty_whenTokenSignedWithAnotherKey() {
		String token = new JwtService("another-secret-with-at-least-32-bytes-987654", Duration.ofHours(1))
				.generateToken(user);

		assertThat(jwtService.extractUserId(token)).isEmpty();
	}

	@Test
	void shouldReturnEmpty_whenTokenIsExpired() {
		String token = new JwtService(SECRET, Duration.ofSeconds(-10)).generateToken(user);

		assertThat(jwtService.extractUserId(token)).isEmpty();
	}

	@Test
	void shouldReturnEmpty_whenTokenIsGarbage() {
		assertThat(jwtService.extractUserId("nao-e-um-jwt")).isEmpty();
	}

	@Test
	void shouldExposeExpirationInSeconds() {
		assertThat(jwtService.expiresInSeconds()).isEqualTo(3600);
	}

}
