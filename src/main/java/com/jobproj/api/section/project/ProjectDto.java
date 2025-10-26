package com.jobproj.api.section.project;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class ProjectDto {
  public record CreateRequest(
      Long resumeId,
      String name,
      String role,
      LocalDate startDate,
      LocalDate endDate,
      Boolean isCurrent,
      String summary,
      String techStack,
      String url) {}

  public record UpdateRequest(
      String name,
      String role,
      LocalDate startDate,
      LocalDate endDate,
      Boolean isCurrent,
      String summary,
      String techStack,
      String url) {}

  public record Response(
      Long projectId,
      Long resumeId,
      String name,
      String role,
      LocalDate startDate,
      LocalDate endDate,
      boolean isCurrent,
      String summary,
      String techStack,
      String url,
      LocalDateTime createdAt,
      LocalDateTime updatedAt) {}
}
