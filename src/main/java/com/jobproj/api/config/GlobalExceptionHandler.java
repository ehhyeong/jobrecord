package com.jobproj.api.config;

import com.jobproj.api.common.ApiResponse;
import com.jobproj.api.common.OwnerMismatchException;
import jakarta.servlet.ServletException;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;

@Slf4j
// springdoc 내부 컨트롤러(/v3/api-docs 등)에는 적용되지 않도록 우리 패키지로만 제한
@RestControllerAdvice(basePackages = "com.jobproj.api")
public class GlobalExceptionHandler {
    /** (3) Bean Validation 실패 → 400 + fieldErrors[] */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, List<Map<String, String>>>>>
    handleValidationExceptions(MethodArgumentNotValidException e) {

        List<Map<String, String>> fieldErrors =
                e.getBindingResult().getFieldErrors().stream()
                        .map(fe -> Map.of("field", fe.getField(), "message", fe.getDefaultMessage()))
                        .collect(Collectors.toList());

        log.warn("[400] BeanValidation failed: {} errors", fieldErrors.size());

        ApiResponse<Map<String, List<Map<String, String>>>> body =
                ApiResponse.fail(
                        "VALIDATION_FAILED", "입력값 검증에 실패했습니다.", Map.of("fieldErrors", fieldErrors));
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    /** (1) 인증 실패 → 401 A001 */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthenticationException(AuthenticationException e) {
        log.warn("[401] Authentication failed: {}", e.getMessage());
        return new ResponseEntity<>(
                ApiResponse.fail("A001", "인증에 실패했습니다. (토큰이 없거나 유효하지 않습니다)"),
                HttpStatus.UNAUTHORIZED
        );
    }

    /** 접근 거부(인가 실패) → 403 A002 */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException e) {
        log.warn("[403] AccessDenied: {}", e.getMessage());
        return new ResponseEntity<>(
                ApiResponse.fail("A002", "권한이 없습니다."),
                HttpStatus.FORBIDDEN
        );
    }

    /** 업로드 용량 초과 → 413 (multipart 10MB 제한) */
    @ExceptionHandler({
            MaxUploadSizeExceededException.class, // Spring multipart 한도 초과
            MultipartException.class,             // 일부 환경에서 래핑 가능
            ServletException.class                // SizeLimitExceededException 이 여기로 래핑될 수 있음
    })
    public ResponseEntity<ApiResponse<Void>> handleUploadSizeExceeded(Exception e) {
        log.warn("[413] Upload size exceeded: {}", e.getMessage());
        return new ResponseEntity<>(
                ApiResponse.fail("UPLOAD_TOO_LARGE", "file too large (max 10MB)"),
                HttpStatus.PAYLOAD_TOO_LARGE
        );
    }

    /** 공통 잘못된 인자 → 400 */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException e) {
        log.warn("[400] IllegalArgument: {}", e.getMessage());
        return new ResponseEntity<>(
                ApiResponse.fail("INVALID_ARGUMENT", e.getMessage()),
                HttpStatus.BAD_REQUEST
        );
    }

    /** (2) 소유권 불일치 → 403 R004 */
    @ExceptionHandler(OwnerMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleOwnerMismatch(OwnerMismatchException e) {
        log.warn("[403] OwnerMismatch: {}", e.getMessage());
        return new ResponseEntity<>(
                ApiResponse.fail("R004", "owner_mismatch"),
                HttpStatus.FORBIDDEN
        );
    }

    /** (수정) 리소스 미존재 → 404 */
    @ExceptionHandler({
            NoSuchElementException.class,        // Optional.get() 등
            EmptyResultDataAccessException.class // deleteById(미존재) 등
    })
    public ResponseEntity<ApiResponse<Void>> handleNotFound(RuntimeException e) {
        log.warn("[404] NotFound: {}", e.getMessage());
        return new ResponseEntity<>(
                ApiResponse.fail("NOT_FOUND", "resource not found"),
                HttpStatus.NOT_FOUND
        );
    }

    /** 기타 런타임 → 400 */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Void>> handleRuntime(RuntimeException e) {
        log.warn("[400] RuntimeException", e);
        return new ResponseEntity<>(
                ApiResponse.fail("BAD_REQUEST", e.getMessage()),
                HttpStatus.BAD_REQUEST
        );
    }

    /** 최종 안전망 → 500 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleAny(Exception e) {
        log.error("[500] Unhandled Exception", e);
        return new ResponseEntity<>(
                ApiResponse.fail("SERVER_ERROR", "unexpected_error"),
                HttpStatus.INTERNAL_SERVER_ERROR
        );
    }
}
