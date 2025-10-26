package com.jobproj.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

  /** 액세스 토큰 */
  private String accessToken;

  /** 토큰 타입 (기본값: Bearer) */
  private String tokenType;

  /** 토큰 만료 시간 (ms 단위) */
  private long expiresIn;

  /** JWT 응답 편의용 정적 팩토리 */
  public static LoginResponse of(String token, long expiresIn) {
    return LoginResponse.builder()
        .accessToken(token)
        .tokenType("Bearer")
        .expiresIn(expiresIn)
        .build();
  }
}
