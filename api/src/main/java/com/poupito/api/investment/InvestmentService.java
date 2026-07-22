package com.poupito.api.investment;

import com.poupito.api.common.error.BusinessException;
import com.poupito.api.common.error.NotFoundException;
import com.poupito.api.investment.InvestmentReturnCalculator.Performance;
import com.poupito.api.investment.dto.AssetClassPerformanceResponse;
import com.poupito.api.investment.dto.InvestmentEntryRequest;
import com.poupito.api.investment.dto.InvestmentEntryResponse;
import com.poupito.api.investment.dto.InvestmentPerformanceResponse;
import com.poupito.api.investment.dto.InvestmentReportResponse;
import com.poupito.api.investment.dto.InvestmentRequest;
import com.poupito.api.investment.dto.InvestmentResponse;
import com.poupito.api.investment.dto.InvestmentUpdateRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class InvestmentService {

	private final InvestmentRepository investmentRepository;
	private final InvestmentEntryRepository entryRepository;
	private final InvestmentReturnCalculator calculator;

	public InvestmentService(InvestmentRepository investmentRepository, InvestmentEntryRepository entryRepository,
			InvestmentReturnCalculator calculator) {
		this.investmentRepository = investmentRepository;
		this.entryRepository = entryRepository;
		this.calculator = calculator;
	}

	@Transactional(readOnly = true)
	public List<InvestmentResponse> list(UUID userId) {
		return investmentRepository.findAllByUserIdOrderByCreatedAtAsc(userId).stream()
				.map(InvestmentResponse::from)
				.toList();
	}

	@Transactional
	public InvestmentResponse create(UUID userId, InvestmentRequest request) {
		Investment investment = investmentRepository.save(
				new Investment(userId, request.name(), request.assetClass(), request.institution()));
		return InvestmentResponse.from(investment);
	}

	@Transactional
	public InvestmentResponse update(UUID userId, UUID investmentId, InvestmentUpdateRequest request) {
		Investment investment = findOwned(userId, investmentId);
		investment.update(request.name(), request.institution());
		return InvestmentResponse.from(investment);
	}

	@Transactional
	public void delete(UUID userId, UUID investmentId) {
		investmentRepository.delete(findOwned(userId, investmentId));
	}

	@Transactional(readOnly = true)
	public List<InvestmentEntryResponse> listEntries(UUID userId, UUID investmentId) {
		findOwned(userId, investmentId);
		return entryRepository.findAllByInvestmentIdOrderByDateAscCreatedAtAsc(investmentId).stream()
				.map(InvestmentEntryResponse::from)
				.toList();
	}

	@Transactional
	public InvestmentEntryResponse createEntry(UUID userId, UUID investmentId, InvestmentEntryRequest request) {
		findOwned(userId, investmentId);
		if (request.type() == EntryType.ATUALIZACAO_SALDO && request.balanceAfter() == null) {
			throw new BusinessException("balanceAfter é obrigatório para atualização de saldo");
		}
		InvestmentEntry entry = entryRepository.save(new InvestmentEntry(investmentId, request.date(),
				request.type(), request.amount(), request.balanceAfter()));
		return InvestmentEntryResponse.from(entry);
	}

	@Transactional
	public void deleteEntry(UUID userId, UUID investmentId, UUID entryId) {
		findOwned(userId, investmentId);
		InvestmentEntry entry = entryRepository.findByIdAndInvestmentId(entryId, investmentId)
				.orElseThrow(() -> new NotFoundException("Lançamento não encontrado"));
		entryRepository.delete(entry);
	}

	@Transactional(readOnly = true)
	public InvestmentReportResponse report(UUID userId) {
		List<Investment> investments = investmentRepository.findAllByUserIdOrderByCreatedAtAsc(userId);
		if (investments.isEmpty()) {
			return new InvestmentReportResponse(List.of(), List.of());
		}
		List<UUID> investmentIds = investments.stream().map(Investment::getId).toList();
		Map<UUID, List<InvestmentEntry>> entriesByInvestment = entryRepository
				.findAllByInvestmentIdInOrderByDateAscCreatedAtAsc(investmentIds).stream()
				.collect(Collectors.groupingBy(InvestmentEntry::getInvestmentId));

		List<InvestmentPerformanceResponse> perInvestment = investments.stream()
				.map(investment -> InvestmentPerformanceResponse.from(investment,
						calculator.compute(entriesByInvestment.getOrDefault(investment.getId(), List.of()))))
				.toList();

		Map<AssetClass, List<Investment>> investmentsByClass = investments.stream()
				.collect(Collectors.groupingBy(Investment::getAssetClass));
		List<AssetClassPerformanceResponse> byClass = investmentsByClass.entrySet().stream()
				.map(classEntry -> {
					List<InvestmentEntry> classEntries = classEntry.getValue().stream()
							.flatMap(investment -> entriesByInvestment.getOrDefault(investment.getId(), List.of())
									.stream())
							.sorted(Comparator.comparing(InvestmentEntry::getDate)
									.thenComparing(InvestmentEntry::getCreatedAt))
							.toList();
					return AssetClassPerformanceResponse.from(classEntry.getKey(), calculator.compute(classEntries));
				})
				.toList();

		return new InvestmentReportResponse(perInvestment, byClass);
	}

	private Investment findOwned(UUID userId, UUID investmentId) {
		return investmentRepository.findByIdAndUserId(investmentId, userId)
				.orElseThrow(() -> new NotFoundException("Investimento não encontrado"));
	}

}
