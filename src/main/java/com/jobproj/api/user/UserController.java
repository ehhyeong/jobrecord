package com.jobproj.api.user;

import com.jobproj.api.common.ApiResponse;
import com.jobproj.api.dto.UserDto;
import com.jobproj.api.repo.UserRepo;
import com.jobproj.api.security.CurrentUser;
import com.jobproj.api.service.UserService; // 12주차 추가: 서비스 임포트
import java.util.Map; // 12주차 추가
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

    private final UserRepo userRepo;
    private final CurrentUser currentUser;
    private final UserService userService; // 12주차 추가: 비즈니스 로직(탈퇴) 처리를 위해 필요

    // 1. 내 정보 조회
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserDto.Response>> getMyInfo() {

        Long usersId = currentUser.id();

        return userRepo
                .findById(usersId)
                .map(
                        userRow -> {
                            UserDto.Response dto = new UserDto.Response(userRow);
                            return ResponseEntity.ok(ApiResponse.ok(dto));
                        })
                .orElseGet(
                        () ->
                                ResponseEntity.status(404)
                                        .body(ApiResponse.fail("USER_NOT_FOUND", "사용자 정보를 찾을 수 없습니다.")));
    }

    // ==========================================
    // 2233076 12주차 추가: 회원 탈퇴 API
    // ==========================================
    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<String>> withdraw(@RequestBody Map<String, String> req) {
        // 1. 토큰에서 현재 접속한 사용자의 ID(PK)를 가져옴
        Long usersId = currentUser.id();

        // 2. ID를 이용해 이메일을 찾음 (UserService가 이메일 기반으로 동작하므로)
        UserRepo.UserRow user = userRepo.findById(usersId)
                .orElseThrow(() -> new IllegalArgumentException("사용자 정보를 찾을 수 없습니다."));

        // 3. 요청 바디에서 비밀번호 추출
        String password = req.get("password");

        // 4. 서비스 호출 (비밀번호 검증 및 삭제)
        userService.withdraw(user.email, password);

        // 5. 성공 응답
        return ResponseEntity.ok(ApiResponse.ok("회원 탈퇴가 완료되었습니다."));
    }
}