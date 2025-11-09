package com.jobproj.api.jobs;

import com.jobproj.api.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/jobs")
@RequiredArgsConstructor
@Tag(name = "Jobs", description = "잡코리아 연동(더미) - 추천/검색 API")
@SecurityRequirement(name = "bearerAuth")
public class JobsController {

    private final JobsService jobsService;

    @GetMapping("/recommend")
    @Operation(
            summary = "추천 공고(더미)",
            description = "리스트/상세 하단에 '잡코리아 제공' 링크 고지 필요(sourceUrl 사용).",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "200", description = "성공",
                            content = @Content(schema = @Schema(implementation = ApiResponse.class),
                                    examples = @ExampleObject(
                                            name = "success",
                                            value = """
                        {"success":true,"code":"OK","message":null,"data":[
                          {"id":"JK-2025-0001","title":"Spring 백엔드 개발자 (신입~경력)",
                           "company":"잡코리아","location":"서울 강남구","salaryMin":4500,"salaryMax":8000,
                           "experience":"MID","tags":["Spring","JPA","AWS"],"matchScore":87.5,
                           "postedAt":"2025-11-07T12:34:56",
                           "applyUrl":"https://www.jobkorea.co.kr/Recruit/GI_Read/18523338",
                           "source":"JOBKOREA",
                           "sourceUrl":"https://www.jobkorea.co.kr/Recruit/GI_Read/18523338"}]}"""
                                    )
                            )
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음")
            }
    )
    public ApiResponse<List<JobDto>> recommend(
            @Parameter(description = "반환 개수", example = "10") @RequestParam(defaultValue = "10") int limit
    ) {
        return ApiResponse.ok(jobsService.recommend(limit));
    }

    @GetMapping("/search")
    @Operation(
            summary = "공고 검색(더미)",
            description = "JobKorea 스타일 파라미터 수용(q/page/size/ob/rbcd/rpcd/area/edu1/edu2/edu3/"
                    + "pay/payterm/ctype/mcareerchk/car1/car2/car_chk/jtype).",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요청값 오류"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요")
            }
    )
    public ApiResponse<JobSearchResponse> search(
            @Parameter(description = "검색어", example = "spring backend") @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false, name = "ob") Integer orderBy,
            @RequestParam(required = false, name = "rbcd") String rbcd,
            @RequestParam(required = false, name = "rpcd") String rpcd,
            @RequestParam(required = false, name = "area") String area,
            @RequestParam(required = false, name = "edu1") Integer edu1,
            @RequestParam(required = false, name = "edu2") Integer edu2,
            @RequestParam(required = false, name = "edu3") Integer edu3,
            @RequestParam(required = false, name = "pay") Integer pay,
            @RequestParam(required = false, name = "payterm") String payterm,
            @RequestParam(required = false, name = "ctype") Integer ctype,
            @RequestParam(required = false, name = "mcareerchk") Integer mcareerchk,
            @RequestParam(required = false, name = "car1") Integer car1,
            @RequestParam(required = false, name = "car2") Integer car2,
            @RequestParam(required = false, name = "car_chk") Integer carChk,
            @RequestParam(required = false, name = "jtype") String jtype
    ) {
        return ApiResponse.ok(jobsService.search(q, page, size));
    }
}
