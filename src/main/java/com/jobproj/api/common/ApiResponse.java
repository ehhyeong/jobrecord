package com.jobproj.api.common;

// 8주차 수정: @JsonInclude 추가 (실패 시 data:null 숨기기)
import com.fasterxml.jackson.annotation.JsonInclude;

// 8주차 수정: data가 null일 때는 json에서 제외
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
  private final boolean success;
  private final String code;
  private final String message;
  private final T data;

  // 8주차 수정: 생성자를 private -> public으로 변경 (GlobalExceptionHandler에서 접근해야 함)
  // (이 생성자를 직접 호출해도 되지만, 아래 static 메서드를 쓰는 것이 더 깔끔합니다)
  public ApiResponse(boolean success, String code, String message, T data) {
    this.success = success;
    this.code = code;
    this.message = message;
    this.data = data;
  }

  // --- 성공 응답 ---
  public static <T> ApiResponse<T> ok(T data) {
    return new ApiResponse<>(true, "OK", null, data);
  }

  public static <T> ApiResponse<T> ok(String message, T data) {
    return new ApiResponse<>(true, "OK", message, data);
  }

  // --- 실패 응답 (기존) ---
  public static <T> ApiResponse<T> fail(String message) {
    return new ApiResponse<>(false, "ERROR", message, null);
  }

  public static <T> ApiResponse<T> fail(String code, String message) {
    return new ApiResponse<>(false, code, message, null);
  }

  // 8주차 추가: fieldErrors(data)를 포함하는 실패 응답
  public static <T> ApiResponse<T> fail(String code, String message, T data) {
    return new ApiResponse<>(false, code, message, data);
  }

  // --- Getter ---
  public boolean isSuccess() {
    return success;
  }

  public String getCode() {
    return code;
  }

  public String getMessage() {
    return message;
  }

  public T getData() {
    return data;
  }
}
