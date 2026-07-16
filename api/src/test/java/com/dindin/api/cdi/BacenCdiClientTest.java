package com.dindin.api.cdi;

import com.dindin.api.common.error.ExternalServiceException;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class BacenCdiClientTest {

	@Test
	void shouldParseSeries_whenBacenRespondsSuccessfully() {
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		server.expect(requestTo(
						"https://api.bcb.gov.br/dados/serie/bcdata.sgs.12/dados?formato=json&dataInicial=02/01/2026&dataFinal=05/01/2026"))
				.andExpect(method(org.springframework.http.HttpMethod.GET))
				.andRespond(withSuccess(
						"[{\"data\":\"02/01/2026\",\"valor\":\"0.038932\"},{\"data\":\"05/01/2026\",\"valor\":\"0.039000\"}]",
						MediaType.APPLICATION_JSON));
		BacenCdiClient client = new BacenCdiClient(builder);

		List<CdiRate> rates = client.fetchSeries(LocalDate.of(2026, 1, 2), LocalDate.of(2026, 1, 5));

		assertThat(rates).hasSize(2);
		assertThat(rates.get(0).getDate()).isEqualTo(LocalDate.of(2026, 1, 2));
		assertThat(rates.get(0).getDailyRate()).isEqualByComparingTo("0.038932");
		assertThat(rates.get(1).getDate()).isEqualTo(LocalDate.of(2026, 1, 5));
	}

	@Test
	void shouldThrowExternalServiceException_whenBacenFails() {
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		server.expect(requestTo(
						"https://api.bcb.gov.br/dados/serie/bcdata.sgs.12/dados?formato=json&dataInicial=02/01/2026&dataFinal=05/01/2026"))
				.andRespond(withServerError());
		BacenCdiClient client = new BacenCdiClient(builder);

		assertThatThrownBy(() -> client.fetchSeries(LocalDate.of(2026, 1, 2), LocalDate.of(2026, 1, 5)))
				.isInstanceOf(ExternalServiceException.class);
	}

}
