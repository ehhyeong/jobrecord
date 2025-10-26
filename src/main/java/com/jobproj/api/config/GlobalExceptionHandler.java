package com.jobproj.api.config;

import java.time.Instant;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
// ✅ springdoc 내부 컨트롤러(/v3/api-docs 등)에는 적용되지 않도록 우리 패키지로만 제한
@RestControllerAdvice(basePackages = "com.jobproj.api")
public class GlobalExceptionHandler {

  private Map<String, Object> body(String code, String msg, int status) {
    return Map.of(
        "errorCode", code,
        "message", msg,
        "status", status,
        "timestamp", Instant.now().toString());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Map<String, Object>> handleBadRequest(MethodArgumentNotValidException e) {
    String msg = "invalid_request";
    FieldError fe = e.getBindingResult().getFieldError();
    if (fe != null) msg = fe.getField() + ": " + fe.getDefaultMessage();
    log.warn("[400] BeanValidation failed: {}", msg);
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body("INVALID_REQUEST", msg, 400));
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException e) {
    log.warn("[400] IllegalArgument: {}", e.getMessage());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(body("INVALID_ARGUMENT", e.getMessage(), 400));
  }

  @ExceptionHandler(RuntimeException.class)
  public ResponseEntity<Map<String, Object>> handleRuntime(RuntimeException e) {
    log.warn("[400] RuntimeException", e);
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(body("BAD_REQUEST", e.getMessage(), 400));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<Map<String, Object>> handleAny(Exception e) {
    log.error("[500] Unhandled Exception", e);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(body("SERVER_ERROR", "unexpected_error", 500));
  }
}
