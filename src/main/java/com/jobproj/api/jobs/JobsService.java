package com.jobproj.api.jobs;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class JobsService {

    /**
     * 추천 공고 더미 생성.
     * - 최소 limit 개수 보장 (샘플 2개를 기반으로 패턴 확장)
     * - postedAt, id, 급여가 항목마다 달라지도록 변형
     */
    public List<JobDto> recommend(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 50)); // 1~50
        var now = LocalDateTime.now();

        // 베이스 샘플 2개
        var base = List.of(
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

        if (safeLimit <= base.size()) {
            return base.subList(0, safeLimit);
        }

        var list = new ArrayList<JobDto>(safeLimit);
        list.addAll(base);

        // base를 패턴 복제하여 safeLimit까지 채우기
        for (int i = base.size(); i < safeLimit; i++) {
            var b = base.get(i % base.size());
            int idx = i + 1;
            int salaryMin = (b.getSalaryMin() != null ? b.getSalaryMin() : 4000) + (idx % 5) * 100;
            int salaryMax = Math.max(salaryMin + 1000, (b.getSalaryMax() != null ? b.getSalaryMax() : 7000) + (idx % 3) * 150);

            list.add(JobDto.builder()
                .id("JK-2025-" + String.format("%04d", idx + 1))
                .title(b.getTitle() + " #" + idx)
                .company(idx % 2 == 0 ? b.getCompany() : "잡코리아")
                .location(idx % 3 == 0 ? "서울" : (idx % 3 == 1 ? "경기" : "부산"))
                .salaryMin(salaryMin)
                .salaryMax(salaryMax)
                .experience((idx % 3 == 0) ? ExperienceLevel.JUNIOR :
                            (idx % 3 == 1) ? ExperienceLevel.MID : ExperienceLevel.SENIOR)
                .tags((idx % 2 == 0) ? b.getTags() : List.of("Java", "Spring", "JPA"))
                .matchScore(Math.round((70 + (idx % 30)) * 10.0) / 10.0)
                .postedAt(now.minusDays(idx % 10).minusHours(idx % 24))
                .applyUrl(b.getApplyUrl())
                .source("JOBKOREA")
                .sourceUrl(b.getSourceUrl())
                .giNo(b.getGiNo() + idx)
                .areaCodes(b.getAreaCodes())
                .jobTypes(b.getJobTypes())
                .partNo(b.getPartNo())
                .career(Math.min(10, b.getCareer() != null ? b.getCareer() + (idx % 3) : (idx % 6)))
                .pay(b.getPay())
                .payTerm(b.getPayTerm())
                .endDate(b.getEndDate())
                .wDate(b.getWDate())
                .eDate(b.getEDate())
                .build());
        }
        return list;
    }

    /**
     * 간단 검색(q) + 페이지네이션(0-base)
     * - q: title/company/location/tags에 포함되면 매칭 (대소문자 무시)
     * - size: 1~50, page: 0 이상
     */
    public JobSearchResponse search(String q, int page, int size) {
        int safeSize = Math.max(1, Math.min(size, 50));
        int safePage = Math.max(0, page);

        // 검색 풀: 추천 50개 생성
        var pool = recommend(50);

        // q 필터
        List<JobDto> filtered;
        if (q == null || q.isBlank()) {
            filtered = pool;
        } else {
            String term = q.toLowerCase();
            filtered = pool.stream().filter(j ->
                (j.getTitle() != null && j.getTitle().toLowerCase().contains(term)) ||
                (j.getCompany() != null && j.getCompany().toLowerCase().contains(term)) ||
                (j.getLocation() != null && j.getLocation().toLowerCase().contains(term)) ||
                (j.getTags() != null && j.getTags().stream().anyMatch(t -> t != null && t.toLowerCase().contains(term)))
            ).toList();
        }

        int total = filtered.size();
        int from = Math.min(safePage * safeSize, total);
        int to = Math.min(from + safeSize, total);
        var slice = (from >= to) ? List.<JobDto>of() : filtered.subList(from, to);
        int totalPages = (int) Math.ceil(total / (double) safeSize);

        return JobSearchResponse.builder()
            .totalElements(total)
            .totalPages(totalPages)
            .page(safePage)
            .size(safeSize)
            .content(slice)
            .build();
    }
}
