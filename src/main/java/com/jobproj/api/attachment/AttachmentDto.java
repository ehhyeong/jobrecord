package com.jobproj.api.attachment;

import java.time.LocalDateTime;

public class AttachmentDto {
  public record CreateRequest(
      Long resumeId,
      String filename,
      String contentType,
      Long sizeBytes,
      String storageKey,
      Boolean isProfileImage) {}

  public record Response(
      Long attachmentId,
      Long resumeId,
      String filename,
      String contentType,
      Long sizeBytes,
      String storageKey,
      boolean isProfileImage,
      LocalDateTime createdAt) {}
}
