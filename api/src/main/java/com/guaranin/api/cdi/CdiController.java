package com.guaranin.api.cdi;

import com.guaranin.api.cdi.dto.CdiPointResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/v1/investments/cdi")
public class CdiController {

	private final CdiService cdiService;

	public CdiController(CdiService cdiService) {
		this.cdiService = cdiService;
	}

	@GetMapping
	public List<CdiPointResponse> accumulated(
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
		return cdiService.accumulatedSeries(from, to);
	}

}
