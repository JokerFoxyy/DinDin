package com.poupito.api.importer;

import com.poupito.api.common.error.BusinessException;
import com.poupito.api.common.security.AuthenticatedUser;
import com.poupito.api.importer.dto.ImportCommitResponse;
import com.poupito.api.importer.dto.ImportMappingRequest;
import com.poupito.api.importer.dto.ImportPreviewResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/v1/import")
public class ImportController {

	private final ImportService importService;

	public ImportController(ImportService importService) {
		this.importService = importService;
	}

	@PostMapping("/preview")
	public ImportPreviewResponse preview(@AuthenticationPrincipal AuthenticatedUser user,
			@RequestPart("file") MultipartFile file,
			@RequestParam(defaultValue = "2026") int year) {
		try {
			return importService.preview(user.id(), file.getInputStream(), year);
		} catch (IOException e) {
			throw new BusinessException("Não foi possível ler o arquivo enviado");
		}
	}

	@PostMapping("/commit")
	public ImportCommitResponse commit(@AuthenticationPrincipal AuthenticatedUser user,
			@RequestPart("file") MultipartFile file,
			@RequestPart(value = "mapping", required = false) ImportMappingRequest mapping,
			@RequestParam(defaultValue = "2026") int year) {
		try {
			return importService.commit(user.id(), file.getInputStream(), year,
					mapping != null ? mapping : ImportMappingRequest.empty());
		} catch (IOException e) {
			throw new BusinessException("Não foi possível ler o arquivo enviado");
		}
	}

}
