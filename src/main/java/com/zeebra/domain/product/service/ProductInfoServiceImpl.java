package com.zeebra.domain.product.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.zeebra.domain.order.dto.ProductInfo;
import com.zeebra.domain.product.repository.ProductInfoRepository;

import lombok.RequiredArgsConstructor;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ProductInfoServiceImpl implements ProductInfoService{

	private final ProductInfoRepository productInfoRepository;

	public ProductInfo getProductInfoBySalesId(Long salesId) {
		return productInfoRepository.findProductInfoBySaleId(salesId);
	}
}