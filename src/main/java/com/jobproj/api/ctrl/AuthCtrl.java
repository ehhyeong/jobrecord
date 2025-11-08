package com.jobproj.api.ctrl;

import com.jobproj.api.dto.LoginRequest;
import com.jobproj.api.dto.LoginResponse;
import com.jobproj.api.dto.SignupRequest;
import com.jobproj.api.security.JwtTokenProvider;
import com.jobproj.api.service.UserService;

import io.swagger.v3.oas.annotations.Operation;                      // (추가)
import io.swagger.v3.oas.annotations.headers.Header;                 // (추가)
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;          // (추가)
import io.swagger.v3.oas.annotations.responses.ApiResponses;         // (추가)
import io.swagger.v3.oas.annotations.security.SecurityRequirement;   // (추가)
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.Duration;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;                               // (추가)
import org.springframework.http.ResponseCookie;                       // (수정)
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Tag(name = "인증", description = "로그인/회원가입 API")
public class AuthCtrl {

  private final UserService userService;
  private final JwtTokenProvider jwtTokenProvider;

  // 융합프로젝트 김태형 9주차 OpenAPI 스펙 확정(핵심 도메인 + 오류 예시)
  // - cookieAuth 보안 시나리오에 맞춰 access_token/refresh_token 쿠키 발급/회수 로직 반영 (수정)

  // ---------------------- 로그인 ----------------------
  @Operation(summary = "로그인", description = "이메일/패스워드로 로그인 후 JWT 발급(쿠키).", security = {}) // (추가)
  @ApiResponses(value = { // (추가)
      @ApiResponse(responseCode = "200", description = "성공",
          headers = {
              @Header(name = "Set-Cookie", description = "access_token=...; HttpOnly; ..."),
              @Header(name = "Set-Cookie", description = "refresh_token=...; HttpOnly; ...")
          }),
      @ApiResponse(responseCode = "400", description = "요청 값 오류"),
      @ApiResponse(responseCode = "401", description = "인증 실패")
  })
  @PostMapping(value = "/auth/login", consumes = MediaType.APPLICATION_JSON_VALUE) // (수정)
  public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req) {
    LoginResponse res = userService.login(req.getEmail(), req.getPassword());

    // (수정) access_token/refresh_token 모두 HttpOnly 쿠키로 발급
    var userRow = userService.loadUserRowByEmail(req.getEmail());
    String access = jwtTokenProvider.createAccessToken(userRow);
    String refresh = jwtTokenProvider.createRefreshToken(userRow);

    // 개발환경 예시: Secure=false, SameSite=Lax (배포 시 Secure=true, SameSite=None 권장)
    ResponseCookie atCookie = ResponseCookie.from("access_token", access)
        .httpOnly(true).secure(false).sameSite("Lax").path("/")
        .maxAge(Duration.ofMillis(userService.getAccessTokenTtlMs()))
        .build();
    ResponseCookie rtCookie = ResponseCookie.from("refresh_token", refresh)
        .httpOnly(true).secure(false).sameSite("Lax").path("/auth")
        .maxAge(Duration.ofDays(14))
        .build();

    return ResponseEntity.ok()
        .header(HttpHeaders.SET_COOKIE, atCookie.toString())
        .header(HttpHeaders.SET_COOKIE, rtCookie.toString())
        // (수정) 하위호환 위해 바디에도 기존 응답을 유지
        .body(Map.of(
            "message", "로그인 성공",
            "token", res.getAccessToken(),
            "tokenType", res.getTokenType(),
            "expiresIn", res.getExpiresIn()
        ));
  }

  // ---------------------- 액세스 토큰 재발급 ----------------------
  @Operation(
      summary = "액세스 토큰 재발급",
      description = "refresh_token(쿠키) 검증 후 access_token(쿠키) 재발급.",
      security = { @SecurityRequirement(name = "cookieAuth") } // (추가)
  )
  @ApiResponses(value = { // (추가)
      @ApiResponse(responseCode = "200", description = "OK",
          headers = @Header(name = "Set-Cookie", description = "access_token=...; HttpOnly; ...")),
      @ApiResponse(responseCode = "401", description = "리프레시 토큰 불일치/만료")
  })
  @PostMapping("/auth/refresh")
  public ResponseEntity<?> refresh(HttpServletRequest request) {
    // (수정) 쿠키에서 refresh_token 추출
    String rt = jwtTokenProvider.resolveRefreshToken(request);
    if (rt == null || !jwtTokenProvider.validateRefreshToken(rt)) {
      return ResponseEntity.status(401).body(
          Map.of("errorCode", "A003", "message", "유효하지 않은 리프레시 토큰입니다.", "status", 401));
    }

    String email = jwtTokenProvider.getEmail(rt);
    var userRow = userService.loadUserRowByEmail(email);

    // (수정) access_token 재발급 후 쿠키로 세팅
    String newAccess = jwtTokenProvider.createAccessToken(userRow);
    ResponseCookie atCookie = ResponseCookie.from("access_token", newAccess)
        .httpOnly(true).secure(false).sameSite("Lax").path("/")
        .maxAge(Duration.ofMillis(userService.getAccessTokenTtlMs()))
        .build();

    return ResponseEntity.ok()
        .header(HttpHeaders.SET_COOKIE, atCookie.toString())
        // (수정) 하위호환: 바디에도 액세스 토큰 포함 유지
        .body(Map.of(
            "message", "재발급 성공",
            "token", newAccess,
            "tokenType", "Bearer",
            "expiresIn", userService.getAccessTokenTtlMs()
        ));
  }

  // ---------------------- 로그아웃 ----------------------
  @Operation(summary = "로그아웃", description = "access_token/refresh_token 쿠키 만료.", 
            security = { @SecurityRequirement(name = "cookieAuth") }) // (추가)
  @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "쿠키 만료 처리") }) // (추가)
  @PostMapping("/auth/logout")
  public ResponseEntity<?> logout() {
    // (수정) 두 쿠키 모두 만료
    ResponseCookie atClear = ResponseCookie.from("access_token", "")
        .httpOnly(true).secure(false).sameSite("Lax").path("/").maxAge(0).build();
    ResponseCookie rtClear = ResponseCookie.from("refresh_token", "")
        .httpOnly(true).secure(false).sameSite("Lax").path("/auth").maxAge(0).build();

    return ResponseEntity.ok()
        .header(HttpHeaders.SET_COOKIE, atClear.toString())
        .header(HttpHeaders.SET_COOKIE, rtClear.toString())
        .body(Map.of("message", "로그아웃"));
  }

  // ---------------------- 회원가입 ----------------------
  @Operation(summary = "회원가입", description = "이메일/비밀번호/이름으로 회원가입을 수행합니다.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "201", description = "회원가입 성공"),
      @ApiResponse(responseCode = "400", description = "요청 값 오류"),
      @ApiResponse(responseCode = "409", description = "이미 존재")
  })
  @PostMapping(value = "/auth/signup", consumes = MediaType.APPLICATION_JSON_VALUE) // (수정)
  public ResponseEntity<?> signup(@Valid @RequestBody SignupRequest req) {
    userService.signup(req.getEmail(), req.getPassword(), req.getName());
    return ResponseEntity.status(201)
        .body(Map.of("message", "회원가입 성공", "email", req.getEmail()));
  }
}
