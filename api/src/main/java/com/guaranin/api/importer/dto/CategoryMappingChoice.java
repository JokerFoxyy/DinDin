package com.guaranin.api.importer.dto;

import com.guaranin.api.category.CategoryKind;

import java.util.UUID;

/** Se {@code existingCategoryId} for null, cria uma categoria nova com o nome bruto e {@code createKind}. */
public record CategoryMappingChoice(UUID existingCategoryId, CategoryKind createKind) {
}
