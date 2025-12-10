package com.jobproj.api.section.skill;

import java.time.LocalDateTime;

public class SkillDto {
  // 마스터 스킬
  public record SkillCreate(String name) {}

  public record SkillUpdate(String name) {}

  public record SkillResponse(
      Long skillId, String name, LocalDateTime createdAt, LocalDateTime updatedAt) {}

  // 이력서-스킬 매핑
  public record ResumeSkillUpsert(Integer proficiency) {}

  public record ResumeSkillResponse(
      Long resumeId, Long skillId, String name, Integer proficiency) {}
}
