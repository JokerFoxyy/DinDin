package com.dindin.api.auth;

import com.dindin.api.auth.dto.TokenResponse;
import com.dindin.api.common.error.EmailAlreadyUsedException;
import com.dindin.api.common.error.InvalidCredentialsException;
import com.dindin.api.user.User;
import com.dindin.api.user.UserRepository;
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
		when(userRepository.existsByEmail("victor@dindin.com")).thenReturn(false);
		when(passwordEncoder.encode("senha-forte")).thenReturn("hash");
		when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(jwtService.generateToken(any(User.class))).thenReturn("jwt-token");
		when(jwtService.expiresInSeconds()).thenReturn(7200L);

		TokenResponse response = authService.register(" Victor@DinDin.com ", "senha-forte");

		assertThat(response.token()).isEqualTo("jwt-token");
		assertThat(response.tokenType()).isEqualTo("Bearer");
		assertThat(response.expiresInSeconds()).isEqualTo(7200L);
		ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
		verify(userRepository).save(saved.capture());
		assertThat(saved.getValue().getEmail()).isEqualTo("victor@dindin.com");
		assertThat(saved.getValue().getPasswordHash()).isEqualTo("hash");
	}

	@Test
	void shouldThrowEmailAlreadyUsed_whenEmailExists() {
		when(userRepository.existsByEmail("victor@dindin.com")).thenReturn(true);

		assertThatThrownBy(() -> authService.register("victor@dindin.com", "senha-forte"))
				.isInstanceOf(EmailAlreadyUsedException.class);
		verify(userRepository, never()).save(any());
	}

	@Test
	void shouldLoginAndReturnToken_whenCredentialsAreValid() {
		User user = new User("victor@dindin.com", "hash");
		when(userRepository.findByEmail("victor@dindin.com")).thenReturn(Optional.of(user));
		when(passwordEncoder.matches("senha-forte", "hash")).thenReturn(true);
		when(jwtService.generateToken(user)).thenReturn("jwt-token");
		when(jwtService.expiresInSeconds()).thenReturn(7200L);

		TokenResponse response = authService.login("victor@dindin.com", "senha-forte");

		assertThat(response.token()).isEqualTo("jwt-token");
	}

	@Test
	void shouldThrowInvalidCredentials_whenEmailIsUnknown() {
		when(userRepository.findByEmail("nao-existe@dindin.com")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> authService.login("nao-existe@dindin.com", "qualquer"))
				.isInstanceOf(InvalidCredentialsException.class);
	}

	@Test
	void shouldThrowInvalidCredentials_whenPasswordIsWrong() {
		User user = new User("victor@dindin.com", "hash");
		when(userRepository.findByEmail("victor@dindin.com")).thenReturn(Optional.of(user));
		when(passwordEncoder.matches("senha-errada", "hash")).thenReturn(false);

		assertThatThrownBy(() -> authService.login("victor@dindin.com", "senha-errada"))
				.isInstanceOf(InvalidCredentialsException.class);
	}

}
