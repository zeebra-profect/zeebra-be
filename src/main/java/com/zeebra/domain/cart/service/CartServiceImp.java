package com.zeebra.domain.cart.service;

import com.zeebra.domain.cart.dto.CartRequest;
import com.zeebra.domain.cart.dto.CartResponse;
import com.zeebra.domain.cart.entity.Cart;
import com.zeebra.domain.cart.entity.CartItem;
import com.zeebra.domain.cart.repository.CartItemRepository;
import com.zeebra.domain.cart.repository.CartRepository;
import com.zeebra.domain.member.entity.Member;
import com.zeebra.domain.member.repository.MemberRepository;
import com.zeebra.domain.product.entity.OptionCombination;
import com.zeebra.domain.product.entity.ProductOption;
import com.zeebra.domain.product.repository.OptionCombinationRepository;
import com.zeebra.domain.product.repository.ProductOptionRepository;
import com.zeebra.domain.product.repository.SalesQueryRepository;
import com.zeebra.global.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartServiceImp implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
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

            return ApiResponse.success(toCartResponse(cartItem));
        } catch (NoSuchElementException e) {
            return ApiResponse.error(null, e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error(null, "장바구니에 상품을 담는 과정에서 오류가 발생했습니다.");
        }
    }
}
