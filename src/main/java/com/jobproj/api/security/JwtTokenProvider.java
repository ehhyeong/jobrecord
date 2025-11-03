package com.jobproj.api.security;

import com.jobproj.api.repo.UserRepo;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Base64;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import io.jsonwebtoken.SignatureAlgorithm;
import jakarta.servlet.http.Cookie;

@Slf4j
@Component
public class JwtTokenProvider {
  private final SecretKey key;
  private final long validityInMilliseconds;
  private final long refreshValidityInMilliseconds;
  // 시계 오차 허용(60초) 추가
  private static final long ALLOWED_CLOCK_SKEW_MS = 60_000L;

  public JwtTokenProvider(
      @Value("${jwt.secret-key}") String secretKey,
      @Value("${jwt.expiration-ms}") long validityInMilliseconds,
      // refresh TTL 주입
      @Value("${jwt.refresh-expiration-ms:1209600000}") long refreshValidityInMilliseconds
  ) {
    byte[] keyBytes = Base64.getDecoder().decode(secretKey);
    this.key = Keys.hmacShaKeyFor(keyBytes);
    this.validityInMilliseconds = validityInMilliseconds;
    this.refreshValidityInMilliseconds = refreshValidityInMilliseconds;
  }

  // Access 토큰 발급 분리
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

  // (호환) 기존 호출부 유지: Access 토큰 발급으로 위임
  public String createToken(UserRepo.UserRow user) {
    // 기존 메서드는 Access 토큰 발급으로 위임
    return createAccessToken(user);
  }

  // Refresh 토큰 발급
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

  public String resolveToken(HttpServletRequest request) {
    String bearerToken = request.getHeader("Authorization");
    if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
      return bearerToken.substring(7);
    }
    // 일부 클라이언트가 "Bearer " 없이 순수 토큰만 보낼 때 대비
    if (StringUtils.hasText(bearerToken)) {
      return bearerToken.trim();
    }
    return null;
  }

  // 요청에서 Refresh 토큰 추출(Cookie RT 우선, 그다음 Authorization)
  public String resolveRefreshToken(HttpServletRequest request) {
    if (request.getCookies() != null) {
      for (Cookie c : request.getCookies()) {
        if ("RT".equals(c.getName()) && StringUtils.hasText(c.getValue())) {
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

  public boolean validateToken(String token) {
    try {
      // 파서에 시계오차 허용을 직접 설정(만료/서명 검증 파서에 일임)
      Jws<Claims> claims = Jwts.parserBuilder()
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

  // Refresh 전용 검증
  public boolean validateRefreshToken(String token) {
    try {
      Jws<Claims> claims = Jwts.parserBuilder()
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
    // validate와 동일 파서(시계오차 허용)로 일관 파싱
    Claims payload = Jwts.parserBuilder()
        .setSigningKey(key)
        .setAllowedClockSkewSeconds(ALLOWED_CLOCK_SKEW_MS / 1000)
        .build()
        .parseClaimsJws(token)
        .getBody();
    String emailClaim = payload.get("email", String.class);
    return (emailClaim != null && !emailClaim.isBlank()) ? emailClaim : payload.getSubject();
  }
}
