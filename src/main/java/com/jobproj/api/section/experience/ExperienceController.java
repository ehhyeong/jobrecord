package com.jobproj.api.section.experience;

import com.jobproj.api.common.ApiResponse;
import com.jobproj.api.section.experience.ExperienceDto.*;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping
public class ExperienceController {
  private final ExperienceService svc;

  public ExperienceController(ExperienceService svc) {
    this.svc = svc;
  }

  @PostMapping("/resumes/{resumeId}/experiences")
  public ApiResponse<Long> create(@PathVariable long resumeId, @RequestBody CreateRequest r) {
    var id =
        svc.create(
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
  public ApiResponse<List<Response>> list(@PathVariable long resumeId) {
    return ApiResponse.ok(svc.listByResume(resumeId));
  }

  @GetMapping("/experiences/{id}")
  public ApiResponse<?> get(@PathVariable long id) {
    return svc.get(id)
        .<ApiResponse<?>>map(ApiResponse::ok)
        .orElseGet(() -> ApiResponse.fail("NOT_FOUND", "experience not found"));
  }

  @PutMapping("/experiences/{id}")
  public ApiResponse<?> update(@PathVariable long id, @RequestBody UpdateRequest r) {
    return svc.update(id, r)
        ? ApiResponse.ok(true)
        : ApiResponse.fail("NOT_FOUND", "experience not found");
  }

  @DeleteMapping("/experiences/{id}")
  public ApiResponse<?> delete(@PathVariable long id) {
    return svc.delete(id)
        ? ApiResponse.ok(true)
        : ApiResponse.fail("NOT_FOUND", "experience not found");
  }
}
