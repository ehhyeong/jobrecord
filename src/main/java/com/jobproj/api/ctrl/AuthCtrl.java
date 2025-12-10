package com.jobproj.api.ctrl;

import com.jobproj.api.dto.LoginRequest;
import com.jobproj.api.dto.LoginResponse;
import com.jobproj.api.dto.SignupRequest;
import com.jobproj.api.security.JwtTokenProvider;
import com.jobproj.api.dto.PasswordResetRequest;
import com.jobproj.api.dto.PasswordResetConfirm;
import com.jobproj.api.service.UserService;
import com.jobproj.api.dto.PasswordResetRequest;
import com.jobproj.api.dto.PasswordResetConfirm;
import com.jobproj.api.dto.PasswordResetVerifyRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import java.time.Duration;
import java.util.Map;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Tag(name = "인증", description = "로그인/회원가입 API")
public class AuthCtrl {

    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;

    // ---------------------- 로그인 ----------------------
    @Operation(summary = "로그인", description = "이메일/패스워드로 로그인 후 JWT 발급(쿠키).", security = {})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "성공",
                    headers = {
                            @Header(name = "Set-Cookie", description = "access_token=...; HttpOnly; ..."),
                            @Header(name = "Set-Cookie", description = "refresh_token=...; HttpOnly; ...")
                    }),
            @ApiResponse(responseCode = "400", description = "요청 값 오류"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @PostMapping(value = "/auth/login", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req) {
        LoginResponse res = userService.login(req.getEmail(), req.getPassword());

        // access_token/refresh_token 모두 HttpOnly 쿠키로 발급
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
                // 하위호환 위해 바디에도 기존 응답을 유지
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
            security = { @SecurityRequirement(name = "cookieAuth") }
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OK",
                    headers = @Header(name = "Set-Cookie", description = "access_token=...; HttpOnly; ...")),
            @ApiResponse(responseCode = "401", description = "리프레시 토큰 불일치/만료")
    })
    @PostMapping("/auth/refresh")
    public ResponseEntity<?> refresh(HttpServletRequest request) {
        // 쿠키에서 refresh_token 추출
        String rt = jwtTokenProvider.resolveRefreshToken(request);
        if (rt == null || !jwtTokenProvider.validateRefreshToken(rt)) {
            return ResponseEntity.status(401).body(
                    Map.of("errorCode", "A003", "message", "유효하지 않은 리프레시 토큰입니다.", "status", 401));
        }

        String email = jwtTokenProvider.getEmail(rt);
        var userRow = userService.loadUserRowByEmail(email);

        // access_token 재발급 후 쿠키로 세팅
        String newAccess = jwtTokenProvider.createAccessToken(userRow);
        ResponseCookie atCookie = ResponseCookie.from("access_token", newAccess)
                .httpOnly(true).secure(false).sameSite("Lax").path("/")
                .maxAge(Duration.ofMillis(userService.getAccessTokenTtlMs()))
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, atCookie.toString())
                // 하위호환: 바디에도 액세스 토큰 포함 유지
                .body(Map.of(
                        "message", "재발급 성공",
                        "token", newAccess,
                        "tokenType", "Bearer",
                        "expiresIn", userService.getAccessTokenTtlMs()
                ));
    }

    // ---------------------- 로그아웃 ----------------------
    @Operation(summary = "로그아웃", description = "access_token/refresh_token 쿠키 만료.",
            security = { @SecurityRequirement(name = "cookieAuth") })
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "쿠키 만료 처리") })
    @PostMapping("/auth/logout")
    public ResponseEntity<?> logout() {
        // 두 쿠키 모두 만료
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
    @PostMapping(value = "/auth/signup", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> signup(@Valid @RequestBody SignupRequest req) {
        // 🔽 phone까지 같이 전달
        userService.signup(req.getEmail(), req.getPassword(), req.getName(), req.getPhone());
        return ResponseEntity.status(201)
                .body(Map.of("message", "회원가입 성공", "email", req.getEmail()));
    }

    // 회원가입 전 이메일 사용 가능 여부 확인 API
    @Operation(
            summary = "이메일 중복 체크",
            description = "회원가입 시 입력한 이메일이 이미 사용 중인지 확인합니다.",
            security = {} // 비로그인 상태에서 호출
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "OK",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = Map.class),
                            examples = {
                                    @ExampleObject(
                                            name = "이미 존재하는 이메일",
                                            value = "{\"exists\": true}"
                                    ),
                                    @ExampleObject(
                                            name = "사용 가능한 이메일",
                                            value = "{\"exists\": false}"
                                    )
                            }
                    )
            )
    })
    @GetMapping(value = "/auth/check-email", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Boolean>> checkEmail(@RequestParam String email) {
        boolean exists = userService.isEmailDuplicate(email);
        return ResponseEntity.ok(Map.of("exists", exists));
    }
        // 🔽 전화번호 중복 체크 API
        @Operation(
                summary = "전화번호 중복 체크",
                description = "회원가입 시 입력한 전화번호가 이미 사용 중인지 확인합니다.",
                security = {} // 비로그인 상태에서 호출
        )
        @ApiResponses(value = {
                @ApiResponse(
                        responseCode = "200",
                        description = "OK",
                        content = @Content(
                                mediaType = "application/json",
                                schema = @Schema(implementation = Map.class),
                                examples = {
                                        @ExampleObject(
                                                name = "이미 존재하는 전화번호",
                                                value = "{\"exists\": true}"
                                        ),
                                        @ExampleObject(
                                                name = "사용 가능한 전화번호",
                                                value = "{\"exists\": false}"
                                        )
                                }
                        )
                )
        })
        @GetMapping(value = "/auth/check-phone", produces = MediaType.APPLICATION_JSON_VALUE)
        public ResponseEntity<Map<String, Boolean>> checkPhone(@RequestParam String phone) {
        boolean exists = userService.isPhoneDuplicate(phone);
        return ResponseEntity.ok(Map.of("exists", exists));
        }    

    // ---------------------- 비밀번호 재설정 ----------------------

    /**
     * 1. 비밀번호 재설정 코드 발송
     * (UserService의 sendPasswordResetCode 호출)
     */
    @Operation(summary = "비밀번호 재설정 코드 발송", description = "이메일로 6자리 인증번호를 발송합니다.", security = {})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "발송 성공 (실패 시에도 200)"),
            @ApiResponse(responseCode = "400", description = "이메일 형식 오류")
    })
    @PostMapping(value = "/auth/password-reset/request", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> requestPasswordReset(@Valid @RequestBody PasswordResetRequest req) {
        userService.sendPasswordResetCode(req.getEmail());
        // (보안) 이메일이 존재하지 않아도, 공격자에게 힌트를 주지 않기 위해 항상 200 OK를 반환합니다.
        // 비밀번호 재설정 UI 개선 : 남은 시간 문구는 프론트 타이머로 대체
        return ResponseEntity.ok(Map.of("message", "인증번호가 발송되었습니다."));
    }

    /**
     * 비밀번호 재설정 : 인증번호만 검증하는 API
     * 2-1. 인증번호 검증 (비밀번호 변경 전 단계)
     */
    @Operation(
            summary = "비밀번호 재설정 인증번호 검증",
            description = "이메일과 인증번호가 일치하는지 확인만 수행합니다.",
            security = {} // 비로그인 상태 호출
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "검증 성공"),
            @ApiResponse(responseCode = "400", description = "검증 실패 (만료/불일치)")
    })
    @PostMapping(value = "/auth/password-reset/verify", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> verifyPasswordReset(
            @Valid @RequestBody PasswordResetVerifyRequest req) {
        userService.verifyPasswordResetCode(req.getEmail(), req.getCode()); // (수정: DTO 타입만 변경)
        return ResponseEntity.ok(Map.of("message", "인증번호가 확인되었습니다."));
    }

    /**
     * 2-2. 비밀번호 재설정 및 완료
     * (UserService의 resetPassword 호출)
     */
    @Operation(summary = "비밀번호 재설정", description = "이메일, 인증번호, 새 비밀번호로 재설정을 완료합니다.", security = {})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "재설정 성공"),
            @ApiResponse(responseCode = "400", description = "요청 값 오류 (인증번호 불일치 등)")
    })
    @PostMapping(value = "/auth/password-reset/confirm", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> confirmPasswordReset(@Valid @RequestBody PasswordResetConfirm req) {
        userService.resetPassword(req.getEmail(), req.getCode(), req.getNewPassword());
        return ResponseEntity.ok(Map.of("message", "비밀번호가 재설정되었습니다."));
    }
}
