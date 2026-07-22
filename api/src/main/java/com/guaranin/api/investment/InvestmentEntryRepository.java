package com.guaranin.api.investment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InvestmentEntryRepository extends JpaRepository<InvestmentEntry, UUID> {

	List<InvestmentEntry> findAllByInvestmentIdOrderByDateAscCreatedAtAsc(UUID investmentId);

	List<InvestmentEntry> findAllByInvestmentIdInOrderByDateAscCreatedAtAsc(List<UUID> investmentIds);

	Optional<InvestmentEntry> findByIdAndInvestmentId(UUID id, UUID investmentId);

}
