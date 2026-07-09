package com.financeia.api.auth;

import com.financeia.api.TestcontainersConfiguration;
import com.financeia.api.auth.dto.TokenResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class AuthFlowIntegrationTest {

	@Autowired
	private TestRestTemplate rest;

	@Test
	void shouldRegisterLoginAndAccessMe_whenFlowIsValid() {
		String email = "fluxo@financeia.com";

		ResponseEntity<TokenResponse> register = rest.postForEntity("/v1/auth/register",
				Map.of("email", email, "password", "senha-forte-123"), TokenResponse.class);
		assertThat(register.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		assertThat(register.getBody().token()).isNotBlank();

		ResponseEntity<TokenResponse> login = rest.postForEntity("/v1/auth/login",
				Map.of("email", email, "password", "senha-forte-123"), TokenResponse.class);
		assertThat(login.getStatusCode()).isEqualTo(HttpStatus.OK);

		HttpHeaders headers = new HttpHeaders();
		headers.setBearerAuth(login.getBody().token());
		ResponseEntity<String> me = rest.exchange("/v1/auth/me", HttpMethod.GET,
				new HttpEntity<>(headers), String.class);
		assertThat(me.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(me.getBody()).contains(email);
	}

	@Test
	void shouldReturn401_whenAccessingMeWithoutToken() {
		ResponseEntity<String> me = rest.getForEntity("/v1/auth/me", String.class);

		assertThat(me.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void shouldReturn401_whenTokenIsInvalid() {
		HttpHeaders headers = new HttpHeaders();
		headers.setBearerAuth("token-invalido");
		ResponseEntity<String> me = rest.exchange("/v1/auth/me", HttpMethod.GET,
				new HttpEntity<>(headers), String.class);

		assertThat(me.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void shouldReturn409_whenEmailIsAlreadyRegistered() {
		String email = "duplicado@financeia.com";
		rest.postForEntity("/v1/auth/register",
				Map.of("email", email, "password", "senha-forte-123"), TokenResponse.class);

		ResponseEntity<String> second = rest.postForEntity("/v1/auth/register",
				Map.of("email", email, "password", "senha-forte-123"), String.class);

		assertThat(second.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
		assertThat(second.getBody()).contains("Email já cadastrado");
	}

	@Test
	void shouldReturn401_whenLoginPasswordIsWrong() {
		String email = "senha-errada@financeia.com";
		rest.postForEntity("/v1/auth/register",
				Map.of("email", email, "password", "senha-forte-123"), TokenResponse.class);

		ResponseEntity<String> login = rest.postForEntity("/v1/auth/login",
				Map.of("email", email, "password", "senha-errada"), String.class);

		assertThat(login.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void shouldReturn400WithFieldErrors_whenRegisterPayloadIsInvalid() {
		ResponseEntity<String> response = rest.postForEntity("/v1/auth/register",
				Map.of("email", "nao-e-email", "password", "curta"), String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(response.getBody()).contains("fieldErrors");
	}

	@Test
	void shouldKeepHealthAndSwaggerPublic_whenNotAuthenticated() {
		assertThat(rest.getForEntity("/actuator/health", String.class).getStatusCode())
				.isEqualTo(HttpStatus.OK);
		assertThat(rest.getForEntity("/v3/api-docs", String.class).getStatusCode())
				.isEqualTo(HttpStatus.OK);
	}

}
