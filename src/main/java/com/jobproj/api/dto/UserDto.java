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
    private final Role role;

    // UserRow(DB) 객체를 Response(DTO) 객체로 변환하는 생성자
    public Response(UserRow user) {
      this.id = user.id;
      this.email = user.email;
      this.name = user.name;
      this.role = user.role;
    }
  }
}
