package com.poupito.api.transaction;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class TransactionSpecifications {

	private TransactionSpecifications() {
	}

	public static Specification<Transaction> search(UUID userId, YearMonth month,
			UUID accountId, UUID categoryId, TransactionType type, String q, String tag) {
		return (root, query, cb) -> {
			List<Predicate> predicates = new ArrayList<>();
			predicates.add(cb.equal(root.get("userId"), userId));
			predicates.add(cb.between(root.get("date"), month.atDay(1), month.atEndOfMonth()));
			if (accountId != null) {
				predicates.add(cb.equal(root.get("accountId"), accountId));
			}
			if (categoryId != null) {
				predicates.add(cb.equal(root.get("categoryId"), categoryId));
			}
			if (type != null) {
				predicates.add(cb.equal(root.get("type"), type));
			}
			if (q != null && !q.isBlank()) {
				predicates.add(cb.like(cb.lower(root.get("description")), "%" + q.trim().toLowerCase() + "%"));
			}
			if (tag != null && !tag.isBlank()) {
				predicates.add(cb.isMember(tag.trim().toLowerCase(), root.get("tags")));
			}
			return cb.and(predicates.toArray(Predicate[]::new));
		};
	}

}
