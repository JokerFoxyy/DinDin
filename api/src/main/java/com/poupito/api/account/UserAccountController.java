package com.poupito.api.account;

import com.poupito.api.common.security.AuthCookieFactory;
import com.poupito.api.common.security.AuthenticatedUser;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Conta do usuário autenticado — direitos do titular (LGPD).
 * (Distinto de /v1/accounts, que são as contas bancárias/cartões.)
 */
@RestController
@RequestMapping("/v1/account")
public class UserAccountController {

	private final UserDataService userDataService;
	private final AuthCookieFactory cookieFactory;

	public UserAccountController(UserDataService userDataService, AuthCookieFactory cookieFactory) {
		this.userDataService = userDataService;
		this.cookieFactory = cookieFactory;
	}

	/** Exportação/portabilidade: todos os dados do usuário em JSON. */
	@GetMapping("/export")
	public ResponseEntity<Map<String, Object>> export(@AuthenticationPrincipal AuthenticatedUser user) {
		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"poupito-meus-dados.json\"")
				.contentType(MediaType.APPLICATION_JSON)
				.body(userDataService.export(user.id()));
	}

	/** Direito de eliminação: apaga o usuário e todos os dados vinculados; encerra a sessão. */
	@DeleteMapping
	public ResponseEntity<Void> deleteAccount(@AuthenticationPrincipal AuthenticatedUser user) {
		userDataService.deleteAccount(user.id());
		return ResponseEntity.noContent()
				.header(HttpHeaders.SET_COOKIE, cookieFactory.clearAccess().toString())
				.header(HttpHeaders.SET_COOKIE, cookieFactory.clearRefresh().toString())
				.build();
	}

}
