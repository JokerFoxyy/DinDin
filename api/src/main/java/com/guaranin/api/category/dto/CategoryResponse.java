package com.guaranin.api.category.dto;

import com.guaranin.api.category.Category;
import com.guaranin.api.category.CategoryKind;

import java.util.UUID;

public record CategoryResponse(UUID id, String name, String icon, String color, CategoryKind kind) {

	public static CategoryResponse from(Category category) {
		return new CategoryResponse(category.getId(), category.getName(), category.getIcon(),
				category.getColor(), category.getKind());
	}

}
