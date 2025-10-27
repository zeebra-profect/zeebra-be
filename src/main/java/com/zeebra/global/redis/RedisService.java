package com.zeebra.global.redis;

import java.time.Duration;

import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zeebra.global.ErrorCode.RedisErrorCode;
import com.zeebra.global.exception.RedisException;

import io.lettuce.core.RedisConnectionException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisService {
	private static final String BLACKLIST_TOKEN_PREFIX = "blacklist:token:";
	private static final String BLACKLIST_MEMBER_PREFIX = "blacklist:member:";

	private final RedisTemplate<String, String> redisTemplate;
	private final ObjectMapper objectMapper;

	// 블랙리스트 추가
	public void addToBlacklist(String token, long expirationTime, Long memberId) {
		if (token == null || token.isBlank()) {
			log.warn("비어있는 토큰을 블랙리스트에 추가하려고 시도했습니다");
			return;
		}

		long currentTime = System.currentTimeMillis();
		long timeToLive = expirationTime - currentTime;

		if (timeToLive <= 0) {
			log.debug("토큰이 이미 만료되었습니다 (TTL: {}ms), 블랙리스트에 추가하지 않습니다", timeToLive);
			return;
		}

		try {
			String tokenKey = buildTokenKey(token);
			String memberIdValue = memberId != null ? memberId.toString() : "unknown";

			redisTemplate.opsForValue().set(
				tokenKey,
				memberIdValue,
				Duration.ofMillis(timeToLive)
			);

			if (memberId != null) {
				String memberKey = buildMemberTokensKey(memberId);
				redisTemplate.opsForSet().add(memberKey, token);

				Long currentTtl = redisTemplate.getExpire(memberKey);
				if (currentTtl == null || currentTtl < timeToLive / 1000) {
					redisTemplate.expire(memberKey, Duration.ofMillis(timeToLive));
				}
			}

		} catch (RedisConnectionFailureException ex) {
			log.error("Redis 연결 실패로 토큰을 블랙리스트에 추가하지 못했습니다 (memberId: {}). " +
				"로그아웃은 계속 진행되지만 토큰은 블랙리스트에 등록되지 않습니다.", memberId, ex);
		} catch (Exception ex) {
			log.error("토큰을 블랙리스트에 추가하는 중 예상치 못한 오류가 발생했습니다 (memberId: {})", memberId, ex);
		}
	}

	// 블랙리스트 확인
	public boolean isBlacklisted(String token) throws RedisConnectionException {
		if (token == null || token.isBlank()) {
			return false;
		}

		String key = buildTokenKey(token);

		try {
			Boolean exists = redisTemplate.hasKey(key);
			boolean result = Boolean.TRUE.equals(exists);

			return result;
		} catch (RedisConnectionFailureException e) {
			log.error("블랙리스트 확인 중 Redis 연결에 실패했습니다.", e);
			throw new RedisException(RedisErrorCode.REDIS_CONNECTION_ERROR);
		} catch (Exception e) {
			log.error("블랙리스트 확인 중 예상치 못한 오류가 발생했습니다", e);
			throw new RedisException(RedisErrorCode.REDIS_OPERATION_ERROR);
		}
	}

	private String buildTokenKey(String token) {
		return BLACKLIST_TOKEN_PREFIX + token;
	}

	private String buildMemberTokensKey(Long memberId) {
		return BLACKLIST_MEMBER_PREFIX + memberId + ":tokens";
	}
}