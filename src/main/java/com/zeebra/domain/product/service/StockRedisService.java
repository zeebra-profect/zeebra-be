package com.zeebra.domain.product.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import com.zeebra.domain.product.entity.Sales;
import com.zeebra.domain.product.repository.SalesRepository;
import com.zeebra.global.ErrorCode.SalesErrorCode;
import com.zeebra.global.exception.BusinessException;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class StockRedisService {
	private static final String STOCK_KEY_PREFIX = "sales:stock:";
	// Lua 스크립트: 재고 확인 후 차감 (원자적 연산)
	private static final String DECREASE_SCRIPT = """
        local stock = tonumber(redis.call('get', KEYS[1]) or '0')
        local quantity = tonumber(ARGV[1])
        if stock >= quantity then
            return redis.call('decrby', KEYS[1], quantity)
        else
            return -1
        end
        """;

	private final RedisTemplate<String, String> redisTemplate;
	private final SalesRepository salesRepository;

	public StockRedisService(
		@Qualifier("stockRedisTemplate") RedisTemplate<String, String> redisTemplate,
		SalesRepository salesRepository) {
		this.redisTemplate = redisTemplate;
		this.salesRepository = salesRepository;
	}

	/**
	 * 재고 차감 (주문/결제 시)
	 * @return 성공 시 남은 재고, 실패 시 -1
	 */
	public boolean decreaseStock(Long salesId, int quantity) {
		String key = STOCK_KEY_PREFIX + salesId;

		Long result = redisTemplate.execute(
			RedisScript.of(DECREASE_SCRIPT, Long.class),
			List.of(key),
			String.valueOf(quantity)
		);

		if (result == null || result < 0) {
			// DB에서 동기화 후 재시도
			if (syncStockFromDbIfNeeded(salesId)) {
				result = redisTemplate.execute(
					RedisScript.of(DECREASE_SCRIPT, Long.class),
					List.of(key),
					String.valueOf(quantity)
				);
			}
		}

		if (result == null || result < 0) {
			log.warn("[재고 차감 실패] salesId: {}, 요청 수량: {}", salesId, quantity);
			return false;
		}

		log.info("[재고 차감 성공] salesId: {}, 차감: {}, 남은 재고: {}", salesId, quantity, result);
		return true;
	}

	/**
	 * 재고 복구 (결제 실패/취소 시)
	 */
	public void increaseStock(Long salesId, int quantity) {
		String key = STOCK_KEY_PREFIX + salesId;

		if (Boolean.FALSE.equals(redisTemplate.hasKey(key))) {
			syncStockFromDb(salesId);
		}

		Long result = redisTemplate.opsForValue().increment(key, quantity);
		log.info("[재고 복구] salesId: {}, 복구: {}, 현재 재고: {}", salesId, quantity, result);
	}

	/**
	 * 재고 조회
	 */
	public int getStock(Long salesId) {
		String key = STOCK_KEY_PREFIX + salesId;

		if (Boolean.FALSE.equals(redisTemplate.hasKey(key))) {
			syncStockFromDb(salesId);
		}
		String value = redisTemplate.opsForValue().get(key);
		return value != null ? Integer.parseInt(value) : 0;
	}

	/**
	 * 재고 설정 (상품 등록/수정 시 DB → Redis 동기화)
	 */
	public void setStock(Long salesId, int stock) {
		String key = STOCK_KEY_PREFIX + salesId;
		redisTemplate.opsForValue().set(key, String.valueOf(stock));
		log.info("[재고 동기화] salesId: {}, stock: {}", salesId, stock);
	}

	/**
	 * 재고 삭제 (상품 삭제 시)
	 */
	public void deleteStock(Long salesId) {
		String key = STOCK_KEY_PREFIX + salesId;
		redisTemplate.delete(key);
		log.info("[재고 삭제] salesId: {}", salesId);
	}

	/**
	 * DB에서 Redis로 재고 동기화
	 */
	private void syncStockFromDb(Long salesId) {
		Sales sales = salesRepository.findById(salesId)
			.orElseThrow(() -> new BusinessException(SalesErrorCode.SALES_NOT_FOUND));

		setStock(salesId, sales.getStock());
		log.info("[DB → Redis 동기화] salesId: {}, stock: {}", salesId, sales.getStock());
	}

	/**
	 * Redis에 값이 없거나 0이면 DB에서 동기화
	 */
	private boolean syncStockFromDbIfNeeded(Long salesId) {
		String key = STOCK_KEY_PREFIX + salesId;
		String currentValue = redisTemplate.opsForValue().get(key);

		// 키가 없거나 0이면 DB에서 동기화
		if (currentValue == null || "0".equals(currentValue)) {
			Sales sales = salesRepository.findById(salesId)
				.orElseThrow(() -> new BusinessException(SalesErrorCode.SALES_NOT_FOUND));

			if (sales.getStock() > 0) {
				setStock(salesId, sales.getStock());
				log.info("[DB → Redis 동기화] salesId: {}, stock: {}", salesId, sales.getStock());
				return true;
			}
		}
		return false;
	}
}