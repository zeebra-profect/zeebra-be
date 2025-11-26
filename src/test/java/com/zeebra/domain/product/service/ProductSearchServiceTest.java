//package com.zeebra.domain.product.service;
//
//import com.zeebra.FullTextSearchTestSupport;
//import com.zeebra.domain.brand.entity.Brand;
//import com.zeebra.domain.brand.repository.BrandRepository;
//import com.zeebra.domain.category.entity.Category;
//import com.zeebra.domain.category.repository.CategoryRepository;
//import com.zeebra.domain.product.dto.SearchProductResponse;
//import com.zeebra.domain.product.entity.Product;
//import com.zeebra.domain.product.repository.ProductRepository;
//import com.zeebra.global.ApiResponse;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.data.domain.PageRequest;
//import org.springframework.data.domain.Pageable;
//
//import java.util.List;
//
//import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
//
//public class ProductSearchServiceTest extends FullTextSearchTestSupport {
//
//    @Autowired
//    private ProductRepository productRepository;
//
//    @Autowired
//    private BrandRepository brandRepository;
//
//    @Autowired
//    private CategoryRepository categoryRepository;
//
//    @Autowired
//    private ProductService productService;
//
//    @Test
//    @DisplayName("감성가먼트로 검색하면 감성가먼트 상품 10개가 반환된다")
//    void searchProduct_byBrandName() {
//        Pageable pageable = PageRequest.of(0, 10);
//
//        // when
//        ApiResponse<SearchProductResponse> response =
//                productService.searchProduct("감성가먼트", null, null, pageable, null);
//
//        // then
//        assertThat(response.getData().productDetailResponses().size()).isEqualTo(10);
//        assertThat(response.getData().brandResponses().size()).isEqualTo(1);
//        assertThat(response.getData().brandResponses().getFirst().brandName())
//                .isEqualTo("감성가먼트");
//        assertThat(response.getData().categoryResponses().size()).isEqualTo(1);
//        assertThat(response.getData().categoryResponses().getFirst().categoryName()).isEqualTo("스킨케어");
//    }
//
//    private Product createProduct(String productName) {
//        return Product.builder()
//                .thumbnail("testImage")
//                .images(List.of("testImage1", "testImage2"))
//                .name(productName)
//                .brandId(1L)
//                .categoryId(1L)
//                .modelNumber("testModelNumber")
//                .build();
//    }
//}
