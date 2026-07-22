package com.guaranin.api.auth;

import com.guaranin.api.auth.dto.IssuedTokens;
import com.guaranin.api.auth.refresh.RefreshTokenService;
import com.guaranin.api.common.error.EmailAlreadyUsedException;
import com.guaranin.api.common.error.InvalidCredentialsException;
import com.guaranin.api.common.error.InvalidRefreshTokenException;
import com.guaranin.api.user.User;
import com.guaranin.api.user.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;
	private final RefreshTokenService refreshTokenService;

	public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
			JwtService jwtService, RefreshTokenService refreshTokenService) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtService = jwtService;
		this.refreshTokenService = refreshTokenService;
	}

	@Transactional
	public IssuedTokens register(String email, String rawPassword) {
		String normalizedEmail = normalize(email);
		if (userRepository.existsByEmail(normalizedEmail)) {
			throw new EmailAlreadyUsedException();
		}
		User user = userRepository.save(new User(normalizedEmail, passwordEncoder.encode(rawPassword)));
		return issueFor(user);
	}

	@Transactional
	public IssuedTokens login(String email, String rawPassword) {
		User user = userRepository.findByEmail(normalize(email))
				.orElseThrow(InvalidCredentialsException::new);
		if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
			throw new InvalidCredentialsException();
		}
		return issueFor(user);
	}

	/** Rotaciona o refresh token e emite um novo par de tokens. */
	@Transactional
	public IssuedTokens refresh(String rawRefreshToken) {
		if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
			throw new InvalidRefreshTokenException();
		}
		RefreshTokenService.Rotation rotation = refreshTokenService.rotate(rawRefreshToken);
		User user = userRepository.findById(rotation.userId())
				.orElseThrow(InvalidRefreshTokenException::new);
		return new IssuedTokens(jwtService.generateToken(user), rotation.rawToken(),
				user.getId(), user.getEmail());
	}

	@Transactional
	public void logout(String rawRefreshToken) {
		refreshTokenService.revoke(rawRefreshToken);
	}

	private IssuedTokens issueFor(User user) {
		String accessToken = jwtService.generateToken(user);
		String refreshToken = refreshTokenService.issue(user.getId());
		return new IssuedTokens(accessToken, refreshToken, user.getId(), user.getEmail());
	}

	private String normalize(String email) {
		return email.trim().toLowerCase();
	}

}
