package com.guaranin.api.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
		@NotBlank @Email @Size(max = 254) String email,
		@NotBlank
		@Size(min = 10, max = 100)
		@Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$",
				message = "A senha deve ter ao menos 10 caracteres, incluindo letra e número")
		String password) {
}
