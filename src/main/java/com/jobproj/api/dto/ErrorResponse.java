package com.jobproj.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Schema(description = "공통 오류 응답")
public class ErrorResponse {
  @Schema(description = "오류 코드", example = "C001")
  private String code;

  @Schema(description = "메시지", example = "요청 값이 올바르지 않습니다.")
  private String message;
}
