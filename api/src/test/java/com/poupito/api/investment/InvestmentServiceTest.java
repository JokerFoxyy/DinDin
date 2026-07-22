package com.poupito.api.investment;

import com.poupito.api.common.error.BusinessException;
import com.poupito.api.common.error.NotFoundException;
import com.poupito.api.investment.dto.InvestmentEntryRequest;
import com.poupito.api.investment.dto.InvestmentEntryResponse;
import com.poupito.api.investment.dto.InvestmentRequest;
import com.poupito.api.investment.dto.InvestmentResponse;
import com.poupito.api.investment.dto.InvestmentUpdateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvestmentServiceTest {

	private final UUID userId = UUID.randomUUID();
	private final UUID investmentId = UUID.randomUUID();

	@Mock
	private InvestmentRepository investmentRepository;
	@Mock
	private InvestmentEntryRepository entryRepository;

	private InvestmentService service;

	private Investment investment;

	@BeforeEach
	void setUp() {
		service = new InvestmentService(investmentRepository, entryRepository, new InvestmentReturnCalculator());
		investment = new Investment(userId, "Tesouro Selic", AssetClass.RENDA_FIXA, "NuInvest");
		ReflectionTestUtils.setField(investment, "id", investmentId);
	}

	@Test
	void shouldCreateInvestment() {
		when(investmentRepository.save(any(Investment.class))).thenAnswer(invocation -> {
			Investment saved = invocation.getArgument(0);
			ReflectionTestUtils.setField(saved, "id", investmentId);
			return saved;
		});

		InvestmentResponse response = service.create(userId,
				new InvestmentRequest("Tesouro Selic", AssetClass.RENDA_FIXA, "NuInvest"));

		assertThat(response.id()).isEqualTo(investmentId);
		assertThat(response.name()).isEqualTo("Tesouro Selic");
	}

	@Test
	void shouldUpdateNameAndInstitution_whenOwnedByUser() {
		when(investmentRepository.findByIdAndUserId(investmentId, userId)).thenReturn(Optional.of(investment));

		InvestmentResponse response = service.update(userId, investmentId,
				new InvestmentUpdateRequest("Tesouro IPCA", "Rico"));

		assertThat(response.name()).isEqualTo("Tesouro IPCA");
		assertThat(response.institution()).isEqualTo("Rico");
	}

	@Test
	void shouldThrowNotFound_whenUpdatingInvestmentNotOwned() {
		when(investmentRepository.findByIdAndUserId(investmentId, userId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.update(userId, investmentId, new InvestmentUpdateRequest("X", "Y")))
				.isInstanceOf(NotFoundException.class);
	}

	@Test
	void shouldDeleteInvestment_whenOwnedByUser() {
		when(investmentRepository.findByIdAndUserId(investmentId, userId)).thenReturn(Optional.of(investment));

		service.delete(userId, investmentId);

		ArgumentCaptor<Investment> captor = ArgumentCaptor.forClass(Investment.class);
		verify(investmentRepository).delete(captor.capture());
		assertThat(captor.getValue().getId()).isEqualTo(investmentId);
	}

	@Test
	void shouldThrowNotFound_whenDeletingInvestmentNotOwned() {
		when(investmentRepository.findByIdAndUserId(investmentId, userId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.delete(userId, investmentId)).isInstanceOf(NotFoundException.class);
	}

	@Test
	void shouldCreateEntry_whenInvestmentOwnedAndValid() {
		when(investmentRepository.findByIdAndUserId(investmentId, userId)).thenReturn(Optional.of(investment));
		when(entryRepository.save(any(InvestmentEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));

		InvestmentEntryResponse response = service.createEntry(userId, investmentId,
				new InvestmentEntryRequest(LocalDate.of(2026, 1, 5), EntryType.APORTE, new BigDecimal("500.00"), null));

		assertThat(response.type()).isEqualTo(EntryType.APORTE);
		assertThat(response.amount()).isEqualByComparingTo("500.00");
	}

	@Test
	void shouldThrowBusiness_whenAtualizacaoSaldoMissingBalanceAfter() {
		when(investmentRepository.findByIdAndUserId(investmentId, userId)).thenReturn(Optional.of(investment));

		assertThatThrownBy(() -> service.createEntry(userId, investmentId,
				new InvestmentEntryRequest(LocalDate.of(2026, 1, 5), EntryType.ATUALIZACAO_SALDO,
						BigDecimal.ZERO, null)))
				.isInstanceOf(BusinessException.class);
		verify(entryRepository, never()).save(any());
	}

	@Test
	void shouldThrowNotFound_whenCreatingEntryForInvestmentNotOwned() {
		when(investmentRepository.findByIdAndUserId(investmentId, userId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.createEntry(userId, investmentId,
				new InvestmentEntryRequest(LocalDate.of(2026, 1, 5), EntryType.APORTE, new BigDecimal("500.00"), null)))
				.isInstanceOf(NotFoundException.class);
	}

	@Test
	void shouldDeleteEntry_whenOwnedByUser() {
		UUID entryId = UUID.randomUUID();
		InvestmentEntry entry = new InvestmentEntry(investmentId, LocalDate.of(2026, 1, 5), EntryType.APORTE,
				new BigDecimal("500.00"), null);
		ReflectionTestUtils.setField(entry, "id", entryId);
		when(investmentRepository.findByIdAndUserId(investmentId, userId)).thenReturn(Optional.of(investment));
		when(entryRepository.findByIdAndInvestmentId(entryId, investmentId)).thenReturn(Optional.of(entry));

		service.deleteEntry(userId, investmentId, entryId);

		verify(entryRepository).delete(entry);
	}

	@Test
	void shouldThrowNotFound_whenDeletingEntryThatDoesNotExist() {
		UUID entryId = UUID.randomUUID();
		when(investmentRepository.findByIdAndUserId(investmentId, userId)).thenReturn(Optional.of(investment));
		when(entryRepository.findByIdAndInvestmentId(entryId, investmentId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.deleteEntry(userId, investmentId, entryId))
				.isInstanceOf(NotFoundException.class);
	}

	@Test
	void shouldReturnEmptyReport_whenNoInvestments() {
		when(investmentRepository.findAllByUserIdOrderByCreatedAtAsc(userId)).thenReturn(List.of());

		var report = service.report(userId);

		assertThat(report.investments()).isEmpty();
		assertThat(report.byClass()).isEmpty();
	}

	@Test
	void shouldAggregateReportByInvestmentAndClass() {
		Investment second = new Investment(userId, "CDB Banco X", AssetClass.RENDA_FIXA, "Banco X");
		ReflectionTestUtils.setField(second, "id", UUID.randomUUID());
		when(investmentRepository.findAllByUserIdOrderByCreatedAtAsc(userId)).thenReturn(List.of(investment, second));

		InvestmentEntry entry1 = new InvestmentEntry(investmentId, LocalDate.of(2026, 1, 5), EntryType.APORTE,
				new BigDecimal("1000.00"), null);
		InvestmentEntry entry2 = new InvestmentEntry(second.getId(), LocalDate.of(2026, 1, 6), EntryType.APORTE,
				new BigDecimal("2000.00"), null);
		when(entryRepository.findAllByInvestmentIdInOrderByDateAscCreatedAtAsc(List.of(investmentId, second.getId())))
				.thenReturn(List.of(entry1, entry2));

		var report = service.report(userId);

		assertThat(report.investments()).hasSize(2);
		assertThat(report.byClass()).hasSize(1);
		assertThat(report.byClass().getFirst().assetClass()).isEqualTo(AssetClass.RENDA_FIXA);
		assertThat(report.byClass().getFirst().totalBalance()).isEqualByComparingTo("3000.00");
	}

}
