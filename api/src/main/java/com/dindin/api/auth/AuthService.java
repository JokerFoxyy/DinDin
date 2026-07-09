package com.dindin.api.auth;

import com.dindin.api.auth.dto.TokenResponse;
import com.dindin.api.common.error.EmailAlreadyUsedException;
import com.dindin.api.common.error.InvalidCredentialsException;
import com.dindin.api.user.User;
import com.dindin.api.user.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;

	public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtService = jwtService;
	}

	@Transactional
	public TokenResponse register(String email, String rawPassword) {
		String normalizedEmail = normalize(email);
		if (userRepository.existsByEmail(normalizedEmail)) {
			throw new EmailAlreadyUsedException();
		}
		User user = userRepository.save(new User(normalizedEmail, passwordEncoder.encode(rawPassword)));
		return TokenResponse.bearer(jwtService.generateToken(user), jwtService.expiresInSeconds());
	}

	@Transactional(readOnly = true)
	public TokenResponse login(String email, String rawPassword) {
		User user = userRepository.findByEmail(normalize(email))
				.orElseThrow(InvalidCredentialsException::new);
		if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
			throw new InvalidCredentialsException();
		}
		return TokenResponse.bearer(jwtService.generateToken(user), jwtService.expiresInSeconds());
	}

	private String normalize(String email) {
		return email.trim().toLowerCase();
	}

}
