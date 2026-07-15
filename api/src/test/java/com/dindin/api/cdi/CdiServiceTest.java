package com.dindin.api.cdi;

import com.dindin.api.cdi.dto.CdiPointResponse;
import com.dindin.api.common.error.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CdiServiceTest {

	@Mock
	private CdiRateRepository repository;
	@Mock
	private BacenCdiClient client;

	private CdiService service;

	private final LocalDate from = LocalDate.of(2026, 1, 2);
	private final LocalDate to = LocalDate.of(2026, 1, 5);

	@Test
	void shouldThrowBusiness_whenFromIsAfterTo() {
		service = new CdiService(repository, client);

		assertThatThrownBy(() -> service.accumulatedSeries(to, from)).isInstanceOf(BusinessException.class);
	}

	@Test
	void shouldFetchFromBacen_whenNotCached() {
		service = new CdiService(repository, client);
		when(repository.existsById(to)).thenReturn(false);
		List<CdiRate> fetched = List.of(new CdiRate(from, new BigDecimal("0.038932")),
				new CdiRate(to, new BigDecimal("0.039000")));
		when(client.fetchSeries(from, to)).thenReturn(fetched);
		when(repository.findAllByDateBetweenOrderByDateAsc(from, to)).thenReturn(fetched);

		List<CdiPointResponse> series = service.accumulatedSeries(from, to);

		verify(client).fetchSeries(from, to);
		ArgumentCaptor<List<CdiRate>> captor = ArgumentCaptor.forClass(List.class);
		verify(repository).saveAll(captor.capture());
		assertThat(captor.getValue()).hasSize(2);
		assertThat(series).hasSize(2);
	}

	@Test
	void shouldSkipBacenCall_whenAlreadyCached() {
		service = new CdiService(repository, client);
		when(repository.existsById(to)).thenReturn(true);
		when(repository.findAllByDateBetweenOrderByDateAsc(from, to)).thenReturn(List.of(
				new CdiRate(from, new BigDecimal("0.038932")), new CdiRate(to, new BigDecimal("0.039000"))));

		service.accumulatedSeries(from, to);

		verify(client, never()).fetchSeries(eq(from), eq(to));
	}

	@Test
	void shouldComputeCompoundAccumulatedPercentage() {
		service = new CdiService(repository, client);
		when(repository.existsById(to)).thenReturn(true);
		when(repository.findAllByDateBetweenOrderByDateAsc(from, to)).thenReturn(List.of(
				new CdiRate(from, new BigDecimal("1.000000")), new CdiRate(to, new BigDecimal("1.000000"))));

		List<CdiPointResponse> series = service.accumulatedSeries(from, to);

		// (1.01 * 1.01 - 1) * 100 = 2.0100
		assertThat(series).hasSize(2);
		assertThat(series.get(0).accumulatedPercentage()).isEqualByComparingTo("1.0000");
		assertThat(series.get(1).accumulatedPercentage()).isEqualByComparingTo("2.0100");
	}

}
