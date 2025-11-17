package com.jobproj.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

// 2233076 10주차 추가
@Getter
@Setter // (컨트롤러에서 @RequestBody로 받기 위해 필요)
public class PasswordResetRequest {

    @NotBlank(message = "이메일은 필수입니다.")
    @Email(message = "유효한 이메일 형식이 아닙니다.")
    private String email;
}