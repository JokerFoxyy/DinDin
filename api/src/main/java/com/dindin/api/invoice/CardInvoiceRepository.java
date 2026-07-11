package com.dindin.api.invoice;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CardInvoiceRepository extends JpaRepository<CardInvoice, UUID> {

	Optional<CardInvoice> findByAccountIdAndMonth(UUID accountId, LocalDate month);

	List<CardInvoice> findByAccountIdIn(Collection<UUID> accountIds);

	void deleteByAccountIdIn(Collection<UUID> accountIds);

}
