package com.dindin.api.account;

import com.dindin.api.TestcontainersConfiguration;
import com.dindin.api.auth.dto.TokenResponse;
import org.junit.jupiter.api.BeforeEach;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class AccountCategoryFlowIntegrationTest {

	@Autowired
	private TestRestTemplate rest;

	private HttpHeaders headers;

	@BeforeEach
	void authenticate() {
		String email = "contas-" + UUID.randomUUID() + "@dindin.com";
		ResponseEntity<TokenResponse> register = rest.postForEntity("/v1/auth/register",
				Map.of("email", email, "password", "senha-forte-123"), TokenResponse.class);
		headers = new HttpHeaders();
		headers.setBearerAuth(register.getBody().token());
	}

	private <T> ResponseEntity<T> exchange(HttpMethod method, String url, Object body, Class<T> type) {
		return rest.exchange(url, method, new HttpEntity<>(body, headers), type);
	}

	@Test
	void shouldCompleteAccountCrudFlow_whenAuthenticated() {
		ResponseEntity<String> created = exchange(HttpMethod.POST, "/v1/accounts",
				Map.of("name", "Nubank", "type", "CREDIT_CARD", "closingDay", 28, "dueDay", 7), String.class);
		assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		String id = created.getBody().replaceAll(".*\"id\":\"([^\"]+)\".*", "$1");

		ResponseEntity<String> list = exchange(HttpMethod.GET, "/v1/accounts", null, String.class);
		assertThat(list.getBody()).contains("Nubank").contains("\"closingDay\":28");

		ResponseEntity<String> updated = exchange(HttpMethod.PUT, "/v1/accounts/" + id,
				Map.of("name", "Nubank UV", "type", "CREDIT_CARD", "closingDay", 25, "dueDay", 4), String.class);
		assertThat(updated.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(updated.getBody()).contains("Nubank UV");

		ResponseEntity<Void> deleted = exchange(HttpMethod.DELETE, "/v1/accounts/" + id, null, Void.class);
		assertThat(deleted.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

		ResponseEntity<String> afterDelete = exchange(HttpMethod.GET, "/v1/accounts", null, String.class);
		assertThat(afterDelete.getBody()).doesNotContain("Nubank");
	}

	@Test
	void shouldReturn400_whenCreditCardWithoutInvoiceDays() {
		ResponseEntity<String> response = exchange(HttpMethod.POST, "/v1/accounts",
				Map.of("name", "Black", "type", "CREDIT_CARD"), String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(response.getBody()).contains("fechamento");
	}

	@Test
	void shouldCompleteCategoryCrudFlow_whenAuthenticated() {
		ResponseEntity<String> created = exchange(HttpMethod.POST, "/v1/categories",
				Map.of("name", "Mercado", "icon", "🛒", "color", "#3fb950", "kind", "EXPENSE"), String.class);
		assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		String id = created.getBody().replaceAll(".*\"id\":\"([^\"]+)\".*", "$1");

		ResponseEntity<String> duplicate = exchange(HttpMethod.POST, "/v1/categories",
				Map.of("name", "mercado", "kind", "EXPENSE"), String.class);
		assertThat(duplicate.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

		ResponseEntity<String> updated = exchange(HttpMethod.PUT, "/v1/categories/" + id,
				Map.of("name", "Supermercado", "icon", "🛒", "color", "#d29922", "kind", "EXPENSE"), String.class);
		assertThat(updated.getBody()).contains("Supermercado").contains("#d29922");

		ResponseEntity<Void> deleted = exchange(HttpMethod.DELETE, "/v1/categories/" + id, null, Void.class);
		assertThat(deleted.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
	}

	@Test
	void shouldReturn400_whenCategoryColorIsInvalid() {
		ResponseEntity<String> response = exchange(HttpMethod.POST, "/v1/categories",
				Map.of("name", "Lazer", "color", "azul", "kind", "EXPENSE"), String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(response.getBody()).contains("fieldErrors");
	}

	@Test
	void shouldReturn404_whenAccessingResourceOfAnotherUser() {
		ResponseEntity<String> created = exchange(HttpMethod.POST, "/v1/accounts",
				Map.of("name", "Uniclass", "type", "CHECKING"), String.class);
		String id = created.getBody().replaceAll(".*\"id\":\"([^\"]+)\".*", "$1");

		// segundo usuário tenta acessar a conta do primeiro
		String otherEmail = "intruso-" + UUID.randomUUID() + "@dindin.com";
		TokenResponse other = rest.postForEntity("/v1/auth/register",
				Map.of("email", otherEmail, "password", "senha-forte-123"), TokenResponse.class).getBody();
		HttpHeaders otherHeaders = new HttpHeaders();
		otherHeaders.setBearerAuth(other.token());

		ResponseEntity<String> stolen = rest.exchange("/v1/accounts/" + id, HttpMethod.PUT,
				new HttpEntity<>(Map.of("name", "Hackeada", "type", "CHECKING"), otherHeaders), String.class);
		assertThat(stolen.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void shouldReturn401_whenNotAuthenticated() {
		assertThat(rest.getForEntity("/v1/accounts", String.class).getStatusCode())
				.isEqualTo(HttpStatus.UNAUTHORIZED);
		assertThat(rest.getForEntity("/v1/categories", String.class).getStatusCode())
				.isEqualTo(HttpStatus.UNAUTHORIZED);
	}

}
