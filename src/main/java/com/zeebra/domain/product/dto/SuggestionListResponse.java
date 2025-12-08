package com.zeebra.domain.product.dto;

import java.util.List;

public record SuggestionListResponse(
        List<String> suggestions
) {
}
