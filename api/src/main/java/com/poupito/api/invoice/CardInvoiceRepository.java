package com.poupito.api.invoice;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CardInvoiceRepository extends JpaRepository<CardInvoice, UUID> {

	Optional<CardInvoice> findByCardIdAndMonth(UUID cardId, LocalDate month);

	List<CardInvoice> findByCardIdIn(Collection<UUID> cardIds);

	List<CardInvoice> findByCardIdInAndMonthOrderByCreatedAtAsc(Collection<UUID> cardIds, LocalDate month);

	void deleteByCardIdIn(Collection<UUID> cardIds);

}
