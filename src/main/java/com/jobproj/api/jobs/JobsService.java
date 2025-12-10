package com.jobproj.api.jobs;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 2233076 13주차 추가
 * JobsService - 잡코리아 API 연동
 * 
 * 현재 상태: Mock 데이터 사용 중
 * - API 키가 있으면 실제 API 호출
 * - API 키가 없으면 Mock 데이터 반환 (테스트용)
 * 
 * API 연결 시 작업 필요
 * 1. application.yml에 실제 API URL과 키 설정
 * 2. parseJobkoreaXmlResponse() 메서드에 XML 파싱 로직 구현
 * 3. search() 메서드를 실제 검색 API로 교체
 * 4. generateMockData() 메서드 삭제
 * 5. 모든 "API 연결 시 삭제" 주석이 달린 코드 제거
 */
@Slf4j
@Service
public class JobsService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${jobkorea.api.url:}")
    private String apiUrl;

    @Value("${jobkorea.api.key:}")
    private String apiKey;

    @Value("${jobkorea.api.default-keywords:Java,Spring,백엔드}")
    private String defaultKeywords;

    /**
     * 추천 공고 조회
     */
    public List<JobDto> recommend(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 50));

        if (isApiConfigured()) {
            log.info("잡코리아 실제 API 호출 - limit: {}", safeLimit);
            return callRealJobkoreaApi(defaultKeywords, safeLimit);
        } else {
            log.info("잡코리아 Mock 데이터 반환 - limit: {}", safeLimit);
            // API 연결 시 삭제: Mock 데이터 반환 로직
            return generateMockData(safeLimit);
        }
    }

    private boolean isApiConfigured() {
        return apiUrl != null && !apiUrl.isBlank() && apiKey != null && !apiKey.isBlank();
    }

    private List<JobDto> callRealJobkoreaApi(String keywords, int limit) {
        try {
            // 2233076 13주차 추가: EUC-KR 인코딩 처리
            String requestUrl = String.format("%s&keyword=%s&rbcd=10007&ob=2&size=%d", 
                apiUrl, keywords, limit);
            log.info("잡코리아 API 요청: {}", requestUrl);
            
            // EUC-KR 인코딩으로 응답 받기
            byte[] responseBytes = restTemplate.getForObject(requestUrl, byte[].class);
            String response = new String(responseBytes, "EUC-KR");
            
            log.info("===== 잡코리아 API 전체 응답 =====");
            log.info(response);
            log.info("===== 응답 종료 =====");
            
            return parseJobkoreaXmlResponse(response);
        } catch (Exception e) {
            log.error("잡코리아 API 호출 실패, Mock 데이터로 대체", e);
            return generateMockData(limit);
        }
    }

    /**
     * 2233076 13주차 추가: 잡코리아 XML 응답 파싱
     */
    private List<JobDto> parseJobkoreaXmlResponse(String xmlResponse) {
        List<JobDto> jobs = new ArrayList<>();
        
        try {
            log.info("XML 파싱 시작, 응답 길이: {}", xmlResponse != null ? xmlResponse.length() : 0);
            
            // 간단한 XML 파싱 (정규식 사용)
            String[] items = xmlResponse.split("<Items>");
            log.info("분리된 Items 개수: {}", items.length - 1);
            
            for (int i = 1; i < items.length; i++) {
                String item = items[i].split("</Items>")[0];
                
                try {
                    String giNo = extractValue(item, "GI_No");
                    String title = extractValue(item, "GI_Subject");
                    String company = extractValue(item, "C_Name");
                    
                    log.debug("공고 #{}: GI_No={}, Title={}, Company={}", i, giNo, title, company);
                    
                    JobDto job = JobDto.builder()
                        .id(giNo)
                        .title(title)
                        .company(company)
                        .location(parseLocation(extractValue(item, "AreaCode")))
                        .experience(parseExperience(extractValue(item, "GI_Career")))
                        .tags(parseKeywords(extractValue(item, "GI_Keyword")))
                        .postedAt(parseDate(extractValue(item, "GI_W_Date")))
                        .applyUrl(extractValue(item, "JK_URL"))
                        .source("JOBKOREA")
                        .sourceUrl(extractValue(item, "JK_URL"))
                        .giNo(parseLong(giNo))
                        .build();
                    
                    jobs.add(job);
                } catch (Exception e) {
                    log.warn("개별 공고 파싱 실패, 건너뜀: {}", e.getMessage());
                }
            }
            
            log.info("잡코리아 XML 파싱 완료: {}개 공고", jobs.size());
            return jobs;
            
        } catch (Exception e) {
            log.error("XML 파싱 실패, Mock 데이터로 대체", e);
            // API 연결 시 삭제: Mock 데이터로 폴백
            return generateMockData(10);
        }
    }
    
    /**
     * 2233076 13주차 추가: XML에서 값 추출
     */
    private String extractValue(String xml, String tagName) {
        try {
            String openTag = "<" + tagName + ">";
            String closeTag = "</" + tagName + ">";
            int start = xml.indexOf(openTag);
            int end = xml.indexOf(closeTag);
            
            if (start != -1 && end != -1 && start < end) {
                return xml.substring(start + openTag.length(), end).trim();
            }
        } catch (Exception e) {
            log.debug("태그 추출 실패: {}", tagName);
        }
        return "";
    }
    
    /**
     * 2233076 13주차 추가: 지역 코드 파싱
     */
    private String parseLocation(String areaCode) {
        if (areaCode == null || areaCode.isEmpty()) {
            return "전국";
        }
        
        // 첫 번째 지역 코드만 사용 (B180 → 서울 등)
        String firstCode = areaCode.split(",")[0].trim();
        
        // 간단한 매핑 (필요시 확장)
        if (firstCode.startsWith("B")) {
            return "서울";
        } else if (firstCode.startsWith("I")) {
            return "경기";
        }
        
        return "전국";
    }
    
    /**
     * 2233076 13주차 추가: 경력 파싱
     */
    private ExperienceLevel parseExperience(String careerCode) {
        if (careerCode == null || careerCode.isEmpty()) {
            return ExperienceLevel.JUNIOR;
        }
        
        try {
            int code = Integer.parseInt(careerCode.trim());
            // 3: 신입, 4: 경력, 5: 신입+경력
            if (code == 3) return ExperienceLevel.JUNIOR;
            if (code == 4) return ExperienceLevel.SENIOR;
            return ExperienceLevel.MID;
        } catch (Exception e) {
            return ExperienceLevel.JUNIOR;
        }
    }
    
    /**
     * 2233076 13주차 추가: 키워드 파싱 (태그로 변환)
     */
    private List<String> parseKeywords(String keywords) {
        if (keywords == null || keywords.isEmpty()) {
            return List.of();
        }
        
        // 쉼표로 구분된 키워드를 리스트로 변환
        return List.of(keywords.split(","))
            .stream()
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .limit(10) // 최대 10개만
            .toList();
    }
    
    /**
     * 2233076 13주차 추가: 날짜 파싱 (yyyyMMdd → LocalDateTime)
     */
    private LocalDateTime parseDate(String dateStr) {
        if (dateStr == null || dateStr.length() != 8) {
            return LocalDateTime.now();
        }
        
        try {
            int year = Integer.parseInt(dateStr.substring(0, 4));
            int month = Integer.parseInt(dateStr.substring(4, 6));
            int day = Integer.parseInt(dateStr.substring(6, 8));
            return LocalDateTime.of(year, month, day, 0, 0);
        } catch (Exception e) {
            return LocalDateTime.now();
        }
    }
    
    /**
     * 2233076 13주차 추가: Long 파싱
     */
    private Long parseLong(String value) {
        try {
            return Long.parseLong(value.trim());
        } catch (Exception e) {
            return 0L;
        }
    }

    public JobSearchResponse search(String q, int page, int size) {
        int safeSize = Math.max(1, Math.min(size, 50));
        int safePage = Math.max(0, page);
        // API 연결 시 수정 필요: 실제 검색 API 호출로 교체
        var pool = recommend(50);
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

    // API 연결 시 삭제: Mock 데이터 생성 메서드 전체
    private List<JobDto> generateMockData(int limit) {
        var now = LocalDateTime.now();
        var base = List.of(
            JobDto.builder().id("JK-2025-0001").title("Spring 백엔드 개발자 (신입~경력)").company("잡코리아").location("서울 강남구")
                .salaryMin(4500).salaryMax(8000).experience(ExperienceLevel.MID).tags(List.of("Spring","JPA","AWS"))
                .matchScore(87.5).postedAt(now.minusDays(2)).applyUrl("https://www.jobkorea.co.kr/Recruit/GI_Read/18523338")
                .source("JOBKOREA").sourceUrl("https://www.jobkorea.co.kr/Recruit/GI_Read/18523338").giNo(18523338L)
                .areaCodes(List.of("I010","I130")).jobTypes(List.of(1,2)).partNo("1000229").career(3).pay(1)
                .payTerm("3000,6000").endDate("20251224").wDate("20251107").eDate("20251107").build(),
            JobDto.builder().id("JK-2025-0002").title("Java 백엔드 엔지니어").company("원티드랩").location("서울 마포구")
                .salaryMin(5000).salaryMax(9000).experience(ExperienceLevel.SENIOR).tags(List.of("Java17","SpringBoot","MySQL"))
                .matchScore(82.0).postedAt(now.minusDays(5)).applyUrl("https://www.wanted.co.kr/wd/000000")
                .source("JOBKOREA").sourceUrl("https://www.wanted.co.kr/wd/000000").giNo(18520000L)
                .areaCodes(List.of("I000")).jobTypes(List.of(1)).partNo("1000229").career(5).pay(1)
                .payTerm("6000,9000").endDate("20251220").wDate("20251101").eDate("20251101").build()
        );
        if (limit <= base.size()) return base.subList(0, limit);
        var list = new ArrayList<JobDto>(limit);
        list.addAll(base);
        for (int i = base.size(); i < limit; i++) {
            var b = base.get(i % base.size());
            int idx = i + 1;
            int salaryMin = (b.getSalaryMin() != null ? b.getSalaryMin() : 4000) + (idx % 5) * 100;
            int salaryMax = Math.max(salaryMin + 1000, (b.getSalaryMax() != null ? b.getSalaryMax() : 7000) + (idx % 3) * 150);
            list.add(JobDto.builder().id("JK-2025-" + String.format("%04d", idx + 1)).title(b.getTitle() + " #" + idx)
                .company(idx % 2 == 0 ? b.getCompany() : "잡코리아")
                .location(idx % 3 == 0 ? "서울" : (idx % 3 == 1 ? "경기" : "부산"))
                .salaryMin(salaryMin).salaryMax(salaryMax)
                .experience((idx % 3 == 0) ? ExperienceLevel.JUNIOR : (idx % 3 == 1) ? ExperienceLevel.MID : ExperienceLevel.SENIOR)
                .tags((idx % 2 == 0) ? b.getTags() : List.of("Java", "Spring", "JPA"))
                .matchScore(Math.round((70 + (idx % 30)) * 10.0) / 10.0)
                .postedAt(now.minusDays(idx % 10).minusHours(idx % 24))
                .applyUrl(b.getApplyUrl()).source("JOBKOREA").sourceUrl(b.getSourceUrl()).giNo(b.getGiNo() + idx)
                .areaCodes(b.getAreaCodes()).jobTypes(b.getJobTypes()).partNo(b.getPartNo())
                .career(Math.min(10, b.getCareer() != null ? b.getCareer() + (idx % 3) : (idx % 6)))
                .pay(b.getPay()).payTerm(b.getPayTerm()).endDate(b.getEndDate())
                .wDate(b.getWDate()).eDate(b.getEDate()).build());
        }
        return list;
    }
}
