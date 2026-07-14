package com.dindin.api.dashboard;

import com.dindin.api.common.security.AuthenticatedUser;
import com.dindin.api.dashboard.dto.AnnualPointResponse;
import com.dindin.api.dashboard.dto.DashboardSummaryResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/v1/dashboard")
public class DashboardController {

	private final DashboardService dashboardService;

	public DashboardController(DashboardService dashboardService) {
		this.dashboardService = dashboardService;
	}

	@GetMapping("/summary")
	public DashboardSummaryResponse summary(@AuthenticationPrincipal AuthenticatedUser user,
			@RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth month) {
		return dashboardService.summary(user.id(), month);
	}

	@GetMapping("/annual")
	public List<AnnualPointResponse> annual(@AuthenticationPrincipal AuthenticatedUser user,
			@RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth month) {
		return dashboardService.annual(user.id(), month);
	}

}
