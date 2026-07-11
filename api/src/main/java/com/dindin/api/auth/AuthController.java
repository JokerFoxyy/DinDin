package com.dindin.api.auth;

import com.dindin.api.auth.dto.IssuedTokens;
import com.dindin.api.auth.dto.LoginRequest;
import com.dindin.api.auth.dto.RegisterRequest;
import com.dindin.api.auth.dto.UserResponse;
import com.dindin.api.auth.refresh.RefreshTokenService;
import com.dindin.api.common.security.AuthCookieFactory;
import com.dindin.api.common.security.AuthenticatedUser;
import com.dindin.api.common.security.LoginRateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/auth")
public class AuthController {

	private final AuthService authService;
	private final AuthCookieFactory cookieFactory;
	private final LoginRateLimiter rateLimiter;
	private final JwtService jwtService;
	private final RefreshTokenService refreshTokenService;

	public AuthController(AuthService authService, AuthCookieFactory cookieFactory,
			LoginRateLimiter rateLimiter, JwtService jwtService, RefreshTokenService refreshTokenService) {
		this.authService = authService;
		this.cookieFactory = cookieFactory;
		this.rateLimiter = rateLimiter;
		this.jwtService = jwtService;
		this.refreshTokenService = refreshTokenService;
	}

	@PostMapping("/register")
	public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request,
			HttpServletRequest httpRequest) {
		rateLimiter.check(rateKey(httpRequest, request.email()));
		IssuedTokens tokens = authService.register(request.email(), request.password());
		return withSessionCookies(HttpStatus.CREATED, tokens);
	}

	@PostMapping("/login")
	public ResponseEntity<UserResponse> login(@Valid @RequestBody LoginRequest request,
			HttpServletRequest httpRequest) {
		rateLimiter.check(rateKey(httpRequest, request.email()));
		IssuedTokens tokens = authService.login(request.email(), request.password());
		return withSessionCookies(HttpStatus.OK, tokens);
	}

	@PostMapping("/refresh")
	public ResponseEntity<UserResponse> refresh(
			@CookieValue(name = AuthCookieFactory.REFRESH_COOKIE, required = false) String refreshToken) {
		IssuedTokens tokens = authService.refresh(refreshToken);
		return withSessionCookies(HttpStatus.OK, tokens);
	}

	@PostMapping("/logout")
	public ResponseEntity<Void> logout(
			@CookieValue(name = AuthCookieFactory.REFRESH_COOKIE, required = false) String refreshToken) {
		authService.logout(refreshToken);
		return ResponseEntity.noContent()
				.header(HttpHeaders.SET_COOKIE, cookieFactory.clearAccess().toString())
				.header(HttpHeaders.SET_COOKIE, cookieFactory.clearRefresh().toString())
				.build();
	}

	@GetMapping("/me")
	public UserResponse me(@AuthenticationPrincipal AuthenticatedUser user) {
		return new UserResponse(user.id(), user.email());
	}

	private ResponseEntity<UserResponse> withSessionCookies(HttpStatus status, IssuedTokens tokens) {
		ResponseCookie access = cookieFactory.access(tokens.accessToken(), jwtService.expiration());
		ResponseCookie refresh = cookieFactory.refresh(tokens.refreshToken(), refreshTokenService.ttl());
		return ResponseEntity.status(status)
				.header(HttpHeaders.SET_COOKIE, access.toString())
				.header(HttpHeaders.SET_COOKIE, refresh.toString())
				.body(tokens.user());
	}

	private String rateKey(HttpServletRequest request, String email) {
		return request.getRemoteAddr() + ":" + (email == null ? "" : email.trim().toLowerCase());
	}

}
