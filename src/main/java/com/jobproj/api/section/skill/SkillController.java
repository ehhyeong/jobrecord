package com.jobproj.api.section.skill;

import com.jobproj.api.common.ApiResponse;
import com.jobproj.api.section.skill.SkillDto.ResumeSkillResponse;
import com.jobproj.api.section.skill.SkillDto.ResumeSkillUpsert;
import com.jobproj.api.section.skill.SkillDto.SkillCreate;
import com.jobproj.api.section.skill.SkillDto.SkillResponse;
import com.jobproj.api.section.skill.SkillDto.SkillUpdate;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@Tag(name = "섹션")
public class SkillController {

  private final SkillService svc;

  public SkillController(SkillService svc) {
    this.svc = svc;
  }

  // ---- Skill master ----
  @PostMapping("/skills")
  @Operation(summary = "스킬 등록", tags = {"섹션"})
  public ApiResponse<Long> create(@RequestBody SkillCreate r) {
    return ApiResponse.ok(svc.create(r));
  }

  @GetMapping("/skills")
  @Operation(summary = "스킬 검색", tags = {"섹션"})
  public ApiResponse<List<SkillResponse>> search(
      @RequestParam(required = false) String q,
      @RequestParam(defaultValue = "50") int limit) {
    return ApiResponse.ok(svc.search(q, Math.min(Math.max(1, limit), 100)));
  }

  @GetMapping("/skills/{id}")
  @Operation(summary = "스킬 조회", tags = {"섹션"})
  public ApiResponse<?> get(@PathVariable long id) {
    return svc.get(id)
        .<ApiResponse<?>>map(ApiResponse::ok)
        .orElseGet(() -> ApiResponse.fail("NOT_FOUND", "skill not found"));
  }

  @PutMapping("/skills/{id}")
  @Operation(summary = "스킬 수정", tags = {"섹션"})
  public ApiResponse<?> update(@PathVariable long id, @RequestBody SkillUpdate r) {
    return svc.update(id, r)
        ? ApiResponse.ok(true)
        : ApiResponse.fail("NOT_FOUND", "skill not found");
  }

  @DeleteMapping("/skills/{id}")
  @Operation(summary = "스킬 삭제", tags = {"섹션"})
  public ApiResponse<?> delete(@PathVariable long id) {
    return svc.delete(id) ? ApiResponse.ok(true) : ApiResponse.fail("NOT_FOUND", "skill not found");
  }

  // ---- Resume-Skill mapping ----
  @PutMapping("/resumes/{resumeId}/skills/{skillId}")
  @Operation(summary = "이력서-스킬 연결/갱신", tags = {"섹션"})
  public ApiResponse<?> upsert(@PathVariable long resumeId,
                               @PathVariable long skillId,
                               @RequestBody ResumeSkillUpsert r) {
    var p = r.proficiency() == null ? 0 : r.proficiency();
    return ApiResponse.ok(svc.upsert(resumeId, skillId, p));
  }

  @GetMapping("/resumes/{resumeId}/skills")
  @Operation(summary = "이력서의 스킬 목록", tags = {"섹션"})
  public ApiResponse<List<ResumeSkillResponse>> list(@PathVariable long resumeId) {
    return ApiResponse.ok(svc.listByResume(resumeId));
  }

  @DeleteMapping("/resumes/{resumeId}/skills/{skillId}")
  @Operation(summary = "이력서-스킬 연결 해제", tags = {"섹션"})
  public ApiResponse<?> remove(@PathVariable long resumeId,
                               @PathVariable long skillId) {
    return ApiResponse.ok(svc.remove(resumeId, skillId));
  }
}
