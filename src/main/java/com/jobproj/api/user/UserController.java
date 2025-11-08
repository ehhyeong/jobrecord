// 8주차 추가: GET /users/me API 컨트롤러 (파일전체)
package com.jobproj.api.user;

import com.jobproj.api.common.ApiResponse;
import com.jobproj.api.dto.UserDto;
import com.jobproj.api.repo.UserRepo;
import com.jobproj.api.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

  private final UserRepo userRepo;
  private final CurrentUser currentUser; // (ResumeController와 동일한 방식)

  @GetMapping("/me")
  public ResponseEntity<ApiResponse<UserDto.Response>> getMyInfo() {

    // 1. JwtAuthenticationFilter가 등록한 사용자 ID를 가져옵니다.
    Long usersId = currentUser.id();

    // 2. 1단계에서 UserRepo에 추가한 findById 메서드로 DB 조회
    return userRepo
        .findById(usersId)
        .map(
            userRow -> {
              // 3. 2단계에서 만든 UserDto.Response로 변환 (비밀번호 제외)
              UserDto.Response dto = new UserDto.Response(userRow);
              // 4. ApiResponse.ok()로 감싸서 반환
              return ResponseEntity.ok(ApiResponse.ok(dto));
            })
        // 5. (혹시 모를) ID는 있는데 DB에 사용자가 없는 경우
        .orElseGet(
            () ->
                ResponseEntity.status(404)
                    .body(ApiResponse.fail("USER_NOT_FOUND", "사용자 정보를 찾을 수 없습니다.")));
  }
}
