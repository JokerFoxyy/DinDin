package com.dindin.api.auth.refresh;

import com.dindin.api.common.error.InvalidRefreshTokenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

	private final UUID userId = UUID.randomUUID();

	@Mock
	private RefreshTokenRepository repository;

	private RefreshTokenService service;

	@BeforeEach
	void setUp() {
		service = new RefreshTokenService(repository, Duration.ofDays(30));
	}

	@Test
	void shouldIssueRawTokenAndPersistHash() {
		when(repository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

		String raw = service.issue(userId);

		assertThat(raw).isNotBlank();
		verify(repository).save(any(RefreshToken.class));
	}

	@Test
	void shouldRotateActiveTokenRevokingTheOldOne() {
		RefreshToken active = new RefreshToken(userId, "hash", Instant.now().plus(Duration.ofDays(1)));
		when(repository.findByTokenHash(anyString())).thenReturn(Optional.of(active));
		when(repository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

		RefreshTokenService.Rotation rotation = service.rotate("raw-token");

		assertThat(rotation.userId()).isEqualTo(userId);
		assertThat(rotation.rawToken()).isNotBlank();
		assertThat(active.isRevoked()).isTrue();
	}

	@Test
	void shouldThrow_whenRefreshTokenNotFound() {
		when(repository.findByTokenHash(anyString())).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.rotate("raw-token"))
				.isInstanceOf(InvalidRefreshTokenException.class);
	}

	@Test
	void shouldThrow_whenRefreshTokenExpired() {
		RefreshToken expired = new RefreshToken(userId, "hash", Instant.now().minus(Duration.ofDays(1)));
		when(repository.findByTokenHash(anyString())).thenReturn(Optional.of(expired));

		assertThatThrownBy(() -> service.rotate("raw-token"))
				.isInstanceOf(InvalidRefreshTokenException.class);
	}

	@Test
	void shouldIgnoreRevoke_whenTokenIsBlank() {
		service.revoke(null);
		service.revoke("  ");

		verify(repository, never()).findByTokenHash(anyString());
	}

	@Test
	void shouldRevokeExistingToken() {
		RefreshToken active = new RefreshToken(userId, "hash", Instant.now().plus(Duration.ofDays(1)));
		when(repository.findByTokenHash(anyString())).thenReturn(Optional.of(active));

		service.revoke("raw-token");

		assertThat(active.isRevoked()).isTrue();
	}

	@Test
	void shouldRevokeAllForUser() {
		service.revokeAllForUser(userId);

		verify(repository).deleteByUserId(userId);
	}

	@Test
	void shouldExposeTtl() {
		assertThat(service.ttl()).isEqualTo(Duration.ofDays(30));
	}

}
