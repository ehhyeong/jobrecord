package com.jobproj.api.section.project;

import com.jobproj.api.common.ApiResponse;
import com.jobproj.api.section.project.ProjectDto.CreateRequest;
import com.jobproj.api.section.project.ProjectDto.Response;
import com.jobproj.api.section.project.ProjectDto.UpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@Tag(name = "섹션")
public class ProjectController {

  private final ProjectService svc;

  public ProjectController(ProjectService svc) {
    this.svc = svc;
  }

  @PostMapping("/resumes/{resumeId}/projects")
  @Operation(
      summary = "프로젝트 섹션 생성",
      tags = {"섹션"})
  public ApiResponse<Long> create(@PathVariable long resumeId, @RequestBody CreateRequest r) {
    var id =
        svc.create(
            new CreateRequest(
                resumeId,
                r.name(),
                r.role(),
                r.startDate(),
                r.endDate(),
                r.isCurrent(),
                r.summary(),
                r.techStack(),
                r.url()));
    return ApiResponse.ok(id);
  }

  @GetMapping("/resumes/{resumeId}/projects")
  @Operation(
      summary = "프로젝트 섹션 목록",
      tags = {"섹션"})
  public ApiResponse<List<Response>> list(@PathVariable long resumeId) {
    return ApiResponse.ok(svc.listByResume(resumeId));
  }

  @GetMapping("/projects/{id}")
  @Operation(
      summary = "프로젝트 섹션 조회",
      tags = {"섹션"})
  public ApiResponse<?> get(@PathVariable long id) {
    return svc.get(id)
        .<ApiResponse<?>>map(ApiResponse::ok)
        .orElseGet(() -> ApiResponse.fail("NOT_FOUND", "project not found"));
  }

  @PutMapping("/projects/{id}")
  @Operation(
      summary = "프로젝트 섹션 수정",
      tags = {"섹션"})
  public ApiResponse<?> update(@PathVariable long id, @RequestBody UpdateRequest r) {
    return svc.update(id, r)
        ? ApiResponse.ok(true)
        : ApiResponse.fail("NOT_FOUND", "project not found");
  }

  @DeleteMapping("/projects/{id}")
  @Operation(
      summary = "프로젝트 섹션 삭제",
      tags = {"섹션"})
  public ApiResponse<?> delete(@PathVariable long id) {
    return svc.delete(id)
        ? ApiResponse.ok(true)
        : ApiResponse.fail("NOT_FOUND", "project not found");
  }
}
