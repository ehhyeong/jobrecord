// 8주차 추가: 사용자 정보 응답 DTO (파일전체)
package com.jobproj.api.dto;

import com.jobproj.api.domain.Role;
import com.jobproj.api.repo.UserRepo.UserRow;
import lombok.Getter;

@Getter
public class UserDto {

  // "내 정보" 응답용 DTO
  @Getter
  public static class Response {
    private final Long id;
    private final String email;
    private final String name;
    private final String phone; // 🔽 전화번호 추가
    private final Role role;
    private final String createdAt; // 가입일
    private final String updatedAt; // 최근 수정일

    // UserRow(DB) 객체를 Response(DTO) 객체로 변환하는 생성자
    public Response(UserRow user) {
      this.id = user.id;
      this.email = user.email;
      this.name = user.name;
      this.phone = user.phone; // 🔽 매핑
      this.role = user.role;
      this.createdAt = user.createdAt != null ? user.createdAt.toString() : null;
      this.updatedAt = user.updatedAt != null ? user.updatedAt.toString() : null;
    }
  }
}
