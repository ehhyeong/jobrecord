package com.jobproj.api.ctrl;

import com.jobproj.api.dto.LoginRequest;
import com.jobproj.api.dto.LoginResponse;
import com.jobproj.api.dto.SignupRequest;
import com.jobproj.api.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Tag(name = "인증", description = "로그인/회원가입 API")
public class AuthCtrl {

  private final UserService userService;

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
    return ResponseEntity.ok(
        Map.of(
            "message", "로그인 성공",
            "token", res.getAccessToken(),
            "tokenType", res.getTokenType(),
            "expiresIn", res.getExpiresIn()));
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
