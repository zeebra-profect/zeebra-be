package com.zeebra.domain.category.controller;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.zeebra.domain.category.dto.CategoryRequest;
import com.zeebra.domain.category.dto.CategoryResponse;
import com.zeebra.domain.category.service.CategoryService;
import com.zeebra.global.ApiResponse;
import com.zeebra.global.security.jwt.JwtProvider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequiredArgsConstructor
@Slf4j
public class CategoryController {

    private final CategoryService categoryService;

    @PostMapping("/api/categories")
    public ApiResponse<CategoryResponse> createCategory(@AuthenticationPrincipal JwtProvider.JwtUserPrincipal principal,
                                                        @RequestBody CategoryRequest request) {
        Long memberId = principal.getMemberId();
        return categoryService.createCategory(memberId,request);
    }

	@GetMapping("/api/categories")
	public ApiResponse<List<CategoryResponse>> getCategories() {
		return ApiResponse.success(categoryService.selectCategories());
	}
}