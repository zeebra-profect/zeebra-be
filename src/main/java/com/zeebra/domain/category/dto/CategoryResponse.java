package com.zeebra.domain.category.dto;

import java.util.List;

import com.zeebra.domain.category.entity.Category;

import lombok.Builder;

public record CategoryResponse(
        Long categoryId,
        Long parentCategoryId,
        String categoryName,
		String categoryThumbnail
) {

	public CategoryResponse(Long categoryId, Long parentCategoryId, String categoryName) {
		this(categoryId, parentCategoryId, categoryName, null);
	}

	@Builder
	public CategoryResponse(Long categoryId, Long parentCategoryId, String categoryName, String categoryThumbnail) {
		this.categoryId = categoryId;
		this.parentCategoryId = parentCategoryId;
		this.categoryName = categoryName;
		this.categoryThumbnail = categoryThumbnail;
	}

	public static CategoryResponse of(Category category) {
		return CategoryResponse.builder()
			.categoryId(category.getId())
			.parentCategoryId(category.getParentId())
			.categoryName(category.getName())
			.categoryThumbnail(category.getThumbnail())
			.build();
	}

	public static List<CategoryResponse> of(List<Category> categories) {
		return categories.stream()
			.map(CategoryResponse::of)
			.toList();
	}
}