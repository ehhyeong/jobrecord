package com.jobproj.api.jobs;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@Schema(description = "채용 공고(더미 + 원본필드 보존)")
public class JobDto {

    // ===== 표준 필드 =====
    @Schema(description = "내부 공고 ID(더미)", example = "JK-2025-0001")
    private String id;

    @Schema(description = "공고 제목", example = "Spring 백엔드 개발자 (신입~경력)")
    private String title;

    @Schema(description = "회사명", example = "잡코리아")
    private String company;

    @Schema(description = "근무지", example = "서울 강남구")
    private String location;

    @Schema(description = "연봉(최소)", example = "4500")
    private Integer salaryMin;

    @Schema(description = "연봉(최대)", example = "8000")
    private Integer salaryMax;

    @Schema(description = "경력 레벨")
    private ExperienceLevel experience;

    @Schema(description = "태그", example = "[\"Spring\",\"JPA\",\"AWS\"]")
    private List<String> tags;

    @Schema(description = "매칭 점수(더미)", example = "87.5")
    private Double matchScore;

    @Schema(description = "게시일시")
    private LocalDateTime postedAt;

    @Schema(description = "지원/상세 URL", example = "https://www.jobkorea.co.kr/Recruit/GI_Read/12345678")
    private String applyUrl;

    // ===== 원본(JobKorea) 필드 보존 =====
    private String source;        // "JOBKOREA"
    private String sourceUrl;     // JK_URL
    private Long giNo;            // GI_No
    private List<String> areaCodes;   // AreaCode split
    private List<Integer> jobTypes;   // GI_Job_Type split
    private String partNo;        // GI_Part_No
    private Integer career;       // GI_Career
    private Integer pay;          // GI_Pay
    private String payTerm;       // GI_Pay_Term
    private String endDate;       // GI_End_Date(YYYYMMDD)
    private String wDate;         // GI_W_Date
    private String eDate;         // GI_E_Date
}
