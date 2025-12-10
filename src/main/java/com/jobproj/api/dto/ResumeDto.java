package com.jobproj.api.dto;

// 8주차 추가: Validation 어노테이션 임포트
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
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

    // 융합프로젝트 김태형 12주차 : 이력서 기본 정보(이름/전화번호/이메일/생년월일) 필드
    @Size(max = 50, message = "이름은 50자 이내여야 합니다.")
    public String name;

    @Size(max = 20, message = "전화번호는 20자 이내여야 합니다.")
    public String phone;

    @Size(max = 100, message = "이메일은 100자 이내여야 합니다.")
    public String email;

    public LocalDate birthDate;

    // 2233076 12주차 추가: 템플릿 ID (1-6)
    public Integer templateId;
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

    // 융합프로젝트 김태형 12주차 : 이력서 기본 정보(이름/전화번호/이메일/생년월일) 필드
    @Size(max = 50, message = "이름은 50자 이내여야 합니다.")
    public String name;

    @Size(max = 20, message = "전화번호는 20자 이내여야 합니다.")
    public String phone;

    @Size(max = 100, message = "이메일은 100자 이내여야 합니다.")
    public String email;

    public LocalDate birthDate;

    // 2233076 12주차 추가: 템플릿 ID (1-6)
    public Integer templateId;
  }

  public static class Response {
    // (응답 DTO는 검증이 필요 없으므로 Validation 없음)
    public Long resumeId;
    public Long usersId;
    public String title;
    public String summary;
    public Boolean isPublic;
    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;

    // 융합프로젝트 김태형 12주차 : 이력서 기본 정보(이름/전화번호/이메일/생년월일)
    public String name;
    public String phone;
    public String email;
    public LocalDate birthDate;

    // 융합프로젝트 김태형 12주차 : 프로필 사진 URL
    public String profileImageUrl;

    // 2233076 12주차 추가: 템플릿 ID (1-6)
    public Integer templateId;

    // 2233076 12주차 수정: 템플릿 ID 포함하는 생성자
    // 융합프로젝트 김태형 12주차 :
    //  이력서 기본 정보 + 프로필 사진 필드를 포함하는 생성자
    public Response(
        Long resumeId,
        Long usersId,
        String title,
        String summary,
        Boolean isPublic,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String name,
        String phone,
        String email,
        LocalDate birthDate,
        String profileImageUrl,
        Integer templateId) {
      this.resumeId = resumeId;
      this.usersId = usersId;
      this.title = title;
      this.summary = summary;
      this.isPublic = isPublic;
      this.createdAt = createdAt;
      this.updatedAt = updatedAt;
      this.name = name;
      this.phone = phone;
      this.email = email;
      this.birthDate = birthDate;
      this.profileImageUrl = profileImageUrl;
      this.templateId = templateId;
    }
  }
}
