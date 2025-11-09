package com.jobproj.api.jobs;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class JobsService {

    public List<JobDto> recommend(int limit) {
        var now = LocalDateTime.now();
        var list = List.of(
            JobDto.builder()
                .id("JK-2025-0001")
                .title("Spring 백엔드 개발자 (신입~경력)")
                .company("잡코리아")
                .location("서울 강남구")
                .salaryMin(4500).salaryMax(8000)
                .experience(ExperienceLevel.MID)
                .tags(List.of("Spring","JPA","AWS"))
                .matchScore(87.5)
                .postedAt(now.minusDays(2))
                .applyUrl("https://www.jobkorea.co.kr/Recruit/GI_Read/18523338")
                .source("JOBKOREA")
                .sourceUrl("https://www.jobkorea.co.kr/Recruit/GI_Read/18523338")
                .giNo(18523338L)
                .areaCodes(List.of("I010","I130"))
                .jobTypes(List.of(1,2))
                .partNo("1000229")
                .career(3)
                .pay(1)
                .payTerm("3000,6000")
                .endDate("20251224")
                .wDate("20251107")
                .eDate("20251107")
                .build(),
            JobDto.builder()
                .id("JK-2025-0002")
                .title("Java 백엔드 엔지니어")
                .company("원티드랩")
                .location("서울 마포구")
                .salaryMin(5000).salaryMax(9000)
                .experience(ExperienceLevel.SENIOR)
                .tags(List.of("Java17","SpringBoot","MySQL"))
                .matchScore(82.0)
                .postedAt(now.minusDays(5))
                .applyUrl("https://www.wanted.co.kr/wd/000000")
                .source("JOBKOREA")
                .sourceUrl("https://www.wanted.co.kr/wd/000000")
                .giNo(18520000L)
                .areaCodes(List.of("I000"))
                .jobTypes(List.of(1))
                .partNo("1000229")
                .career(5)
                .pay(1)
                .payTerm("6000,9000")
                .endDate("20251220")
                .wDate("20251101")
                .eDate("20251101")
                .build()
        );
        return list.stream().limit(Math.max(1, limit)).toList();
    }

    public JobSearchResponse search(String q, int page, int size) {
        var all = recommend(50);
        int realSize = Math.min(Math.max(size, 1), 100);
        int from = Math.max(0, page * realSize);
        int to = Math.min(all.size(), from + realSize);
        var slice = from >= all.size() ? List.<JobDto>of() : all.subList(from, to);
        int totalPages = (int) Math.ceil(all.size() / (double) realSize);

        return JobSearchResponse.builder()
            .totalElements(all.size())
            .totalPages(totalPages)
            .page(page)
            .size(realSize)
            .content(slice)
            .build();
    }
}
