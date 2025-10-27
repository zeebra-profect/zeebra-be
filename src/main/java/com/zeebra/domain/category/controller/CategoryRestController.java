package com.zeebra.domain.category.controller;

import com.zeebra.domain.category.dto.CategoryRequest;
import com.zeebra.domain.category.dto.CategoryResponse;
import com.zeebra.domain.category.service.CategoryService;
import com.zeebra.global.ApiResponse;
import com.zeebra.global.security.jwt.JwtProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Slf4j
public class CategoryRestController {

    private final CategoryService categoryService;

    @PostMapping("/api/categories")
    public ApiResponse<CategoryResponse> createCategory(@AuthenticationPrincipal JwtProvider.JwtUserPrincipal principal,
                                                        @RequestBody CategoryRequest request) {
        Long memberId = principal.getMemberId();
        return categoryService.createCategory(memberId,request);
    }
}
