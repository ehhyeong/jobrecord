package com.jobproj.api.attachment;

import com.jobproj.api.attachment.AttachmentDto.*;
import com.jobproj.api.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "파일", description = "첨부 파일 API")
@RestController
@RequestMapping // 기존 경로 패턴을 유지 (절대경로 사용)
public class AttachmentController {

  private final AttachmentService svc;

  public AttachmentController(AttachmentService svc) {
    this.svc = svc;
  }

  /**
   * - 파일 업로드 (multipart/form-data)
   * - Swagger에서 파일선택 UI가 보이도록 consumes 지정
   * - 경로를 두 개 열어 동일 메서드로 처리: /attachments, /api/attachments
   * - resumeId는 선택(없어도 업로드 가능)
   */
  @Operation(summary = "첨부 업로드", description = "multipart/form-data로 파일 업로드 후 첨부 ID 반환")
  @PostMapping(
      value = {"/attachments", "/api/attachments"},
      consumes = MediaType.MULTIPART_FORM_DATA_VALUE
  )
  public ApiResponse<Long> upload(
      @RequestPart("file") MultipartFile file,
      @RequestParam(required = false) Long resumeId
  ) {
    long id = svc.upload(file, resumeId);
    return ApiResponse.ok(id);
  }

  @Operation(summary = "첨부 다운로드", description = "Content-Disposition: attachment; filename=... 으로 파일 다운로드")
  @GetMapping(value = {"/attachments/{id}/download", "/api/attachments/{id}/download"})
  public ResponseEntity<?> download(@PathVariable long id) {
    var p = svc.prepareDownload(id);
    return ResponseEntity
        .ok()
        .headers(p.headers())
        .body(p.resource());
  }

  /**
   * 메타데이터로 첨부 생성 (JSON)
   * 필요 시 계속 사용 가능. (업로드를 외부 스토리지에서 처리한 뒤 메타만 등록하는 케이스)
   */
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

  @GetMapping("/resumes/{resumeId}/attachments")
  public ApiResponse<List<Response>> list(@PathVariable long resumeId) {
    return ApiResponse.ok(svc.listByResume(resumeId));
  }

  @DeleteMapping("/attachments/{id}")
  public ApiResponse<?> delete(@PathVariable long id) {
    return svc.delete(id)
        ? ApiResponse.ok(true)
        : ApiResponse.fail("NOT_FOUND", "attachment not found");
  }

  @PutMapping("/attachments/{id}/profile-image")
  public ApiResponse<?> setProfile(@PathVariable long id, @RequestParam long resumeId) {
    return svc.setProfile(resumeId, id)
        ? ApiResponse.ok(true)
        : ApiResponse.fail("NOT_FOUND", "attachment not found");
  }
}
