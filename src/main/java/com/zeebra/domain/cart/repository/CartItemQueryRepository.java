package com.zeebra.domain.cart.repository;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.zeebra.domain.cart.dto.CartItemOption;
import com.zeebra.domain.cart.dto.CartItemResponse;
import com.zeebra.domain.cart.entity.CartItem;
import com.zeebra.domain.cart.entity.QCart;
import com.zeebra.domain.cart.entity.QCartItem;
import com.zeebra.domain.product.entity.QOptionCombination;
import com.zeebra.domain.product.entity.QOptionName;
import com.zeebra.domain.product.entity.QProduct;
import com.zeebra.domain.product.entity.QProductOption;
import com.zeebra.domain.product.entity.QSales;
import com.zeebra.domain.product.entity.SalesStatus;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class CartItemQueryRepository {
	private final JPAQueryFactory queryFactory;
	private final QCartItem cartItem = QCartItem.cartItem;
	private final QCart cart = QCart.cart;
	private final QSales sales = QSales.sales;
	private final QProductOption productOption = QProductOption.productOption;
	private final QProduct product = QProduct.product;
	private final QOptionName optionName = QOptionName.optionName;
	private final QOptionCombination optionCombination = QOptionCombination.optionCombination;

	public Page<CartItemResponse> findCartItemsByCartId(Long cartId, Pageable pageable) {
		List<CartItem> cartItems = queryFactory
			.selectFrom(cartItem)
			.where(cartItem.cartId.eq(cartId))
			.orderBy(cartItem.createdTime.desc())
			.offset(pageable.getOffset())
			.limit(pageable.getPageSize())
			.fetch();

		if (cartItems.isEmpty()) {
			return new PageImpl<>(List.of(), pageable, 0L);
		}

		Long total = queryFactory
			.select(cartItem.count())
			.from(cartItem)
			.where(cartItem.cartId.eq(cartId))
			.fetchOne();

		List<CartItemResponse> cartItemResponses = cartItems.stream()
			.map(cartItem -> findCartItemResponseByCartItem(cartItem))
			.filter(item -> item != null)
			.collect(Collectors.toList());

		return new PageImpl<>(cartItemResponses, pageable, total != null ? total : 0L);
	}

	private CartItemResponse findCartItemResponseByCartItem(CartItem item) {
		Tuple salesItem = (Tuple) queryFactory
			.select(sales.price, sales.id)
			.from(sales)
			.where(
				sales.productOptionId.eq(item.getProductOptionId()),
				sales.salesStatus.eq(SalesStatus.ON_SALE)
			)
			.orderBy(sales.price.asc(), sales.createdTime.asc())
			.fetchFirst();

		Tuple baseItem = queryFactory
			.select(
				cartItem.productOptionId,
				productOption.productId,
				cartItem.snapShotPrice,
				cartItem.quantity,
				product.name,
				product.thumbnail
			)
			.from(cartItem)
			.join(productOption).on(cartItem.productOptionId.eq(productOption.id))
			.join(product).on(productOption.productId.eq(product.id))
			.where(
				cartItem.id.eq(item.getId())
			)
			.fetchOne();

		if (baseItem == null) {
			return null;
		}

		List<CartItemOption> options = queryFactory
			.select(optionName.name, optionName.value)
			.from(cartItem)
			.join(productOption).on(cartItem.productOptionId.eq(productOption.id))
			.join(optionCombination).on(productOption.id.eq(optionCombination.productOptionId))
			.join(optionName).on(optionCombination.optionNameId.eq(optionName.id))
			.where(cartItem.id.eq(item.getId()))
			.fetch()
			.stream()
			.filter(tuple -> tuple.get(optionName.name) != null)
			.map(tuple -> new CartItemOption(tuple.get(optionName.name), tuple.get(optionName.value)))
			.collect(Collectors.toList());

		boolean isSoldOut = salesItem == null;

		return new CartItemResponse(
			baseItem.get(cartItem.productOptionId),
			baseItem.get(productOption.productId),
			baseItem.get(cartItem.snapShotPrice),
			salesItem != null ? salesItem.get(sales.price) : null,
			baseItem.get(cartItem.quantity),
			isSoldOut,
			baseItem.get(product.name),
			baseItem.get(product.thumbnail),
			options
		);
	}
}