package com.jobproj.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter // @Valid를 위해 Setter 또는 생성자가 필요합니다.
public class SignupRequest {

  @NotBlank(message = "이메일은 필수입니다.")
  @Email(message = "이메일 형식이 아닙니다.")
  private String email;

  @NotBlank(message = "비밀번호는 필수입니다.")
  @Size(min = 8, message = "비밀번호는 8자 이상이어야 합니다.")
  private String password;

  @NotBlank(message = "이름은 필수입니다.")
  private String name;
}
