package com.jobproj.api.attachment;

import com.jobproj.api.attachment.AttachmentDto.*;
import com.jobproj.api.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@Tag(name = "파일", description = "첨부 파일 API")
@RestController
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
public class AttachmentController {

    private final AttachmentService svc;

    public AttachmentController(AttachmentService svc) {
        this.svc = svc;
    }

    @Operation(
            summary = "첨부 업로드",
            description = "multipart/form-data로 파일 업로드 후 첨부 ID 반환",
            security = { @SecurityRequirement(name = "cookieAuth") }
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "업로드 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation/MIME 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "미인증"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "소유권 위반"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "413", description = "업로드 용량 초과")
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody( // (문서용 Swagger RequestBody는 풀네임 사용)
            content = @Content(
                    mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                    schema = @Schema(implementation = AttachmentUploadSchema.class)
            )
    )
    @PostMapping(
            value = {"/attachments", "/api/attachments"},
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Long>> upload(   // ← 반환 타입은 우리 ApiResponse
                                                       @Parameter(description = "업로드 파일 (png, jpg, pdf 허용)")
                                                       @RequestPart("file") MultipartFile file,
                                                       @Parameter(description = "연결할 이력서 ID(선택)")
                                                       @RequestParam(required = false) Long resumeId) {

        long id = svc.upload(file, resumeId);
        // 문서(201)와 실제 응답 코드 일치
        return ResponseEntity.status(201).body(ApiResponse.ok(id));
    }

    @Operation(
            summary = "첨부 다운로드",
            description = "Content-Disposition: attachment; filename=... 으로 파일 다운로드",
            security = { @SecurityRequirement(name = "cookieAuth") }
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "OK",
                    headers = {
                            @Header(name = "Content-Disposition",
                                    description = "attachment; filename=\"...\"")
                    }
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "미인증"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "소유권 위반"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "미존재")
    })
    @GetMapping(value = {"/attachments/{id}/download", "/api/attachments/{id}/download"})
    public ResponseEntity<?> download(@PathVariable long id) {
        var p = svc.prepareDownload(id);
        return ResponseEntity.ok().headers(p.headers()).body(p.resource());
    }

    @Operation(
            summary = "첨부 메타 생성",
            description = "외부 스토리지 업로드 후 메타데이터만 등록",
            security = { @SecurityRequirement(name = "cookieAuth") }
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "생성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "미인증"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "소유권 위반"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "이력서 미존재")
    })
    @PostMapping("/resumes/{resumeId}/attachments")
    public ApiResponse<Long> create(@PathVariable long resumeId, @RequestBody CreateRequest r) {
        var id =
                svc.create(
                        new CreateRequest(
                                resumeId,
                                r.filename(),
                                r.contentType(),
                                r.sizeBytes(),
                                r.storageKey(),
                                r.isProfileImage()));
        return ApiResponse.ok(id);
    }

    @Operation(
            summary = "이력서별 첨부 목록",
            description = "이력서에 연결된 첨부 목록 조회",
            security = { @SecurityRequirement(name = "cookieAuth") }
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OK"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "미인증"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "소유권 위반"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "이력서 미존재")
    })
    @GetMapping("/resumes/{resumeId}/attachments")
    public ApiResponse<List<Response>> list(@PathVariable long resumeId) {
        return ApiResponse.ok(svc.listByResume(resumeId));
    }

    @Operation(
            summary = "첨부 삭제",
            description = "첨부 삭제(소유권 검사)",
            security = { @SecurityRequirement(name = "cookieAuth") }
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "미인증"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "소유권 위반"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "미존재")
    })
    @DeleteMapping("/attachments/{id}")
    public ApiResponse<?> delete(@PathVariable long id) {
        return svc.delete(id)
                ? ApiResponse.ok(true)
                : ApiResponse.fail("NOT_FOUND", "attachment not found");
    }

    @Operation(
            summary = "프로필 이미지 설정",
            description = "해당 이력서의 프로필 이미지를 지정",
            security = { @SecurityRequirement(name = "cookieAuth") }
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "설정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "미인증"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "소유권 위반"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "첨부/이력서 미존재")
    })
    @PutMapping("/attachments/{id}/profile-image")
    public ApiResponse<?> setProfile(@PathVariable long id, @RequestParam long resumeId) {
        return svc.setProfile(resumeId, id)
                ? ApiResponse.ok(true)
                : ApiResponse.fail("NOT_FOUND", "attachment not found");
    }

    // 문서 전용 업로드 스키마 (Swagger에 multipart 필드 구조 노출)
    @Schema(name = "AttachmentUploadSchema")
    static class AttachmentUploadSchema {
        @Schema(description = "이력서 ID(선택)", example = "1")
        public Long resumeId;
        @Schema(
                type = "string",
                format = "binary",
                description = "파일(binary). 허용: image/png, image/jpeg, application/pdf"
        )
        public byte[] file;
    }
}
