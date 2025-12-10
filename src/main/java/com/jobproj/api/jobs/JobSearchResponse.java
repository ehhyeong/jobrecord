package com.jobproj.api.jobs;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@Schema(description = "잡 서치 응답(페이지네이션)")
public class JobSearchResponse {

    @Schema(description = "총 건수", example = "124")
    private long totalElements;

    @Schema(description = "총 페이지", example = "13")
    private int totalPages;

    @Schema(description = "현재 페이지(0부터)", example = "0")
    private int page;

    @Schema(description = "페이지 크기", example = "10")
    private int size;

    @Schema(description = "공고 리스트")
    private List<JobDto> content;
}
