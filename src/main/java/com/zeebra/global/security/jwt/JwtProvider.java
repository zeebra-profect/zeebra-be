package com.zeebra.global.security.jwt;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Header;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtProvider {

	public static final String CLAIM_TYPE = "type";
	public static final String CLAIM_LOGIN_ID = "userLoginId";
	public static final String TYPE_ACCESS = "AccessToken";
	public static final String TYPE_REFRESH = "RefreshToken";

	private final Key KEY;
	private final String issuer;
	private final long clockSkewSec;
	public JwtProvider(@Value("${jwt.secret}") String secret, @Value("${jwt.issuer}") String issuer, @Value("${jwt.clockSkewSec}") long clockSkewSec) {
		if (secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("JWT secret must be at least 256 bits");
        }
		this.KEY = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
		this.issuer = issuer;
		this.clockSkewSec = clockSkewSec;
	}

	public String createAccessToken(Long memberId, String userLoginId, String role, long expMin) {
		Instant now = Instant.now();
		return Jwts.builder()
			.setHeaderParam(Header.TYPE, Header.JWT_TYPE)
			.setIssuer(issuer)
			.setSubject(memberId.toString())
			.setIssuedAt(Date.from(now))
			.setExpiration(Date.from(now.plus(expMin, ChronoUnit.MINUTES)))
			.addClaims(Map.of(
				CLAIM_LOGIN_ID, userLoginId,
				CLAIM_TYPE, TYPE_ACCESS,
				"role", role
			))
			.signWith(KEY, SignatureAlgorithm.HS256)
			.compact();
	}

	public String createRefreshToken(Long memberId, String role, long expDay) {
		Instant now = Instant.now();
		return Jwts.builder()
			.setHeaderParam(Header.TYPE, Header.JWT_TYPE)
			.setIssuer(issuer)
			.setSubject(memberId.toString())
			.setIssuedAt(Date.from(now))
			.setExpiration(Date.from(now.plus(expDay, ChronoUnit.DAYS)))
			.addClaims(Map.of(
				CLAIM_TYPE, TYPE_REFRESH,
				"role", role
			))
			.signWith(KEY, SignatureAlgorithm.HS256)
			.compact();
	}

	public Jws<Claims> parse(String token) throws JwtException {
		return Jwts.parserBuilder()
			.requireIssuer(issuer)
			.setAllowedClockSkewSeconds(clockSkewSec)
			.setSigningKey(KEY)
			.build()
			.parseClaimsJws(token);
	}

	public boolean isValid(String token) {
		try {
			parse(token);
			return true;
		} catch (JwtException | IllegalArgumentException e) {
			return false;
		}
	}

	public boolean isAccessToken(String token) {
		try {
			return "AccessToken".equals(parse(token).getBody().get("type", String.class));
		} catch (JwtException e) { return false; }
	}

	public boolean isRefreshToken(String token) {
		try {
			return "RefreshToken".equals(parse(token).getBody().get("type", String.class));
		} catch (JwtException e) { return false; }
	}

	public boolean isExpired(String token) {
		try {
			parse(token);
			return false;
		} catch (ExpiredJwtException e) {
			return true;
		} catch (JwtException | IllegalArgumentException e) {
			return false;
		}
	}


	public Long getSubjectAsLong(String token) {
		try {
			Jws<Claims> jws = parse(token);
			String subject = jws.getBody().getSubject();
			return subject != null ? Long.parseLong(subject) : null;
		} catch (JwtException | NumberFormatException e) {
			return null;
		}
	}

	public Authentication toAuthentication(String token) {
		Jws<Claims> jws = parse(token);
		Claims claims = jws.getBody();

		String subject = claims.getSubject();
		Long memberId = subject != null ? Long.parseLong(subject) : null;
		String userLoginId = claims.get(CLAIM_LOGIN_ID, String.class);
		String role = claims.get("role", String.class);

		SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + role);

		JwtUserPrincipal principal = new JwtUserPrincipal(memberId, userLoginId);
		return new UsernamePasswordAuthenticationToken(principal, token, List.of(authority));
	}

	public static final class JwtUserPrincipal {
		private final Long memberId;
		private final String userLoginId;
		public JwtUserPrincipal(Long memberId, String userLoginId) {
			this.memberId = memberId;
			this.userLoginId = userLoginId;
		}
		public Long getMemberId() { return memberId; }
		public String getUserLoginId() { return userLoginId; }
	}
}