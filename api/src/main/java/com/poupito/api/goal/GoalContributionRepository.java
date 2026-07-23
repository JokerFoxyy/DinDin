package com.poupito.api.goal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GoalContributionRepository extends JpaRepository<GoalContribution, UUID> {

	List<GoalContribution> findAllByGoalIdOrderByMonthAsc(UUID goalId);

	Optional<GoalContribution> findByIdAndGoalId(UUID id, UUID goalId);

	@Query("select coalesce(sum(c.amount), 0) from GoalContribution c where c.goalId = :goalId")
	BigDecimal sumByGoalId(@Param("goalId") UUID goalId);

	@Query("select new com.poupito.api.goal.GoalContributionSum(c.goalId, sum(c.amount)) "
			+ "from GoalContribution c where c.goalId in :goalIds group by c.goalId")
	List<GoalContributionSum> sumByGoalIdIn(@Param("goalIds") List<UUID> goalIds);

}
