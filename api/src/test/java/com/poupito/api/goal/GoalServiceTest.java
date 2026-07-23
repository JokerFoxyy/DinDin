package com.poupito.api.goal;

import com.poupito.api.common.error.NotFoundException;
import com.poupito.api.goal.dto.GoalContributionRequest;
import com.poupito.api.goal.dto.GoalContributionResponse;
import com.poupito.api.goal.dto.GoalRequest;
import com.poupito.api.goal.dto.GoalResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
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
class GoalServiceTest {

	private final UUID userId = UUID.randomUUID();
	private final UUID goalId = UUID.randomUUID();

	@Mock
	private GoalRepository goalRepository;
	@Mock
	private GoalContributionRepository contributionRepository;

	private GoalService service;

	private Goal goal;

	@BeforeEach
	void setUp() {
		service = new GoalService(goalRepository, contributionRepository, new RequiredContributionCalculator());
		goal = new Goal(userId, "Reserva de emergência", new BigDecimal("12000.00"),
				YearMonth.now().plusMonths(5).atDay(1));
		ReflectionTestUtils.setField(goal, "id", goalId);
	}

	@Test
	void shouldReturnEmptyList_whenUserHasNoGoals() {
		when(goalRepository.findAllByUserIdOrderByCreatedAtAsc(userId)).thenReturn(List.of());

		assertThat(service.list(userId)).isEmpty();
	}

	@Test
	void shouldComputeAccumulatedAndRequiredContribution() {
		when(goalRepository.findAllByUserIdOrderByCreatedAtAsc(userId)).thenReturn(List.of(goal));
		when(contributionRepository.sumByGoalIdIn(List.of(goalId)))
				.thenReturn(List.of(new GoalContributionSum(goalId, new BigDecimal("7200.00"))));

		List<GoalResponse> goals = service.list(userId);

		assertThat(goals).hasSize(1);
		assertThat(goals.getFirst().accumulated()).isEqualByComparingTo("7200.00");
		assertThat(goals.getFirst().requiredMonthlyContribution()).isEqualByComparingTo("960.00");
	}

	@Test
	void shouldCreateGoal() {
		when(goalRepository.save(any(Goal.class))).thenAnswer(invocation -> {
			Goal saved = invocation.getArgument(0);
			ReflectionTestUtils.setField(saved, "id", goalId);
			return saved;
		});

		GoalResponse response = service.create(userId,
				new GoalRequest("Reserva", new BigDecimal("12000.00"), YearMonth.now().plusMonths(5).atDay(1)));

		assertThat(response.id()).isEqualTo(goalId);
		assertThat(response.accumulated()).isEqualByComparingTo("0");
	}

	@Test
	void shouldUpdateGoal_whenOwnedByUser() {
		when(goalRepository.findByIdAndUserId(goalId, userId)).thenReturn(Optional.of(goal));
		when(contributionRepository.sumByGoalId(goalId)).thenReturn(new BigDecimal("1000.00"));

		GoalResponse response = service.update(userId, goalId,
				new GoalRequest("Reserva maior", new BigDecimal("20000.00"), YearMonth.now().plusMonths(10).atDay(1)));

		assertThat(response.name()).isEqualTo("Reserva maior");
		assertThat(response.targetAmount()).isEqualByComparingTo("20000.00");
	}

	@Test
	void shouldThrowNotFound_whenUpdatingGoalNotOwned() {
		when(goalRepository.findByIdAndUserId(goalId, userId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.update(userId, goalId,
				new GoalRequest("X", BigDecimal.TEN, YearMonth.now().atDay(1))))
				.isInstanceOf(NotFoundException.class);
	}

	@Test
	void shouldDeleteGoal_whenOwnedByUser() {
		when(goalRepository.findByIdAndUserId(goalId, userId)).thenReturn(Optional.of(goal));

		service.delete(userId, goalId);

		ArgumentCaptor<Goal> captor = ArgumentCaptor.forClass(Goal.class);
		verify(goalRepository).delete(captor.capture());
		assertThat(captor.getValue().getId()).isEqualTo(goalId);
	}

	@Test
	void shouldThrowNotFound_whenDeletingGoalNotOwned() {
		when(goalRepository.findByIdAndUserId(goalId, userId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.delete(userId, goalId)).isInstanceOf(NotFoundException.class);
	}

	@Test
	void shouldListContributions_whenGoalOwnedByUser() {
		GoalContribution contribution = new GoalContribution(goalId, LocalDate.of(2026, 7, 1), new BigDecimal("800.00"));
		when(goalRepository.findByIdAndUserId(goalId, userId)).thenReturn(Optional.of(goal));
		when(contributionRepository.findAllByGoalIdOrderByMonthAsc(goalId)).thenReturn(List.of(contribution));

		List<GoalContributionResponse> contributions = service.listContributions(userId, goalId);

		assertThat(contributions).hasSize(1);
		assertThat(contributions.getFirst().amount()).isEqualByComparingTo("800.00");
	}

	@Test
	void shouldThrowNotFound_whenListingContributionsOfGoalNotOwned() {
		when(goalRepository.findByIdAndUserId(goalId, userId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.listContributions(userId, goalId)).isInstanceOf(NotFoundException.class);
	}

	@Test
	void shouldCreateContribution_whenGoalOwnedByUser() {
		when(goalRepository.findByIdAndUserId(goalId, userId)).thenReturn(Optional.of(goal));
		when(contributionRepository.save(any(GoalContribution.class))).thenAnswer(invocation -> invocation.getArgument(0));

		GoalContributionResponse response = service.createContribution(userId, goalId,
				new GoalContributionRequest(YearMonth.of(2026, 7), new BigDecimal("800.00")));

		assertThat(response.amount()).isEqualByComparingTo("800.00");
		assertThat(response.month()).isEqualTo(LocalDate.of(2026, 7, 1));
	}

	@Test
	void shouldDeleteContribution_whenOwnedByUser() {
		UUID contributionId = UUID.randomUUID();
		GoalContribution contribution = new GoalContribution(goalId, LocalDate.of(2026, 7, 1), new BigDecimal("800.00"));
		ReflectionTestUtils.setField(contribution, "id", contributionId);
		when(goalRepository.findByIdAndUserId(goalId, userId)).thenReturn(Optional.of(goal));
		when(contributionRepository.findByIdAndGoalId(contributionId, goalId)).thenReturn(Optional.of(contribution));

		service.deleteContribution(userId, goalId, contributionId);

		verify(contributionRepository).delete(contribution);
	}

	@Test
	void shouldThrowNotFound_whenDeletingContributionThatDoesNotExist() {
		UUID contributionId = UUID.randomUUID();
		when(goalRepository.findByIdAndUserId(goalId, userId)).thenReturn(Optional.of(goal));
		when(contributionRepository.findByIdAndGoalId(contributionId, goalId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.deleteContribution(userId, goalId, contributionId))
				.isInstanceOf(NotFoundException.class);
		verify(contributionRepository, never()).delete(any());
	}

}
