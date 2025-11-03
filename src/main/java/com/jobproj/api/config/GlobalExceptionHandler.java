package com.jobproj.api.config;

import com.jobproj.api.common.OwnerMismatchException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import jakarta.servlet.ServletException;

@Slf4j
// springdoc 내부 컨트롤러(/v3/api-docs 등)에는 적용되지 않도록 우리 패키지로만 제한
@RestControllerAdvice(basePackages = "com.jobproj.api")
public class GlobalExceptionHandler {

  private Map<String, Object> body(String code, String msg, int status) {
    return Map.of(
        "errorCode", code,
        "message", msg,
        "status", status,
        "timestamp", Instant.now().toString());
  }

  @ExceptionHandler({
      MaxUploadSizeExceededException.class,  // Spring multipart 한도 초과
      MultipartException.class,              // 일부 환경에서 래핑되어 던져질 수 있음
      ServletException.class                 // SizeLimitExceededException 이 여기로 래핑될 수 있음
  })
  public ResponseEntity<Map<String, Object>> handleUploadSizeExceeded(Exception e) {
    log.warn("[400] Upload size exceeded: {}", e.getMessage());
    // 필요 시 코드명을 FILE_TOO_LARGE 등으로 바꿔도 됨(현재 enum/포맷에 맞춰 INVALID_ARGUMENT 유지)
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(body("INVALID_ARGUMENT", "file too large (max 10MB)", 400));
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

  @ExceptionHandler(OwnerMismatchException.class)
  public ResponseEntity<Map<String, Object>> handleOwnerMismatch(OwnerMismatchException e) {
    log.warn("[403] OwnerMismatch: {}", e.getMessage());
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .body(body("R004", "owner_mismatch", 403));

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
