package com.zeebra.domain.category.service;

import java.util.List;

import com.zeebra.domain.category.dto.CategoryRequest;
import com.zeebra.domain.category.dto.CategoryResponse;
import com.zeebra.global.ApiResponse;

public interface CategoryService {

    public ApiResponse<CategoryResponse> createCategory(Long memberId, CategoryRequest request);

	List<CategoryResponse> selectCategories();
}