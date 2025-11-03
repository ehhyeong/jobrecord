package com.jobproj.api.section.experience;

import com.jobproj.api.common.ApiResponse;
import com.jobproj.api.section.experience.ExperienceDto.CreateRequest;
import com.jobproj.api.section.experience.ExperienceDto.Response;
import com.jobproj.api.section.experience.ExperienceDto.UpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@Tag(name = "섹션")
public class ExperienceController {

  private final ExperienceService svc;

  public ExperienceController(ExperienceService svc) {
    this.svc = svc;
  }

  @PostMapping("/resumes/{resumeId}/experiences")
  @Operation(summary = "경력 섹션 생성", tags = {"섹션"})
  public ApiResponse<Long> create(@PathVariable long resumeId,
                                  @RequestBody CreateRequest r) {
    var id = svc.create(
        new CreateRequest(
            resumeId,
            r.companyName(),
            r.positionTitle(),
            r.startDate(),
            r.endDate(),
            r.isCurrent(),
            r.description()));
    return ApiResponse.ok(id);
  }

  @GetMapping("/resumes/{resumeId}/experiences")
  @Operation(summary = "경력 섩션 목록", tags = {"섹션"})
  public ApiResponse<List<Response>> list(@PathVariable long resumeId) {
    return ApiResponse.ok(svc.listByResume(resumeId));
  }

  @GetMapping("/experiences/{id}")
  @Operation(summary = "경력 섹션 조회", tags = {"섹션"})
  public ApiResponse<?> get(@PathVariable long id) {
    return svc.get(id)
        .<ApiResponse<?>>map(ApiResponse::ok)
        .orElseGet(() -> ApiResponse.fail("NOT_FOUND", "experience not found"));
  }

  @PutMapping("/experiences/{id}")
  @Operation(summary = "경력 섹션 수정", tags = {"섹션"})
  public ApiResponse<?> update(@PathVariable long id, @RequestBody UpdateRequest r) {
    return svc.update(id, r)
        ? ApiResponse.ok(true)
        : ApiResponse.fail("NOT_FOUND", "experience not found");
  }

  @DeleteMapping("/experiences/{id}")
  @Operation(summary = "경력 섹션 삭제", tags = {"섹션"})
  public ApiResponse<?> delete(@PathVariable long id) {
    return svc.delete(id)
        ? ApiResponse.ok(true)
        : ApiResponse.fail("NOT_FOUND", "experience not found");
  }
}
