package com.jobproj.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern; // 2233076 10주차 수정
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

// 2233076 10주차 추가
@Getter
@Setter
public class PasswordResetConfirm {

    @NotBlank(message = "이메일은 필수입니다.")
    @Email(message = "유효한 이메일 형식이 아닙니다.")
    private String email;

    @NotBlank(message = "인증번호는 필수입니다.")
    @Size(min = 6, max = 6, message = "인증번호는 6자리입니다.")
    private String code;

    @NotBlank(message = "새 비밀번호는 필수입니다.")
    @Pattern(
            regexp = "^(?=.*[^A-Za-z0-9]).{8,}$",
            message = "비밀번호는 8자 이상이며, 특수문자를 1개 이상 포함해야 합니다."
    )
    private String newPassword;
}