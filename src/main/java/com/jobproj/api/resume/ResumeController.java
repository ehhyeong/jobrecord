package com.jobproj.api.resume;

import com.jobproj.api.common.ApiResponse;
import com.jobproj.api.common.PageRequest;
import com.jobproj.api.common.PageResponse;
import com.jobproj.api.dto.ResumeDto;
import com.jobproj.api.dto.ResumeDto.CreateRequest;
import com.jobproj.api.dto.ResumeDto.Response;
import com.jobproj.api.dto.ResumeDto.UpdateRequest;
import com.jobproj.api.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping(value = "/api/resumes", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Resume", description = "이력서 CRUD")
public class ResumeController {

    private static final Logger log =
        LoggerFactory.getLogger(ResumeController.class);

    private final ResumeService service;
    private final ResumePdfService pdfService;
    private final CurrentUser currentUser;

    public ResumeController(
        ResumeService service,
        ResumePdfService pdfService,
        CurrentUser currentUser
    ) {
        this.service = service;
        this.pdfService = pdfService;
        this.currentUser = currentUser;
    }

    // ---- 기본 CRUD ----

    @Operation(
        summary = "이력서 생성",
        description = "로그인 사용자 소유로 이력서를 생성합니다.",
        security = {@SecurityRequirement(name = "cookieAuth")})
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "201", description = "생성됨"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400", description = "Validation 오류"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401", description = "미인증")
    })
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Long>> create(
        @Valid @RequestBody CreateRequest req
    ) {
        Long usersId = currentUser.id();
        Long id = service.create(usersId, req);
        return ResponseEntity.created(URI.create("/api/resumes/" + id))
            .body(ApiResponse.ok("created", id));
    }

    @Operation(
        summary = "이력서 단건 조회",
        description = "소유권 검사 후 단건 반환",
        security = {@SecurityRequirement(name = "cookieAuth")})
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Response>> get(@PathVariable Long id) {
        Long usersId = currentUser.id();
        var opt = service.get(id, usersId);
        return opt
            .map(r -> ResponseEntity.ok(ApiResponse.ok(r)))
            .orElseGet(
                () -> ResponseEntity.status(404)
                    .body(ApiResponse.fail("NOT_FOUND", "resume not found"))
            );
    }

    @Operation(
        summary = "이력서 목록 조회",
        description = "로그인 사용자의 이력서 페이지 조회",
        security = {@SecurityRequirement(name = "cookieAuth")})
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<Response>>> list(
        @Parameter(description = "페이지(0-base)") @RequestParam(required = false) Integer page,
        @Parameter(description = "페이지 사이즈(1~100)") @RequestParam(required = false) Integer size,
        @Parameter(description = "정렬규칙 예) createdAt,desc") @RequestParam(required = false) String sort,
        @Parameter(description = "검색어") @RequestParam(required = false) String keyword
    ) {
        Long usersId = currentUser.id();
        var pr = new PageRequest(page, size, sort);
        var res = service.list(pr, usersId, keyword);
        return ResponseEntity.ok(ApiResponse.ok(res));
    }

    @Operation(
        summary = "이력서 수정",
        description = "소유권 검사 후 PATCH",
        security = {@SecurityRequirement(name = "cookieAuth")})
    @PatchMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Void>> update(
        @PathVariable Long id,
        @Valid @RequestBody UpdateRequest req
    ) {
        Long usersId = currentUser.id();
        boolean ok = service.update(id, usersId, req);
        return ok
            ? ResponseEntity.ok(ApiResponse.ok(null))
            : ResponseEntity.status(404)
                .body(ApiResponse.fail("NOT_FOUND", "resume not found"));
    }

    @Operation(
        summary = "이력서 삭제",
        description = "소유권 검사 후 삭제",
        security = {@SecurityRequirement(name = "cookieAuth")})
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        Long usersId = currentUser.id();
        boolean ok = service.delete(id, usersId);
        return ok
            ? ResponseEntity.ok(ApiResponse.ok(null))
            : ResponseEntity.status(404)
                .body(ApiResponse.fail("NOT_FOUND", "resume not found"));
    }

    // ---- 프로필 사진 업로드 ----

    @Operation(
        summary = "이력서 프로필 사진 업로드",
        description = "프로필 이미지를 업로드하고 URL을 반환",
        security = {@SecurityRequirement(name = "cookieAuth")})
    @PostMapping(
        value = "/{id}/profile-image",
        consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ProfileImageResponse>> uploadProfileImage(
        @PathVariable Long id,
        @RequestParam("file") MultipartFile file
    ) {
        Long usersId = currentUser.id();

        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.fail("BAD_REQUEST", "파일이 비어 있습니다."));
        }

        String contentType = file.getContentType();
        if (contentType == null
            || (!contentType.startsWith("image/jpeg")
            && !contentType.startsWith("image/png"))) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.fail(
                    "BAD_REQUEST",
                    "jpg, png 형식의 이미지 파일만 업로드할 수 있습니다."));
        }

        try {
            String rootDir = System.getProperty("user.dir");
            Path uploadDir = Paths.get(rootDir, "uploads", "resume-profile")
                .toAbsolutePath()
                .normalize();
            Files.createDirectories(uploadDir);

            String originalName = file.getOriginalFilename();
            String ext = StringUtils.getFilenameExtension(originalName);
            if (ext == null || ext.isBlank()) {
                ext = "jpg";
            }

            String filename =
                "resume-" + id + "-" + System.currentTimeMillis() + "." + ext;
            Path target = uploadDir.resolve(filename);
            file.transferTo(target.toFile());

            String url = "/uploads/resume-profile/" + filename;

            String savedUrl = service.updateProfileImage(id, usersId, url);
            ProfileImageResponse body = new ProfileImageResponse(savedUrl);
            return ResponseEntity.ok(ApiResponse.ok(body));
        } catch (IOException e) {
            log.error("프로필 이미지 저장 실패 (resumeId={}, userId={})", id, usersId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.fail(
                    "IO_ERROR",
                    "파일 저장 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    public static class ProfileImageResponse {
        private final String profileImageUrl;

        public ProfileImageResponse(String profileImageUrl) {
            this.profileImageUrl = profileImageUrl;
        }

        public String getProfileImageUrl() {
            return profileImageUrl;
        }
    }

    // ---- PDF 내보내기 ----

    @Operation(
        summary = "이력서 PDF 다운로드",
        description = "이력서를 PDF로 다운로드",
        security = {@SecurityRequirement(name = "cookieAuth")})
    @GetMapping(value = "/{id}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> exportPdf(
        @PathVariable Long id,
        @RequestParam(name = "template", required = false) Integer template
    ) {
        Long usersId = currentUser.id();
        int templateIndex =
            (template == null || template < 1 || template > 6)
                ? 1 : template.intValue();

        byte[] pdfBytes = pdfService.generateResumePdf(id, usersId, templateIndex);
        if (pdfBytes == null || pdfBytes.length == 0) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        String filename = "resume-" + id + ".pdf";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentLength(pdfBytes.length);
        headers.setContentDispositionFormData("attachment", filename);

        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }
}

/* ====== HTML 뷰용 컨트롤러 ====== */
@Controller
@RequestMapping("/resume")
class ResumePageController {

    private final CurrentUser currentUser;

    public ResumePageController(CurrentUser currentUser) {
        this.currentUser = currentUser;
    }

    @GetMapping("/make")
    public String showMakePage() {
        return "resume/Make";
    }

    @GetMapping("/edit")
    public String showEditPage() {
        return "resume/resume-edit";
    }
}
