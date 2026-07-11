package com.dindin.api.common.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Monta os cookies de sessão. Ambos são httpOnly (inacessíveis ao JS → seguros contra XSS)
 * e SameSite=Strict (não vão em requisições cross-site → defesa contra CSRF).
 * O atributo Secure é ligado só em produção (cookie Secure não é aceito em http local).
 */
@Component
public class AuthCookieFactory {

	public static final String ACCESS_COOKIE = "dindin_at";
	public static final String REFRESH_COOKIE = "dindin_rt";
	private static final String REFRESH_PATH = "/api/v1/auth";
	private static final String ROOT_PATH = "/";

	private final boolean secure;

	public AuthCookieFactory(@Value("${app.security.cookie-secure}") boolean secure) {
		this.secure = secure;
	}

	public ResponseCookie access(String token, Duration ttl) {
		return build(ACCESS_COOKIE, token, ROOT_PATH, ttl);
	}

	public ResponseCookie refresh(String token, Duration ttl) {
		return build(REFRESH_COOKIE, token, REFRESH_PATH, ttl);
	}

	public ResponseCookie clearAccess() {
		return build(ACCESS_COOKIE, "", ROOT_PATH, Duration.ZERO);
	}

	public ResponseCookie clearRefresh() {
		return build(REFRESH_COOKIE, "", REFRESH_PATH, Duration.ZERO);
	}

	private ResponseCookie build(String name, String value, String path, Duration ttl) {
		return ResponseCookie.from(name, value)
				.httpOnly(true)
				.secure(secure)
				.sameSite("Strict")
				.path(path)
				.maxAge(ttl)
				.build();
	}

}
