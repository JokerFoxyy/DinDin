package com.dindin.api.support;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

import java.util.List;

/**
 * Utilidades para testes de integração no novo modelo de auth por cookie.
 * O access token é entregue no cookie httpOnly {@code dindin_at}; como o filtro também
 * aceita {@code Authorization: Bearer}, os testes extraem o token do Set-Cookie e o usam
 * como Bearer (mais simples que manter um cookie jar no TestRestTemplate).
 */
public final class AuthTestSupport {

	public static final String ACCESS_COOKIE = "dindin_at";
	public static final String REFRESH_COOKIE = "dindin_rt";

	private AuthTestSupport() {
	}

	public static String cookieValue(ResponseEntity<?> response, String name) {
		List<String> setCookies = response.getHeaders().get(HttpHeaders.SET_COOKIE);
		if (setCookies == null) {
			return null;
		}
		String prefix = name + "=";
		for (String cookie : setCookies) {
			if (cookie.startsWith(prefix)) {
				String value = cookie.substring(prefix.length());
				int semicolon = value.indexOf(';');
				return semicolon >= 0 ? value.substring(0, semicolon) : value;
			}
		}
		return null;
	}

	/** Headers com Authorization Bearer extraído do cookie de acesso da resposta. */
	public static HttpHeaders bearer(ResponseEntity<?> response) {
		HttpHeaders headers = new HttpHeaders();
		headers.setBearerAuth(cookieValue(response, ACCESS_COOKIE));
		return headers;
	}

	/** Headers com o cookie de refresh (para chamar /auth/refresh e /auth/logout). */
	public static HttpHeaders refreshCookie(ResponseEntity<?> response) {
		HttpHeaders headers = new HttpHeaders();
		headers.add(HttpHeaders.COOKIE, REFRESH_COOKIE + "=" + cookieValue(response, REFRESH_COOKIE));
		return headers;
	}

}
