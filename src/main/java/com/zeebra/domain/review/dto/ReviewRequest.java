package com.zeebra.domain.review.dto;

import java.util.List;

public record ReviewRequest(
        List<String> images,
        String content
) {
}
