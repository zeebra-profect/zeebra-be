package com.zeebra.domain.cart.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.zeebra.domain.cart.dto.CartItemInfo;
import com.zeebra.domain.cart.dto.CartItemResponse;
import com.zeebra.domain.cart.dto.CartRequest;
import com.zeebra.domain.cart.dto.CartResponse;
import com.zeebra.domain.cart.dto.GetCartResponse;
import com.zeebra.domain.cart.entity.Cart;
import com.zeebra.domain.cart.entity.CartItem;
import com.zeebra.domain.cart.repository.CartItemQueryRepository;
import com.zeebra.domain.cart.repository.CartItemRepository;
import com.zeebra.domain.cart.repository.CartRepository;
import com.zeebra.domain.member.entity.Member;
import com.zeebra.domain.member.repository.MemberRepository;
import com.zeebra.domain.product.entity.ProductOption;
import com.zeebra.domain.product.repository.OptionCombinationRepository;
import com.zeebra.domain.product.repository.ProductOptionRepository;
import com.zeebra.domain.product.repository.SalesQueryRepository;
import com.zeebra.global.ApiResponse;
import com.zeebra.global.ErrorCode.CommonErrorCode;
import com.zeebra.global.exception.BusinessException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartServiceImp implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
	private final CartItemQueryRepository cartItemQueryRepository;
    private final MemberRepository memberRepository;
    private final OptionCombinationRepository optionCombinationRepository;
    private final ProductOptionRepository productOptionRepository;
    private final SalesQueryRepository salesQueryRepository;

    private CartResponse toCartResponse(CartItem cartItem) {
        return new CartResponse(cartItem.getCartId());
    }

    private CartItem toCartItem(Cart cart, ProductOption productOption, BigDecimal snapShotPrice, CartRequest request) {
        return new CartItem(
                cart.getId(),
                productOption.getId(),
                snapShotPrice,
                request.quantity());
    }

    @Transactional
    @Override
    public ApiResponse<CartResponse> addCartItem(Long memberId, Long productOptionId, CartRequest request) {
        try {
            Member member = memberRepository.findById(memberId).orElseThrow(
                    () -> new NoSuchElementException("해당하는 사용자가 존재하지 않습니다."));

            Cart cart = cartRepository.findByMemberId(member.getId()).orElseThrow(
                    () -> new NoSuchElementException("해당하는 장바구니가 존재하지 않습니다."));

            ProductOption productOption = productOptionRepository.findById(productOptionId).orElseThrow(
                    () -> new NoSuchElementException("해당하는 상품 옵션이 없습니다."));

            BigDecimal snapShotPrice = salesQueryRepository.cheapestSalesPrice(productOption.getId());

            CartItem cartItem = toCartItem(cart, productOption, snapShotPrice, request);
			cartItemRepository.save(cartItem);

            return ApiResponse.success(toCartResponse(cartItem));
        } catch (NoSuchElementException e) {
            return ApiResponse.error(null, e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error(null, "장바구니에 상품을 담는 과정에서 오류가 발생했습니다.");
        }
    }

    @Override
    @Transactional
    public ApiResponse<Void> deleteCartItem(Long memberId, Long productOptionId) {
        try {
            Member member = memberRepository.findById(memberId).orElseThrow(
                    () -> new NoSuchElementException("해당하는 사용자가 없습니다."));

            ProductOption productOption = productOptionRepository.findById(productOptionId).orElseThrow(
                    () -> new NoSuchElementException("해당하는 상품 상세가 없습니다."));

            Cart cart = cartRepository.findByMemberId(member.getId()).orElseThrow(
                    () -> new NoSuchElementException("해당하는 장바구니가 없습니다."));

            CartItem cartItem = cartItemRepository.findByCartIdAndProductOptionId(cart.getId(), productOption.getId()).orElseThrow(
                    () -> new NoSuchElementException("해당하는 장바구니 상품이 없습니다."));

            cartItemRepository.delete(cartItem);

            return ApiResponse.successMessage("장바구니에서 상품을 삭제하는데 성공했습니다.");
        } catch (NoSuchElementException e) {
            return ApiResponse.error(null, e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error(null, "장바구니에서 상품을 삭제하는 과정에서 오류가 발생했습니다.");
        }
    }

	@Override
	public GetCartResponse getCartItems(Long memberId, Pageable pageable) {
		if (memberId == null) { throw new BusinessException(CommonErrorCode.INVALID_REQUEST); }

		Long cartId = cartRepository.findByMemberId(memberId).orElseThrow(() -> new BusinessException(CommonErrorCode.INVALID_REQUEST)).getId();

		Page<CartItemResponse> cartItemPage = cartItemQueryRepository.findCartItemsByCartId(cartId, pageable);

		List<CartItemResponse> cartItemResponses = cartItemPage.getContent();

		BigDecimal totalPrice = cartItemResponses.stream().map(CartItemResponse::snapShotPrice).reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal discount = BigDecimal.ZERO;
		int totalQuantity = cartItemResponses.stream().mapToInt(CartItemResponse::quantity).sum();


		return GetCartResponse.of(cartId, totalPrice, discount, totalQuantity, cartItemPage);
	}

	public List<CartItemInfo> getCartItemsByCartId(Long cartId, Long memberId) {
		if (memberId == null || cartId == null) { throw new BusinessException(CommonErrorCode.INVALID_REQUEST); }
		cartRepository.findById(cartId).orElseThrow(() -> new BusinessException(CommonErrorCode.INVALID_REQUEST, "해당 장바구니를 찾을 수 없습니다."));
		return CartItemInfo.of(cartItemRepository.findByCartId(cartId));
	}
}