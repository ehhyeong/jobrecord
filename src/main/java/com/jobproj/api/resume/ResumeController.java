package com.jobproj.api.resume;

import com.jobproj.api.common.ApiResponse;
import com.jobproj.api.common.PageRequest;
import com.jobproj.api.common.PageResponse;
import com.jobproj.api.resume.ResumeDto.CreateRequest;
import com.jobproj.api.resume.ResumeDto.Response;
import com.jobproj.api.resume.ResumeDto.UpdateRequest;
import com.jobproj.api.security.CurrentUser;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.MediaType; // (추가)
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
// 융합프로젝트 김태형 9주차 OpenAPI 스펙 확정(핵심 도메인 + 오류 예시)
// - cookieAuth 보안 적용, 각 엔드포인트 오류 응답(401/403/404/400) 명시 (추가)
import io.swagger.v3.oas.annotations.Operation;                 // (추가)
import io.swagger.v3.oas.annotations.tags.Tag;                  // (추가)
import io.swagger.v3.oas.annotations.Parameter;                 // (추가)
import io.swagger.v3.oas.annotations.responses.ApiResponses;    // (추가)
import io.swagger.v3.oas.annotations.security.SecurityRequirement; // (추가)

@RestController
@RequestMapping(value = "/api/resumes", produces = MediaType.APPLICATION_JSON_VALUE) // (수정) produces 명시
@Tag(name = "Resume", description = "이력서 CRUD") // (추가)
public class ResumeController {

    private final ResumeService service;
    private final CurrentUser currentUser;

    public ResumeController(ResumeService service, CurrentUser currentUser) {
        this.service = service;
        this.currentUser = currentUser;
    }

    @Operation(
            summary = "이력서 생성",
            description = "요청 본문의 값 검증 후, 로그인 사용자(토큰)의 소유로 생성합니다.",
            security = { @SecurityRequirement(name = "cookieAuth") } // (추가)
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "생성됨"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation 오류"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "미인증"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "소유권 위반(Owner mismatch 정책 위배 시)")
    }) // (추가)
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE) // (수정) consumes 명시
    public ResponseEntity<ApiResponse<Long>> create(@Valid @RequestBody CreateRequest req) {
        Long usersId = currentUser.id(); // 로그인 사용자 id
        Long id = service.create(usersId, req); // 소유자 id와 함께 생성
        return ResponseEntity.created(URI.create("/api/resumes/" + id))
                .body(ApiResponse.ok("created", id));
    }

    @Operation(
            summary = "이력서 단건 조회",
            description = "소유권(로그인 사용자) 검사 후 단건을 반환합니다.",
            security = { @SecurityRequirement(name = "cookieAuth") } // (추가)
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OK"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "미인증"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "소유권 위반"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "미존재")
    }) // (추가)
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Response>> get(@PathVariable Long id) {
        Long usersId = currentUser.id(); // 소유권 강제
        var opt = service.get(id, usersId); // (id, usersId) 시그니처
        return opt.map(r -> ResponseEntity.ok(ApiResponse.ok(r)))
                .orElseGet(() -> ResponseEntity.status(404)
                        .body(ApiResponse.fail("NOT_FOUND", "resume not found"))); // (수정)
    }

    @Operation(
            summary = "이력서 목록 조회",
            description = "로그인 사용자의 이력서를 페이지네이션/정렬/검색으로 조회합니다.",
            security = { @SecurityRequirement(name = "cookieAuth") } // (추가)
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OK"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "미인증")
    }) // (추가)
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<Response>>> list(
            @Parameter(description = "페이지(0-base)") @RequestParam(required = false) Integer page,     // (추가)
            @Parameter(description = "페이지 사이즈(1~100)") @RequestParam(required = false) Integer size, // (추가)
            @Parameter(description = "정렬규칙 예) createdAt,desc") @RequestParam(required = false) String sort, // (추가)
            @Parameter(description = "검색어") @RequestParam(required = false) String keyword) { // (추가)

        Long usersId = currentUser.id(); // 토큰에서 소유자 추출
        var pr = new PageRequest(page, size, sort);
        var res = service.list(pr, usersId, keyword);
        return ResponseEntity.ok(ApiResponse.ok(res));
    }

    @Operation(
            summary = "이력서 수정",
            description = "소유권 검사 후 부분 업데이트(PATCH)합니다.",
            security = { @SecurityRequirement(name = "cookieAuth") } // (추가)
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 완료"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation 오류"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "미인증"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "소유권 위반"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "미존재")
    }) // (추가)
    @PatchMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE) // (수정) consumes 명시
    public ResponseEntity<ApiResponse<Void>> update(
            @PathVariable Long id, @Valid @RequestBody UpdateRequest req) {

        Long usersId = currentUser.id(); // 소유권 강제
        boolean ok = service.update(id, usersId, req);
        return ok
                ? ResponseEntity.ok(ApiResponse.ok(null))
                : ResponseEntity.status(404)
                    .body(ApiResponse.fail("NOT_FOUND", "resume not found")); // (수정)
    }

    @Operation(
            summary = "이력서 삭제",
            description = "소유권 검사 후 삭제합니다.",
            security = { @SecurityRequirement(name = "cookieAuth") } // (추가)
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "삭제 완료"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "미인증"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "소유권 위반"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "미존재")
    }) // (추가)
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        Long usersId = currentUser.id(); // 소유권 강제
        boolean ok = service.delete(id, usersId);
        return ok
                ? ResponseEntity.ok(ApiResponse.ok(null))
                : ResponseEntity.status(404)
                    .body(ApiResponse.fail("NOT_FOUND", "resume not found")); // (수정)
    }
}
