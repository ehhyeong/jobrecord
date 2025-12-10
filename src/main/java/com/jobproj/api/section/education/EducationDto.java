package com.jobproj.api.section.education;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class EducationDto {

  // --- 생성 요청 ---
  public static class CreateRequest {
    public Long resumeId;
    public String schoolName;
    public String major;
    public String degree;
    public LocalDate startDate;
    public LocalDate endDate;
    public Boolean current;
  }

  // --- 수정 요청 ---
  public static class UpdateRequest {
    public String schoolName;
    public String major;
    public String degree;
    public LocalDate startDate;
    public LocalDate endDate;
    public Boolean current;
  }

  // --- 응답 ---
  public static class Response {
    public Long educationId;
    public Long resumeId;
    public String schoolName;
    public String major;
    public String degree;
    public LocalDate startDate;
    public LocalDate endDate;
    public Boolean current;
    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;

    public Response(
        Long educationId,
        Long resumeId,
        String schoolName,
        String major,
        String degree,
        LocalDate startDate,
        LocalDate endDate,
        Boolean current,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
      this.educationId = educationId;
      this.resumeId = resumeId;
      this.schoolName = schoolName;
      this.major = major;
      this.degree = degree;
      this.startDate = startDate;
      this.endDate = endDate;
      this.current = current;
      this.createdAt = createdAt;
      this.updatedAt = updatedAt;
    }
  }
}
