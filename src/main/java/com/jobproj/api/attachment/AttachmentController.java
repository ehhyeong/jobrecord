package com.jobproj.api.attachment;

import com.jobproj.api.attachment.AttachmentDto.*;
import com.jobproj.api.common.ApiResponse;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping
public class AttachmentController {
  private final AttachmentService svc;

  public AttachmentController(AttachmentService svc) {
    this.svc = svc;
  }

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
