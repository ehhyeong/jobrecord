package com.jobproj.api.security;

import com.jobproj.api.repo.UserRepo;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Base64;
import java.util.Date;
import javax.crypto.SecretKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
public class JwtTokenProvider {

  private final SecretKey key;
  private final long validityInMilliseconds;
  private final long refreshValidityInMilliseconds;

  // 시계 오차 허용(60초)
  private static final long ALLOWED_CLOCK_SKEW_MS = 60_000L;

  public JwtTokenProvider(
      @Value("${jwt.secret-key}") String secretKey,
      @Value("${jwt.expiration-ms}") long validityInMilliseconds,
      @Value("${jwt.refresh-expiration-ms:1209600000}") long refreshValidityInMilliseconds) {

    byte[] keyBytes = Base64.getDecoder().decode(secretKey);
    this.key = Keys.hmacShaKeyFor(keyBytes);
    this.validityInMilliseconds = validityInMilliseconds;
    this.refreshValidityInMilliseconds = refreshValidityInMilliseconds;
  }

  // ===== 발급 =====

  public String createAccessToken(UserRepo.UserRow user) {
    Date now = new Date();
    Date validity = new Date(now.getTime() + validityInMilliseconds);
    return Jwts.builder()
        .setSubject(user.email)
        .claim("name", user.name)
        .claim("role", user.role.name())
        .setIssuedAt(now)
        .setExpiration(validity)
        .signWith(key, SignatureAlgorithm.HS256)
        .compact();
  }

  // (호환) 기존 호출부 유지
  public String createToken(UserRepo.UserRow user) {
    return createAccessToken(user);
  }

  public String createRefreshToken(UserRepo.UserRow user) {
    Date now = new Date();
    Date validity = new Date(now.getTime() + refreshValidityInMilliseconds);
    return Jwts.builder()
        .setSubject(user.email)
        .claim("typ", "refresh")
        .claim("role", user.role.name())
        .setIssuedAt(now)
        .setExpiration(validity)
        .signWith(key, SignatureAlgorithm.HS256)
        .compact();
  }

  // ===== 추출 =====
  // 융합프로젝트 김태형 9주차 OpenAPI 스펙 확정 : 쿠키 기반 인증 정식 지원 (추가)
  // access_token 쿠키 → Authorization: Bearer 순으로 토큰을 추출
  public String resolveAccessToken(HttpServletRequest request) { // (추가)
    // 1) Cookie: access_token
    if (request.getCookies() != null) {
      for (Cookie c : request.getCookies()) {
        if ("access_token".equals(c.getName()) && StringUtils.hasText(c.getValue())) {
          return c.getValue();
        }
      }
    }
    // 2) Header: Authorization
    String auth = request.getHeader("Authorization");
    if (StringUtils.hasText(auth)) {
      return auth.startsWith("Bearer ") ? auth.substring(7) : auth.trim();
    }
    return null;
  }

  // (수정) 기존 resolveToken은 새 메서드로 위임해 호환 유지
  public String resolveToken(HttpServletRequest request) { // (수정)
    return resolveAccessToken(request);
  }

  // 요청에서 Refresh 토큰 추출 (쿠키 RT 우선, 그다음 Authorization)
  public String resolveRefreshToken(HttpServletRequest request) {
    if (request.getCookies() != null) {
      for (Cookie c : request.getCookies()) {
        // 운영에서 cookie 이름을 refresh_token으로 바꾸면 여기만 맞춰주면 됨
        if ("RT".equals(c.getName()) && StringUtils.hasText(c.getValue())) {
          return c.getValue();
        }
        if ("refresh_token".equals(c.getName()) && StringUtils.hasText(c.getValue())) { // (추가, 호환)
          return c.getValue();
        }
      }
    }
    String auth = request.getHeader("Authorization");
    if (StringUtils.hasText(auth)) {
      return auth.startsWith("Bearer ") ? auth.substring(7) : auth.trim();
    }
    return null;
  }

  // ===== 검증/파싱 =====

  public boolean validateToken(String token) {
    try {
      Jws<Claims> claims =
          Jwts.parserBuilder()
              .setSigningKey(key)
              .setAllowedClockSkewSeconds(ALLOWED_CLOCK_SKEW_MS / 1000)
              .build()
              .parseClaimsJws(token);

      Date exp = claims.getBody().getExpiration();
      if (exp == null) {
        log.warn("[JWT] no expiration claim");
        return false;
      }
      return true; // 파서 통과 = 유효
    } catch (io.jsonwebtoken.ExpiredJwtException e) {
      log.warn("[JWT] expired at {}", e.getClaims().getExpiration());
      return false;
    } catch (JwtException | IllegalArgumentException e) {
      log.warn("[JWT] invalid token: {}", e.getMessage());
      return false;
    }
  }

  public boolean validateRefreshToken(String token) {
    try {
      Jws<Claims> claims =
          Jwts.parserBuilder()
              .setSigningKey(key)
              .setAllowedClockSkewSeconds(ALLOWED_CLOCK_SKEW_MS / 1000)
              .build()
              .parseClaimsJws(token);
      Claims body = claims.getBody();
      if (!"refresh".equals(body.get("typ"))) {
        log.warn("[JWT] not a refresh token");
        return false;
      }
      return true;
    } catch (io.jsonwebtoken.ExpiredJwtException e) {
      log.warn("[JWT] refresh expired at {}", e.getClaims().getExpiration());
      return false;
    } catch (JwtException | IllegalArgumentException e) {
      log.warn("[JWT] invalid refresh token: {}", e.getMessage());
      return false;
    }
  }

  public String getEmail(String token) {
    Claims payload =
        Jwts.parserBuilder()
            .setSigningKey(key)
            .setAllowedClockSkewSeconds(ALLOWED_CLOCK_SKEW_MS / 1000)
            .build()
            .parseClaimsJws(token)
            .getBody();
    String emailClaim = payload.get("email", String.class);
    return (emailClaim != null && !emailClaim.isBlank()) ? emailClaim : payload.getSubject();
  }
}
