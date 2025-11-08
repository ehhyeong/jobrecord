package com.jobproj.api.resume;

// 8주차 추가: Validation 어노테이션 임포트
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public class ResumeDto {

  public static class CreateRequest {
    // (usersId는 컨트롤러에서 CurrentUser.id()로 덮어쓰므로 검증 불필요)
    public Long usersId;

    // 8주차 수정: Validation 추가
    @NotBlank(message = "제목은 필수입니다.")
    @Size(max = 200, message = "제목은 200자 이내여야 합니다.")
    public String title;

    // 8주차 수정: Validation 추가 (필수는 아님)
    @Size(max = 2000, message = "요약은 2000자 이내여야 합니다.")
    public String summary;

    // 8주차 수정: Validation 추가
    @NotNull(message = "공개 여부는 필수입니다.")
    public Boolean isPublic;
  }

  public static class UpdateRequest {
    // 8주차 수정: Validation 추가
    @NotBlank(message = "제목은 필수입니다.")
    @Size(max = 200, message = "제목은 200자 이내여야 합니다.")
    public String title;

    // 8주차 수정: Validation 추가 (필수는 아님)
    @Size(max = 2000, message = "요약은 2000자 이내여야 합니다.")
    public String summary;

    // 8주차 수정: Validation 추가
    @NotNull(message = "공개 여부는 필수입니다.")
    public Boolean isPublic;
  }

  public static class Response {
    // (응답 DTO는 검증이 필요 없으므로 수정사항 없음)
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
