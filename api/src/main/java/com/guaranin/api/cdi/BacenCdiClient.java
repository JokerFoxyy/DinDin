package com.guaranin.api.cdi;

import com.guaranin.api.cdi.dto.BacenSeriesPoint;
import com.guaranin.api.common.error.ExternalServiceException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class BacenCdiClient {

	private static final DateTimeFormatter BACEN_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

	private final RestClient restClient;

	public BacenCdiClient(RestClient.Builder restClientBuilder) {
		this.restClient = restClientBuilder.baseUrl("https://api.bcb.gov.br").build();
	}

	public List<CdiRate> fetchSeries(LocalDate from, LocalDate to) {
		try {
			BacenSeriesPoint[] points = restClient.get()
					.uri(uriBuilder -> uriBuilder
							.path("/dados/serie/bcdata.sgs.12/dados")
							.queryParam("formato", "json")
							.queryParam("dataInicial", from.format(BACEN_DATE_FORMAT))
							.queryParam("dataFinal", to.format(BACEN_DATE_FORMAT))
							.build())
					.retrieve()
					.body(BacenSeriesPoint[].class);
			if (points == null) {
				throw new ExternalServiceException("Resposta vazia do Banco Central", null);
			}
			return List.of(points).stream()
					.map(point -> new CdiRate(LocalDate.parse(point.date(), BACEN_DATE_FORMAT),
							new BigDecimal(point.value())))
					.toList();
		} catch (ExternalServiceException e) {
			throw e;
		} catch (Exception e) {
			throw new ExternalServiceException("Falha ao buscar série do CDI no Banco Central", e);
		}
	}

}
