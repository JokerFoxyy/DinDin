package com.poupito.api.category.dto;

import com.poupito.api.category.Category;
import com.poupito.api.category.CategoryKind;

import java.util.UUID;

public record CategoryResponse(UUID id, String name, String icon, String color, CategoryKind kind) {

	public static CategoryResponse from(Category category) {
		return new CategoryResponse(category.getId(), category.getName(), category.getIcon(),
				category.getColor(), category.getKind());
	}

}
