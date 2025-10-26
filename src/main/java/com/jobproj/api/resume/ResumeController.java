package com.jobproj.api.resume;

import com.jobproj.api.common.ApiResponse;
import com.jobproj.api.common.PageRequest;
import com.jobproj.api.common.PageResponse;
import com.jobproj.api.resume.ResumeDto.CreateRequest;
import com.jobproj.api.resume.ResumeDto.Response;
import com.jobproj.api.resume.ResumeDto.UpdateRequest;
import com.jobproj.api.security.CurrentUser;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/resumes")
public class ResumeController {

  private final ResumeService service;
  private final CurrentUser currentUser;

  public ResumeController(ResumeService service, CurrentUser currentUser) {
    this.service = service;
    this.currentUser = currentUser;
  }

  @PostMapping
  public ResponseEntity<ApiResponse<Long>> create(@RequestBody CreateRequest req) {
    Long usersId = currentUser.id(); //  로그인 사용자 id
    Long id = service.create(usersId, req); //  서비스에 usersId 전달
    return ResponseEntity.created(URI.create("/api/resumes/" + id))
        .body(ApiResponse.ok("created", id));
  }

  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<Response>> get(@PathVariable Long id) {
    return service
        .get(id)
        .map(r -> ResponseEntity.ok(ApiResponse.ok(r)))
        .orElseGet(() -> ResponseEntity.status(404).body(ApiResponse.fail("resume not found")));
  }

  @GetMapping
  public ResponseEntity<ApiResponse<PageResponse<Response>>> list(
      @RequestParam(required = false) Integer page,
      @RequestParam(required = false) Integer size,
      @RequestParam(required = false) String sort,
      @RequestParam(required = false) String keyword) {
    Long usersId = currentUser.id(); //  요청 파라미터로 안 받고 토큰에서 추출
    var pr = new PageRequest(page, size, sort);
    var res = service.list(pr, usersId, keyword); //  기존 시그니처 유지
    return ResponseEntity.ok(ApiResponse.ok(res));
  }

  @PatchMapping("/{id}")
  public ResponseEntity<ApiResponse<Void>> update(
      @PathVariable Long id, @RequestBody UpdateRequest req) {
    // (권한 체크 필요 시 currentUser.id()와 소유자 비교 로직 추가 가능)
    boolean ok = service.update(id, req);
    return ok
        ? ResponseEntity.ok(ApiResponse.ok(null))
        : ResponseEntity.status(404).body(ApiResponse.fail("resume not found"));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
    // (권한 체크 필요 시 currentUser.id()와 소유자 비교 로직 추가 가능)
    boolean ok = service.delete(id);
    return ok
        ? ResponseEntity.ok(ApiResponse.ok(null))
        : ResponseEntity.status(404).body(ApiResponse.fail("resume not found"));
  }
}
