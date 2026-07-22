package com.guaranin.api.goal;

import com.guaranin.api.common.error.NotFoundException;
import com.guaranin.api.goal.dto.GoalContributionRequest;
import com.guaranin.api.goal.dto.GoalContributionResponse;
import com.guaranin.api.goal.dto.GoalRequest;
import com.guaranin.api.goal.dto.GoalResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class GoalService {

	private final GoalRepository goalRepository;
	private final GoalContributionRepository contributionRepository;
	private final RequiredContributionCalculator calculator;

	public GoalService(GoalRepository goalRepository, GoalContributionRepository contributionRepository,
			RequiredContributionCalculator calculator) {
		this.goalRepository = goalRepository;
		this.contributionRepository = contributionRepository;
		this.calculator = calculator;
	}

	@Transactional(readOnly = true)
	public List<GoalResponse> list(UUID userId) {
		List<Goal> goals = goalRepository.findAllByUserIdOrderByCreatedAtAsc(userId);
		if (goals.isEmpty()) {
			return List.of();
		}
		List<UUID> goalIds = goals.stream().map(Goal::getId).toList();
		Map<UUID, BigDecimal> accumulatedByGoal = contributionRepository.sumByGoalIdIn(goalIds).stream()
				.collect(Collectors.toMap(GoalContributionSum::goalId, GoalContributionSum::total));
		YearMonth currentMonth = YearMonth.now();
		return goals.stream()
				.map(goal -> toResponse(goal, accumulatedByGoal.getOrDefault(goal.getId(), BigDecimal.ZERO), currentMonth))
				.toList();
	}

	@Transactional
	public GoalResponse create(UUID userId, GoalRequest request) {
		Goal goal = goalRepository.save(new Goal(userId, request.name(), request.targetAmount(), request.targetDate()));
		return toResponse(goal, BigDecimal.ZERO, YearMonth.now());
	}

	@Transactional
	public GoalResponse update(UUID userId, UUID goalId, GoalRequest request) {
		Goal goal = findOwned(userId, goalId);
		goal.update(request.name(), request.targetAmount(), request.targetDate());
		BigDecimal accumulated = contributionRepository.sumByGoalId(goalId);
		return toResponse(goal, accumulated, YearMonth.now());
	}

	@Transactional
	public void delete(UUID userId, UUID goalId) {
		goalRepository.delete(findOwned(userId, goalId));
	}

	@Transactional(readOnly = true)
	public List<GoalContributionResponse> listContributions(UUID userId, UUID goalId) {
		findOwned(userId, goalId);
		return contributionRepository.findAllByGoalIdOrderByMonthAsc(goalId).stream()
				.map(GoalContributionResponse::from)
				.toList();
	}

	@Transactional
	public GoalContributionResponse createContribution(UUID userId, UUID goalId, GoalContributionRequest request) {
		findOwned(userId, goalId);
		GoalContribution contribution = contributionRepository.save(
				new GoalContribution(goalId, request.month().atDay(1), request.amount()));
		return GoalContributionResponse.from(contribution);
	}

	@Transactional
	public void deleteContribution(UUID userId, UUID goalId, UUID contributionId) {
		findOwned(userId, goalId);
		GoalContribution contribution = contributionRepository.findByIdAndGoalId(contributionId, goalId)
				.orElseThrow(() -> new NotFoundException("Aporte não encontrado"));
		contributionRepository.delete(contribution);
	}

	private GoalResponse toResponse(Goal goal, BigDecimal accumulated, YearMonth currentMonth) {
		BigDecimal required = calculator.compute(goal.getTargetAmount(), accumulated, currentMonth,
				YearMonth.from(goal.getTargetDate()));
		return GoalResponse.from(goal, accumulated, required);
	}

	private Goal findOwned(UUID userId, UUID goalId) {
		return goalRepository.findByIdAndUserId(goalId, userId)
				.orElseThrow(() -> new NotFoundException("Meta não encontrada"));
	}

}
