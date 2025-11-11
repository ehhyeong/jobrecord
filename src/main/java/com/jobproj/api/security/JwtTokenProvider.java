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

  // 쿠키 이름 상수
  private static final String ACCESS_TOKEN_COOKIE = "access_token";
  private static final String REFRESH_TOKEN_COOKIE = "refresh_token";
  // (호환) 과거 RT 명칭도 허용
  private static final String LEGACY_RT_COOKIE = "RT";

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

  // ===== 추출 (쿠키 전용) =====

  /** access_token 쿠키만 사용(헤더 경로 제거) */
  public String resolveAccessToken(HttpServletRequest request) {
    Cookie c = getCookie(request, ACCESS_TOKEN_COOKIE);
    return (c != null && StringUtils.hasText(c.getValue())) ? c.getValue() : null;
  }

  /** (호환) 기존 resolveToken은 쿠키 전용 메서드로 위임 */
  public String resolveToken(HttpServletRequest request) {
    return resolveAccessToken(request);
  }

  /** refresh_token 쿠키만 사용(헤더 경로 제거). 과거 RT 명칭도 허용 */
  public String resolveRefreshToken(HttpServletRequest request) {
    Cookie c = getCookie(request, REFRESH_TOKEN_COOKIE);
    if (c != null && StringUtils.hasText(c.getValue())) {
      return c.getValue();
    }
    Cookie legacy = getCookie(request, LEGACY_RT_COOKIE);
    return (legacy != null && StringUtils.hasText(legacy.getValue())) ? legacy.getValue() : null;
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

  // ===== util =====
  private Cookie getCookie(HttpServletRequest request, String name) {
    if (request == null || request.getCookies() == null) return null;
    for (Cookie c : request.getCookies()) {
      if (name.equals(c.getName())) return c;
    }
    return null;
  }
}
