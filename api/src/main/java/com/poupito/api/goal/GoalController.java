package com.poupito.api.goal;

import com.poupito.api.common.security.AuthenticatedUser;
import com.poupito.api.goal.dto.GoalContributionRequest;
import com.poupito.api.goal.dto.GoalContributionResponse;
import com.poupito.api.goal.dto.GoalRequest;
import com.poupito.api.goal.dto.GoalResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/goals")
public class GoalController {

	private final GoalService goalService;

	public GoalController(GoalService goalService) {
		this.goalService = goalService;
	}

	@GetMapping
	public List<GoalResponse> list(@AuthenticationPrincipal AuthenticatedUser user) {
		return goalService.list(user.id());
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public GoalResponse create(@AuthenticationPrincipal AuthenticatedUser user, @Valid @RequestBody GoalRequest request) {
		return goalService.create(user.id(), request);
	}

	@PutMapping("/{id}")
	public GoalResponse update(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id,
			@Valid @RequestBody GoalRequest request) {
		return goalService.update(user.id(), id, request);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id) {
		goalService.delete(user.id(), id);
	}

	@GetMapping("/{id}/contributions")
	public List<GoalContributionResponse> listContributions(@AuthenticationPrincipal AuthenticatedUser user,
			@PathVariable UUID id) {
		return goalService.listContributions(user.id(), id);
	}

	@PostMapping("/{id}/contributions")
	@ResponseStatus(HttpStatus.CREATED)
	public GoalContributionResponse createContribution(@AuthenticationPrincipal AuthenticatedUser user,
			@PathVariable UUID id, @Valid @RequestBody GoalContributionRequest request) {
		return goalService.createContribution(user.id(), id, request);
	}

	@DeleteMapping("/{id}/contributions/{contributionId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteContribution(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id,
			@PathVariable UUID contributionId) {
		goalService.deleteContribution(user.id(), id, contributionId);
	}

}
