// 융합프로젝트 김태형 11주차 비밀번호 찾기 : 인증번호 확인 요청 DTO (추가)
package com.jobproj.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PasswordResetVerify {

    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String code;
}
