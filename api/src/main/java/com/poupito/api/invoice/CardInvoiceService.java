package com.poupito.api.invoice;

import com.poupito.api.card.Card;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;

@Service
public class CardInvoiceService {

	private final CardInvoiceRepository cardInvoiceRepository;

	public CardInvoiceService(CardInvoiceRepository cardInvoiceRepository) {
		this.cardInvoiceRepository = cardInvoiceRepository;
	}

	/**
	 * Retorna (ou cria) a fatura do período de uma compra no cartão.
	 * Regra: compra no dia do fechamento ou depois entra na fatura do mês seguinte.
	 */
	@Transactional
	public CardInvoice getOrCreateInvoiceFor(Card card, LocalDate purchaseDate) {
		YearMonth invoiceMonth = invoiceMonthFor(card.getClosingDay(), purchaseDate);
		return cardInvoiceRepository.findByCardIdAndMonth(card.getId(), invoiceMonth.atDay(1))
				.orElseGet(() -> cardInvoiceRepository.save(newInvoice(card, invoiceMonth)));
	}

	static YearMonth invoiceMonthFor(int closingDay, LocalDate purchaseDate) {
		YearMonth purchaseMonth = YearMonth.from(purchaseDate);
		int effectiveClosingDay = Math.min(closingDay, purchaseMonth.lengthOfMonth());
		return purchaseDate.getDayOfMonth() >= effectiveClosingDay
				? purchaseMonth.plusMonths(1)
				: purchaseMonth;
	}

	private CardInvoice newInvoice(Card card, YearMonth month) {
		LocalDate closingDate = clampToMonth(month, card.getClosingDay());
		// vencimento é sempre depois do fechamento: se o dia de vencimento não vem
		// depois do dia de fechamento dentro do mês, ele cai no mês seguinte
		LocalDate dueDate = card.getDueDay() > card.getClosingDay()
				? clampToMonth(month, card.getDueDay())
				: clampToMonth(month.plusMonths(1), card.getDueDay());
		return new CardInvoice(card.getId(), month.atDay(1), closingDate, dueDate);
	}

	private LocalDate clampToMonth(YearMonth month, int day) {
		return month.atDay(Math.min(day, month.lengthOfMonth()));
	}

}
