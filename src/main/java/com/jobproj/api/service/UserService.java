package com.jobproj.api.service;

import com.jobproj.api.domain.Role;
import com.jobproj.api.dto.LoginResponse;
import com.jobproj.api.repo.UserRepo;
import com.jobproj.api.repo.UserRepo.UserRow;
import com.jobproj.api.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

  private final UserRepo userRepo;
  private final PasswordEncoder passwordEncoder;
  private final JwtTokenProvider jwtTokenProvider;

  // Access 토큰 TTL을 yml에서 주입
  @Value("${jwt.expiration-ms:3600000}")
  private long accessTokenTtlMs; // ms 단위

  /** 로그인 */
  public LoginResponse login(String email, String password) {
    // 1) 사용자 조회
    UserRow user =
        userRepo.findByEmail(email).orElseThrow(() -> new RuntimeException("가입되지 않은 이메일입니다."));

    // 2) 비밀번호 검증
    if (!passwordEncoder.matches(password, user.pwdHash)) {
      throw new RuntimeException("비밀번호가 일치하지 않습니다.");
    }

    // 3) 토큰 생성
    // Access 토큰 발급 메서드로 명시
    String token = jwtTokenProvider.createAccessToken(user);

    // 4) 응답 DTO 반환 (accessToken, tokenType=Bearer, expiresIn)
    // TTL 하드코드 대신 주입값 사용
    long expiresIn = accessTokenTtlMs;
    return LoginResponse.of(token, expiresIn);
  }

  /** 회원가입 */
  public void signup(String email, String rawPassword, String name) {
    // 1) 이메일 중복 체크
    if (userRepo.existsByEmail(email)) {
      throw new RuntimeException("이미 사용 중인 이메일입니다.");
    }

    // 2) 비밀번호 암호화
    String encodedPassword = passwordEncoder.encode(rawPassword);

    // 3) 기본 역할
    Role role = Role.USER;

    // 4) 저장
    userRepo.save(email, encodedPassword, name, role);
  }

  // 유저/TTL 유틸
  public UserRepo.UserRow loadUserRowByEmail(String email) {
    return userRepo.findByEmail(email)
        .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
  }

  // Access TTL 조회
  public long getAccessTokenTtlMs() {
    return accessTokenTtlMs;
  }
}
