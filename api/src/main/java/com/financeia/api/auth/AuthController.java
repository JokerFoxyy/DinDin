package com.financeia.api.auth;

import com.financeia.api.auth.dto.LoginRequest;
import com.financeia.api.auth.dto.MeResponse;
import com.financeia.api.auth.dto.RegisterRequest;
import com.financeia.api.auth.dto.TokenResponse;
import com.financeia.api.common.security.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/auth")
public class AuthController {

	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	@PostMapping("/register")
	@ResponseStatus(HttpStatus.CREATED)
	public TokenResponse register(@Valid @RequestBody RegisterRequest request) {
		return authService.register(request.email(), request.password());
	}

	@PostMapping("/login")
	public TokenResponse login(@Valid @RequestBody LoginRequest request) {
		return authService.login(request.email(), request.password());
	}

	@GetMapping("/me")
	public MeResponse me(@AuthenticationPrincipal AuthenticatedUser user) {
		return new MeResponse(user.id(), user.email());
	}

}
