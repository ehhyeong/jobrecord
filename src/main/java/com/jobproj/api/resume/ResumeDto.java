package com.jobproj.api.resume;

import java.time.LocalDateTime;

public class ResumeDto {

  public static class CreateRequest {
    public Long usersId;
    public String title;
    public String summary;
    public Boolean isPublic;
  }

  public static class UpdateRequest {
    public String title;
    public String summary;
    public Boolean isPublic;
  }

  public static class Response {
    public Long resumeId;
    public Long usersId;
    public String title;
    public String summary;
    public Boolean isPublic;
    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;

    public Response(
        Long resumeId,
        Long usersId,
        String title,
        String summary,
        Boolean isPublic,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
      this.resumeId = resumeId;
      this.usersId = usersId;
      this.title = title;
      this.summary = summary;
      this.isPublic = isPublic;
      this.createdAt = createdAt;
      this.updatedAt = updatedAt;
    }
  }
}
