package com.financeia.api.auth;

import com.financeia.api.auth.dto.TokenResponse;
import com.financeia.api.common.error.EmailAlreadyUsedException;
import com.financeia.api.common.error.InvalidCredentialsException;
import com.financeia.api.user.User;
import com.financeia.api.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

	@Mock
	private UserRepository userRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private JwtService jwtService;

	@InjectMocks
	private AuthService authService;

	@Test
	void shouldRegisterAndReturnToken_whenEmailIsNew() {
		when(userRepository.existsByEmail("victor@financeia.com")).thenReturn(false);
		when(passwordEncoder.encode("senha-forte")).thenReturn("hash");
		when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(jwtService.generateToken(any(User.class))).thenReturn("jwt-token");
		when(jwtService.expiresInSeconds()).thenReturn(7200L);

		TokenResponse response = authService.register(" Victor@FinanceIA.com ", "senha-forte");

		assertThat(response.token()).isEqualTo("jwt-token");
		assertThat(response.tokenType()).isEqualTo("Bearer");
		assertThat(response.expiresInSeconds()).isEqualTo(7200L);
		ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
		verify(userRepository).save(saved.capture());
		assertThat(saved.getValue().getEmail()).isEqualTo("victor@financeia.com");
		assertThat(saved.getValue().getPasswordHash()).isEqualTo("hash");
	}

	@Test
	void shouldThrowEmailAlreadyUsed_whenEmailExists() {
		when(userRepository.existsByEmail("victor@financeia.com")).thenReturn(true);

		assertThatThrownBy(() -> authService.register("victor@financeia.com", "senha-forte"))
				.isInstanceOf(EmailAlreadyUsedException.class);
		verify(userRepository, never()).save(any());
	}

	@Test
	void shouldLoginAndReturnToken_whenCredentialsAreValid() {
		User user = new User("victor@financeia.com", "hash");
		when(userRepository.findByEmail("victor@financeia.com")).thenReturn(Optional.of(user));
		when(passwordEncoder.matches("senha-forte", "hash")).thenReturn(true);
		when(jwtService.generateToken(user)).thenReturn("jwt-token");
		when(jwtService.expiresInSeconds()).thenReturn(7200L);

		TokenResponse response = authService.login("victor@financeia.com", "senha-forte");

		assertThat(response.token()).isEqualTo("jwt-token");
	}

	@Test
	void shouldThrowInvalidCredentials_whenEmailIsUnknown() {
		when(userRepository.findByEmail("nao-existe@financeia.com")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> authService.login("nao-existe@financeia.com", "qualquer"))
				.isInstanceOf(InvalidCredentialsException.class);
	}

	@Test
	void shouldThrowInvalidCredentials_whenPasswordIsWrong() {
		User user = new User("victor@financeia.com", "hash");
		when(userRepository.findByEmail("victor@financeia.com")).thenReturn(Optional.of(user));
		when(passwordEncoder.matches("senha-errada", "hash")).thenReturn(false);

		assertThatThrownBy(() -> authService.login("victor@financeia.com", "senha-errada"))
				.isInstanceOf(InvalidCredentialsException.class);
	}

}
