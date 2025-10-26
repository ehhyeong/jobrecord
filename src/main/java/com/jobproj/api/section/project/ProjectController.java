package com.jobproj.api.section.project;

import com.jobproj.api.common.ApiResponse;
import com.jobproj.api.section.project.ProjectDto.*;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping
public class ProjectController {
  private final ProjectService svc;

  public ProjectController(ProjectService svc) {
    this.svc = svc;
  }

  @PostMapping("/resumes/{resumeId}/projects")
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
  public ApiResponse<List<Response>> list(@PathVariable long resumeId) {
    return ApiResponse.ok(svc.listByResume(resumeId));
  }

  @GetMapping("/projects/{id}")
  public ApiResponse<?> get(@PathVariable long id) {
    return svc.get(id)
        .<ApiResponse<?>>map(ApiResponse::ok)
        .orElseGet(() -> ApiResponse.fail("NOT_FOUND", "project not found"));
  }

  @PutMapping("/projects/{id}")
  public ApiResponse<?> update(@PathVariable long id, @RequestBody UpdateRequest r) {
    return svc.update(id, r)
        ? ApiResponse.ok(true)
        : ApiResponse.fail("NOT_FOUND", "project not found");
  }

  @DeleteMapping("/projects/{id}")
  public ApiResponse<?> delete(@PathVariable long id) {
    return svc.delete(id)
        ? ApiResponse.ok(true)
        : ApiResponse.fail("NOT_FOUND", "project not found");
  }
}
