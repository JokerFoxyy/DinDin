package com.guaranin.api.category.dto;

import com.guaranin.api.category.CategoryKind;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CategoryRequest(
		@NotBlank @Size(max = 60) String name,
		@Size(max = 16) String icon,
		@Pattern(regexp = "^#[0-9a-fA-F]{6}$", message = "Cor deve estar no formato #rrggbb") String color,
		@NotNull CategoryKind kind) {
}
