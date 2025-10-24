package com.zeebra.domain.product.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.zeebra.domain.brand.entity.Brand;
import com.zeebra.domain.brand.entity.QBrand;
import com.zeebra.domain.brand.repository.BrandRepository;
import com.zeebra.domain.category.entity.Category;
import com.zeebra.domain.category.entity.QCategory;
import com.zeebra.domain.product.entity.Product;
import com.zeebra.domain.product.entity.QProduct;
import com.zeebra.domain.product.entity.QProductOption;
import com.zeebra.domain.product.entity.QSales;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.data.web.SpringDataWebProperties;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

@RequiredArgsConstructor
@Repository
public class ProductQueryRepository {

    private final QProduct product = QProduct.product;
    private final QBrand brand = QBrand.brand;
    private final QCategory category = QCategory.category;
    private final QProductOption productOption = QProductOption.productOption;
    private final QSales sales = QSales.sales;
    private final JPAQueryFactory queryFactory;

    public List<Product> searchProduct(String keyword, List<Long> categoryIds,
                                       List<Long> brandIds, Pageable pageable) {
        return queryFactory
                .selectDistinct(product)
                .from(product)
                .where(
                        findByCategoryId(categoryIds),
                        findByKeyword(keyword),
                        findByBrandId(brandIds)
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();
    }

    public List<Brand> filteredBrand(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return Collections.emptyList();
        }

        String search = keyword.trim();

        return queryFactory
                .selectDistinct(brand)
                .from(product)
                .join(brand).on(brand.id.eq(product.brandId))
                .join(category).on(category.id.eq(product.categoryId))
                .where(
                        product.name.containsIgnoreCase(search)
                                .or(category.name.containsIgnoreCase(search))
                )
                .fetch();
    }

    public BigDecimal lowPriceOfProduct(Long productId) {
        return queryFactory
                .select(sales.price.min())
                .from(productOption)
                .join(sales).on(sales.productOptionId.eq(productOption.id))
                .where(productOption.productId.eq(productId))
                .fetchOne();
    }

    private BooleanExpression findByCategoryId(List<Long> categoryIds) {
        if (categoryIds == null) {
            return null;
        }
        return product.categoryId.in(categoryIds);
    }

    private BooleanExpression findByKeyword(String keyword) {
        if (keyword == null) {
            return null;
        }
        return product.name.containsIgnoreCase(keyword);
    }

    private BooleanExpression findByBrandId(List<Long> brandIds) {
        if (brandIds == null) {
            return null;
        }
        return product.brandId.in(brandIds);
    }
}
