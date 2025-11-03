package com.jobproj.api.ctrl;

import com.jobproj.api.dto.LoginRequest;
import com.jobproj.api.dto.LoginResponse;
import com.jobproj.api.dto.SignupRequest;
import com.jobproj.api.security.JwtTokenProvider;
import com.jobproj.api.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.Duration;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Tag(name = "인증", description = "로그인/회원가입 API")
public class AuthCtrl {

  private final UserService userService;
  private final JwtTokenProvider jwtTokenProvider;

  // ---------------------- 로그인 ----------------------
  @Operation(summary = "로그인", description = "이메일/패스워드로 로그인 후 JWT 토큰을 발급합니다.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "성공",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = LoginResponse.class),
                    examples =
                        @ExampleObject(
                            name = "성공예시",
                            value =
                                """
                                {
                                  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6...",
                                  "tokenType": "Bearer",
                                  "expiresIn": 3600000
                                }
                                """))),
        @ApiResponse(
            responseCode = "400",
            description = "요청 값 오류",
            content =
                @Content(
                    mediaType = "application/json",
                    examples =
                        @ExampleObject(
                            value =
                                """
                                { "code": "C001", "message": "요청 값이 올바르지 않습니다." }
                                """))),
        @ApiResponse(
            responseCode = "401",
            description = "인증 실패",
            content =
                @Content(
                    mediaType = "application/json",
                    examples =
                        @ExampleObject(
                            value =
                                """
                                { "code": "A001", "message": "invalid credentials" }
                                """)))
      })
  @PostMapping("/auth/login")
  public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req) {
    LoginResponse res = userService.login(req.getEmail(), req.getPassword());

    // 로그인 시 Refresh 토큰 발급 + HttpOnly 쿠키 저장 (신규)
    var userRow = userService.loadUserRowByEmail(req.getEmail()); // 구현 없으면 유틸 추가 필요
    String refresh = jwtTokenProvider.createRefreshToken(userRow);
    ResponseCookie rtCookie = ResponseCookie.from("RT", refresh)
        .httpOnly(true)
        .secure(false)           // 배포/HTTPS 환경에서는 true 권장
        .sameSite("Lax")
        .path("/")
        .maxAge(Duration.ofDays(14))
        .build();

    return ResponseEntity.ok()
        .header(HttpHeaders.SET_COOKIE, rtCookie.toString())
        .body(Map.of(
            "message", "로그인 성공",
            "token", res.getAccessToken(),
            "tokenType", res.getTokenType(),
            "expiresIn", res.getExpiresIn()));
  }

  // ---------------------- 액세스 토큰 재발급 ----------------------
  // 리프레시로 액세스 재발급 엔드포인트 추가
  @Operation(summary = "액세스 토큰 재발급", description = "쿠키 RT(리프레시) 또는 Authorization 헤더를 사용해 액세스 토큰을 재발급합니다.")
  @PostMapping("/auth/refresh")
  public ResponseEntity<?> refresh(HttpServletRequest request) {
    String rt = jwtTokenProvider.resolveRefreshToken(request);
    if (rt == null || !jwtTokenProvider.validateRefreshToken(rt)) {
      return ResponseEntity.status(401).body(Map.of(
          "errorCode", "A003",
          "message", "유효하지 않은 리프레시 토큰입니다.",
          "status", 401));
    }
    String email = jwtTokenProvider.getEmail(rt);
    var userRow = userService.loadUserRowByEmail(email); // 구현 없으면 유틸 추가 필요
    String access = jwtTokenProvider.createAccessToken(userRow);
    long expiresIn = userService.getAccessTokenTtlMs();  // 구현 없으면 유틸 추가 필요
    return ResponseEntity.ok(Map.of(
        "message", "재발급 성공",
        "token", access,
        "tokenType", "Bearer",
        "expiresIn", expiresIn));
  }

  // ---------------------- 로그아웃 ----------------------
  // RT 쿠키 제거로 로그아웃 처리
  @Operation(summary = "로그아웃", description = "리프레시 토큰 쿠키(RT)를 제거합니다.")
  @PostMapping("/auth/logout")
  public ResponseEntity<?> logout() {
    ResponseCookie clear = ResponseCookie.from("RT", "")
        .httpOnly(true)
        .secure(false) // 배포/HTTPS 환경에서는 true 권장
        .sameSite("Lax")
        .path("/")
        .maxAge(0)
        .build();
    return ResponseEntity.ok()
        .header(HttpHeaders.SET_COOKIE, clear.toString())
        .body(Map.of("message", "로그아웃"));
  }

  // ---------------------- 회원가입 ----------------------
  @Operation(summary = "회원가입", description = "이메일/비밀번호/이름으로 회원가입을 수행합니다.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "201",
            description = "회원가입 성공",
            content =
                @Content(
                    mediaType = "application/json",
                    examples =
                        @ExampleObject(
                            value =
                                """
                                {
                                  "message": "회원가입 성공",
                                  "email": "testuser@example.com"
                                }
                                """))),
        @ApiResponse(
            responseCode = "400",
            description = "요청 값 오류",
            content =
                @Content(
                    mediaType = "application/json",
                    examples =
                        @ExampleObject(
                            value =
                                """
                                { "code": "C001", "message": "요청 값이 올바르지 않습니다." }
                                """))),
        @ApiResponse(
            responseCode = "409",
            description = "이미 존재",
            content =
                @Content(
                    mediaType = "application/json",
                    examples =
                        @ExampleObject(
                            value =
                                """
                                { "code": "U001", "message": "이미 가입된 이메일입니다." }
                                """)))
      })
  @PostMapping("/auth/signup")
  public ResponseEntity<?> signup(@Valid @RequestBody SignupRequest req) {
    userService.signup(req.getEmail(), req.getPassword(), req.getName());
    return ResponseEntity.status(201).body(Map.of("message", "회원가입 성공", "email", req.getEmail()));
  }
}
