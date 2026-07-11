package com.dindin.api.auth.refresh;

import com.dindin.api.common.error.InvalidRefreshTokenException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class RefreshTokenService {

	private static final SecureRandom RANDOM = new SecureRandom();
	private static final Base64.Encoder BASE64 = Base64.getUrlEncoder().withoutPadding();

	private final RefreshTokenRepository repository;
	private final Duration ttl;

	public RefreshTokenService(RefreshTokenRepository repository,
			@Value("${app.jwt.refresh-expiration}") Duration ttl) {
		this.repository = repository;
		this.ttl = ttl;
	}

	/** Cria um novo refresh token e devolve o valor em claro (só ele; o banco guarda o hash). */
	@Transactional
	public String issue(UUID userId) {
		String raw = randomToken();
		repository.save(new RefreshToken(userId, hash(raw), Instant.now().plus(ttl)));
		return raw;
	}

	/** Valida o token recebido, revoga-o (rotação) e emite um novo. */
	@Transactional
	public Rotation rotate(String rawToken) {
		RefreshToken current = repository.findByTokenHash(hash(rawToken))
				.orElseThrow(InvalidRefreshTokenException::new);
		if (!current.isActive(Instant.now())) {
			throw new InvalidRefreshTokenException();
		}
		current.revoke();
		return new Rotation(current.getUserId(), issue(current.getUserId()));
	}

	/** Revoga um refresh token (logout). Idempotente: token inexistente é ignorado. */
	@Transactional
	public void revoke(String rawToken) {
		if (rawToken == null || rawToken.isBlank()) {
			return;
		}
		repository.findByTokenHash(hash(rawToken)).ifPresent(RefreshToken::revoke);
	}

	@Transactional
	public void revokeAllForUser(UUID userId) {
		repository.deleteByUserId(userId);
	}

	public Duration ttl() {
		return ttl;
	}

	private String randomToken() {
		byte[] bytes = new byte[32];
		RANDOM.nextBytes(bytes);
		return BASE64.encodeToString(bytes);
	}

	private String hash(String raw) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			return HexFormat.of().formatHex(digest.digest(raw.getBytes(StandardCharsets.UTF_8)));
		} catch (Exception e) {
			throw new IllegalStateException("SHA-256 indisponível", e);
		}
	}

	public record Rotation(UUID userId, String rawToken) {
	}

}
