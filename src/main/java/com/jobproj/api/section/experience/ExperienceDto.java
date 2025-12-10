package com.jobproj.api.section.experience;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class ExperienceDto {
  public record CreateRequest(
      Long resumeId,
      String companyName,
      String positionTitle,
      LocalDate startDate,
      LocalDate endDate,
      Boolean isCurrent,
      String description) {}

  public record UpdateRequest(
      String companyName,
      String positionTitle,
      LocalDate startDate,
      LocalDate endDate,
      Boolean isCurrent,
      String description) {}

  public record Response(
      Long experienceId,
      Long resumeId,
      String companyName,
      String positionTitle,
      LocalDate startDate,
      LocalDate endDate,
      boolean isCurrent,
      String description,
      LocalDateTime createdAt,
      LocalDateTime updatedAt) {}
}
