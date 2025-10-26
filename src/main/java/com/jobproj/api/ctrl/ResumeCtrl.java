package com.jobproj.api.ctrl;

import com.jobproj.api.dto.ResumeDto;
import com.jobproj.api.repo.ResumeRepo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Validated
public class ResumeCtrl {
  private final ResumeRepo resumes;

  public ResumeCtrl(ResumeRepo r) {
    this.resumes = r;
  }

  @GetMapping("/resumes")
  public List<ResumeDto> listByUser(@RequestParam Long userId) {
    return resumes.findByUserId(userId);
  }

  public static record ResumeIn(
      @NotNull Long userId, @NotBlank String title, String summary, boolean isPublic) {}

  @PostMapping("/resumes")
  public Map<String, Object> create(@RequestBody ResumeIn in) {
    Long id = resumes.insert(in.userId(), in.title(), in.summary(), in.isPublic());
    return Map.of("resumeId", id);
  }

  @PutMapping("/resumes/{id}")
  public Map<String, Object> update(@PathVariable Long id, @RequestBody ResumeIn in) {
    int cnt = resumes.update(id, in.title(), in.summary(), in.isPublic());
    return Map.of("updated", cnt);
  }

  @DeleteMapping("/resumes/{id}")
  public Map<String, Object> remove(@PathVariable Long id) {
    int cnt = resumes.delete(id);
    return Map.of("deleted", cnt);
  }
}
