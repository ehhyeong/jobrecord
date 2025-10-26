package com.jobproj.api.common;

public class ApiResponse<T> {
  private final boolean success;
  private final String code;
  private final String message;
  private final T data;

  private ApiResponse(boolean success, String code, String message, T data) {
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

  // --- 실패 응답 (code + message 모두 가능하게 확장) ---
  public static <T> ApiResponse<T> fail(String message) {
    return new ApiResponse<>(false, "ERROR", message, null);
  }

  public static <T> ApiResponse<T> fail(String code, String message) {
    return new ApiResponse<>(false, code, message, null);
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
