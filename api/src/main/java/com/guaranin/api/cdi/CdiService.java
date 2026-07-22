package com.guaranin.api.cdi;

import com.guaranin.api.cdi.dto.CdiPointResponse;
import com.guaranin.api.common.error.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class CdiService {

	private static final MathContext MC = new MathContext(12);

	private final CdiRateRepository repository;
	private final BacenCdiClient client;

	public CdiService(CdiRateRepository repository, BacenCdiClient client) {
		this.repository = repository;
		this.client = client;
	}

	@Transactional
	public List<CdiPointResponse> accumulatedSeries(LocalDate from, LocalDate to) {
		if (from.isAfter(to)) {
			throw new BusinessException("Data inicial não pode ser depois da data final");
		}
		LocalDate yesterday = LocalDate.now().minusDays(1);
		LocalDate effectiveTo = to.isAfter(yesterday) ? yesterday : to;
		ensureCached(from, effectiveTo);

		List<CdiRate> rates = repository.findAllByDateBetweenOrderByDateAsc(from, effectiveTo);
		List<CdiPointResponse> series = new ArrayList<>(rates.size());
		BigDecimal accumulatedFactor = BigDecimal.ONE;
		for (CdiRate rate : rates) {
			BigDecimal dailyFactor = BigDecimal.ONE.add(rate.getDailyRate().divide(BigDecimal.valueOf(100), MC));
			accumulatedFactor = accumulatedFactor.multiply(dailyFactor, MC);
			BigDecimal percentage = accumulatedFactor.subtract(BigDecimal.ONE)
					.multiply(BigDecimal.valueOf(100))
					.setScale(4, RoundingMode.HALF_UP);
			series.add(new CdiPointResponse(rate.getDate(), percentage));
		}
		return series;
	}

	private void ensureCached(LocalDate from, LocalDate to) {
		if (repository.existsById(to)) {
			return;
		}
		List<CdiRate> fetched = client.fetchSeries(from, to);
		repository.saveAll(fetched);
	}

}
