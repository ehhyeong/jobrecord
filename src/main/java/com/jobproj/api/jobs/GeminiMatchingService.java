package com.jobproj.api.jobs;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.Period;
import java.util.*;

/**
 * 2233076 13주차 추가
 * GeminiMatchingService - Gemini API를 활용한 이력서-채용공고 매칭
 */
@Slf4j
@Service
public class GeminiMatchingService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${gemini.api.key:}")
    private String apiKey;

    @Value("${gemini.api.url}")
    private String apiUrl;

    /**
     * 2233076 13주차 개선: 종합 이력서 정보를 포함한 매칭
     */
    public MatchResult calculateMatchScore(ResumeInfo resumeInfo, JobDto job) {
        // API 키가 없으면 기본 매칭 사용
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("Gemini API 키가 없습니다. 기본 매칭을 사용합니다.");
            return simpleMatch(resumeInfo, job);
        }

        try {
            String prompt = buildPrompt(resumeInfo, job);
            String response = callGeminiApi(prompt);
            return parseGeminiResponse(response);
        } catch (Exception e) {
            log.error("Gemini API 호출 실패, 기본 매칭으로 대체", e);
            return simpleMatch(resumeInfo, job);
        }
    }

    /**
     * 2233076 13주차 개선: 배치 처리 (API 호출 최소화)
     */
    public List<MatchResult> calculateMatchScoreBatch(ResumeInfo resumeInfo, List<JobDto> jobs) {
        // API 키가 없으면 기본 매칭 사용
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("Gemini API 키가 없습니다. 기본 매칭을 사용합니다.");
            return jobs.stream()
                .map(job -> simpleMatch(resumeInfo, job))
                .collect(java.util.stream.Collectors.toList());
        }

        try {
            String prompt = buildBatchPrompt(resumeInfo, jobs);
            String response = callGeminiApi(prompt);
            return parseBatchGeminiResponse(response, jobs);
        } catch (Exception e) {
            log.error("Gemini API 배치 호출 실패, 기본 매칭으로 대체", e);
            return jobs.stream()
                .map(job -> simpleMatch(resumeInfo, job))
                .collect(java.util.stream.Collectors.toList());
        }
    }

    /**
     * 2233076 13주차 개선: 종합 정보를 포함한 프롬프트 생성
     */
    private String buildPrompt(ResumeInfo resume, JobDto job) {
        String skillsStr = resume.getSkills() != null ? String.join(", ", resume.getSkills()) : "없음";
        String tagsStr = job.getTags() != null ? String.join(", ", job.getTags()) : "없음";
        
        // 학력 정보 포맷팅
        String educationStr = formatEducations(resume.getEducations());
        
        // 경력 정보 포맷팅
        String experienceStr = formatExperiences(resume.getExperiences());
        
        // 총 경력 계산
        String totalCareer = calculateTotalCareer(resume.getExperiences());

        return String.format("""
            당신은 채용 전문가입니다. 아래 이력서와 채용공고를 분석하여 매칭도를 평가해주세요.
            
            [이력서 정보]
            이름: %s
            학력: %s
            총 경력: %s
            경력 상세:
            %s
            
            보유 스킬: %s
            
            [채용공고 정보]
            공고 제목: %s
            회사명: %s
            요구 기술: %s
            위치: %s
            경력 요구사항: %s
            
            [평가 기준]
            1. 스킬 매칭 (40점):
               - 오타, 대소문자, 띄어쓰기 무시하고 유사도 판단
               - 유사 기술 인정: Spring과 Spring Boot, MySQL과 MariaDB 등
            
            2. 학력 매칭 (20점):
               - 공고의 학력 요구사항과 지원자의 최종 학력 비교
               - 전공 일치도 (관련 전공인지)
            
            3. 경력 매칭 (30점):
               - 공고의 경력 요구사항과 지원자의 총 경력 비교
               - 유사 직무 경험 여부
               - 경력 연차 적합성
            
            4. 기타 (10점):
               - 지역 매칭
               - 회사 규모/산업 적합성
            
            최종 점수는 0~100점 사이로 산출하되, 각 항목의 가중치를 고려해주세요.
            
            [응답 형식]
            JSON만 응답해주세요. 다른 설명은 하지 마세요.
            {
              "matchScore": 85,
              "skillMatch": 75,
              "educationMatch": 90,
              "careerMatch": 85,
              "otherMatch": 80,
              "summary": "스킬 매칭도가 높고, 경력이 요구사항에 적합함",
              "strengths": ["Spring Boot 3년 경험", "컴퓨터공학 전공"],
              "weaknesses": ["AWS 경험 부족"]
            }
            """,
            resume.getName() != null ? resume.getName() : "미입력",
            educationStr,
            totalCareer,
            experienceStr,
            skillsStr,
            job.getTitle(),
            job.getCompany(),
            tagsStr,
            job.getLocation(),
            job.getExperience() != null ? job.getExperience() : "무관"
        );
    }

    /**
     * 2233076 13주차 개선: 배치 프롬프트 생성
     */
    private String buildBatchPrompt(ResumeInfo resume, List<JobDto> jobs) {
        String skillsStr = resume.getSkills() != null ? String.join(", ", resume.getSkills()) : "없음";
        String educationStr = formatEducations(resume.getEducations());
        String experienceStr = formatExperiences(resume.getExperiences());
        String totalCareer = calculateTotalCareer(resume.getExperiences());
        
        StringBuilder jobsInfo = new StringBuilder();
        for (int i = 0; i < jobs.size(); i++) {
            JobDto job = jobs.get(i);
            String tagsStr = job.getTags() != null ? String.join(", ", job.getTags()) : "없음";
            jobsInfo.append(String.format("""
                공고 %d:
                - 제목: %s
                - 회사: %s
                - 요구 기술: %s
                - 경력: %s
                - 위치: %s
                
                """, i + 1, job.getTitle(), job.getCompany(), tagsStr, 
                job.getExperience() != null ? job.getExperience() : "무관",
                job.getLocation()));
        }

        return String.format("""
            당신은 채용 전문가입니다. 아래 이력서와 여러 채용공고를 분석하여 각각의 매칭도를 평가해주세요.
            
            [이력서 정보]
            이름: %s
            학력: %s
            총 경력: %s
            경력 상세:
            %s
            
            보유 스킬: %s
            
            [채용공고 목록]
            %s
            
            [평가 기준]
            1. 스킬 매칭 (40점): 오타, 대소문자 무시, 유사 기술 인정
            2. 학력 매칭 (20점): 최종 학력과 전공 일치도
            3. 경력 매칭 (30점): 경력 연차와 직무 경험 적합성
            4. 기타 (10점): 지역, 회사 규모 등
            
            각 공고마다 0~100점 사이의 매칭도 점수 산출
            
            [응답 형식]
            JSON 배열로만 응답해주세요. 다른 설명은 하지 마세요.
            [
              {
                "jobIndex": 1,
                "matchScore": 85,
                "skillMatch": 80,
                "educationMatch": 90,
                "careerMatch": 85,
                "otherMatch": 80,
                "summary": "높은 매칭도"
              },
              {
                "jobIndex": 2,
                "matchScore": 60,
                "skillMatch": 55,
                "educationMatch": 70,
                "careerMatch": 60,
                "otherMatch": 50,
                "summary": "중간 매칭도"
              }
            ]
            """,
            resume.getName() != null ? resume.getName() : "미입력",
            educationStr,
            totalCareer,
            experienceStr,
            skillsStr,
            jobsInfo.toString()
        );
    }

    /**
     * 2233076 13주차 추가: 학력 정보 포맷팅
     */
    private String formatEducations(List<EducationInfo> educations) {
        if (educations == null || educations.isEmpty()) {
            return "미입력";
        }
        
        StringBuilder sb = new StringBuilder();
        for (EducationInfo edu : educations) {
            sb.append(String.format("%s %s %s", 
                edu.getSchoolName() != null ? edu.getSchoolName() : "미입력",
                edu.getMajor() != null ? edu.getMajor() : "",
                edu.getDegree() != null ? edu.getDegree() : ""
            )).append(", ");
        }
        
        return sb.length() > 2 ? sb.substring(0, sb.length() - 2) : "미입력";
    }

    /**
     * 2233076 13주차 추가: 경력 정보 포맷팅
     */
    private String formatExperiences(List<ExperienceInfo> experiences) {
        if (experiences == null || experiences.isEmpty()) {
            return "경력 없음";
        }
        
        StringBuilder sb = new StringBuilder();
        for (ExperienceInfo exp : experiences) {
            String period = formatPeriod(exp.getStartDate(), exp.getEndDate(), exp.getIsCurrent());
            sb.append(String.format("- %s | %s | %s\n",
                exp.getCompanyName() != null ? exp.getCompanyName() : "회사명 미입력",
                exp.getPositionTitle() != null ? exp.getPositionTitle() : "직무 미입력",
                period
            ));
        }
        
        return sb.toString();
    }

    /**
     * 2233076 13주차 추가: 총 경력 계산
     */
    private String calculateTotalCareer(List<ExperienceInfo> experiences) {
        if (experiences == null || experiences.isEmpty()) {
            return "신입";
        }
        
        int totalMonths = 0;
        for (ExperienceInfo exp : experiences) {
            if (exp.getStartDate() != null) {
                LocalDate end = exp.getIsCurrent() != null && exp.getIsCurrent() 
                    ? LocalDate.now() 
                    : (exp.getEndDate() != null ? exp.getEndDate() : LocalDate.now());
                
                Period period = Period.between(exp.getStartDate(), end);
                totalMonths += period.getYears() * 12 + period.getMonths();
            }
        }
        
        int years = totalMonths / 12;
        int months = totalMonths % 12;
        
        if (years == 0 && months == 0) {
            return "신입";
        } else if (years == 0) {
            return months + "개월";
        } else if (months == 0) {
            return years + "년";
        } else {
            return years + "년 " + months + "개월";
        }
    }

    /**
     * 2233076 13주차 추가: 기간 포맷팅
     */
    private String formatPeriod(LocalDate start, LocalDate end, Boolean isCurrent) {
        if (start == null) {
            return "기간 미입력";
        }
        
        String startStr = start.toString();
        String endStr = (isCurrent != null && isCurrent) ? "현재" : 
                       (end != null ? end.toString() : "미입력");
        
        return startStr + " ~ " + endStr;
    }

    /**
     * Gemini API 호출
     */
    private String callGeminiApi(String prompt) throws Exception {
        String url = apiUrl + "?key=" + apiKey;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("contents", List.of(
            Map.of("parts", List.of(
                Map.of("text", prompt)
            ))
        ));

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
        String response = restTemplate.postForObject(url, request, String.class);

        log.debug("Gemini API 응답: {}", response);
        return response;
    }

    /**
     * 2233076 13주차 개선: Gemini 응답 파싱
     */
    private MatchResult parseGeminiResponse(String response) throws Exception {
        JsonNode root = objectMapper.readTree(response);
        
        String text = root.path("candidates")
            .path(0)
            .path("content")
            .path("parts")
            .path(0)
            .path("text")
            .asText();

        log.debug("Gemini 응답 텍스트: {}", text);

        String json = text.replaceAll("```json\\s*|```", "").trim();
        JsonNode result = objectMapper.readTree(json);

        int matchScore = result.path("matchScore").asInt(0);
        int skillMatch = result.path("skillMatch").asInt(0);
        int educationMatch = result.path("educationMatch").asInt(0);
        int careerMatch = result.path("careerMatch").asInt(0);
        int otherMatch = result.path("otherMatch").asInt(0);
        String summary = result.path("summary").asText("");

        List<String> strengths = new ArrayList<>();
        JsonNode strengthsNode = result.path("strengths");
        if (strengthsNode.isArray()) {
            for (JsonNode node : strengthsNode) {
                strengths.add(node.asText());
            }
        }

        List<String> weaknesses = new ArrayList<>();
        JsonNode weaknessesNode = result.path("weaknesses");
        if (weaknessesNode.isArray()) {
            for (JsonNode node : weaknessesNode) {
                weaknesses.add(node.asText());
            }
        }

        return new MatchResult(matchScore, skillMatch, educationMatch, careerMatch, 
                              otherMatch, summary, strengths, weaknesses);
    }

    /**
     * 2233076 13주차 개선: 기본 매칭 (Gemini 사용 불가 시)
     */
    private MatchResult simpleMatch(ResumeInfo resume, JobDto job) {
        List<String> resumeSkills = resume.getSkills() != null ? resume.getSkills() : List.of();
        List<String> jobTags = job.getTags() != null ? job.getTags() : List.of();
        
        if (jobTags.isEmpty() || resumeSkills.isEmpty()) {
            return new MatchResult(0, 0, 0, 0, 0, "스킬 정보 없음", List.of(), List.of());
        }

        int matchedCount = 0;
        for (String tag : jobTags) {
            boolean matched = resumeSkills.stream()
                .anyMatch(s -> s.equalsIgnoreCase(tag));
            if (matched) matchedCount++;
        }

        int skillMatch = (int) Math.round((double) matchedCount / jobTags.size() * 100);
        int matchScore = (int) Math.round(skillMatch * 0.4); // 스킬만 40% 가중치
        
        return new MatchResult(matchScore, skillMatch, 0, 0, 0, "기본 매칭 (스킬만)", 
                              List.of(), List.of());
    }

    /**
     * 2233076 13주차 개선: 배치 응답 파싱
     */
    private List<MatchResult> parseBatchGeminiResponse(String response, List<JobDto> jobs) throws Exception {
        JsonNode root = objectMapper.readTree(response);
        
        String text = root.path("candidates")
            .path(0)
            .path("content")
            .path("parts")
            .path(0)
            .path("text")
            .asText();

        String json = text.replaceAll("```json\\s*|```", "").trim();
        JsonNode results = objectMapper.readTree(json);

        List<MatchResult> matchResults = new ArrayList<>();
        
        for (JsonNode result : results) {
            int jobIndex = result.path("jobIndex").asInt(1) - 1;
            int matchScore = result.path("matchScore").asInt(0);
            int skillMatch = result.path("skillMatch").asInt(0);
            int educationMatch = result.path("educationMatch").asInt(0);
            int careerMatch = result.path("careerMatch").asInt(0);
            int otherMatch = result.path("otherMatch").asInt(0);
            String summary = result.path("summary").asText("");
            
            matchResults.add(new MatchResult(matchScore, skillMatch, educationMatch, 
                                            careerMatch, otherMatch, summary, List.of(), List.of()));
        }
        
        return matchResults;
    }

    /**
     * 2233076 13주차 개선: 종합 이력서 정보
     */
    public static class ResumeInfo {
        private String name;
        private List<String> skills;
        private List<EducationInfo> educations;
        private List<ExperienceInfo> experiences;

        public ResumeInfo() {}

        public ResumeInfo(String name, List<String> skills, 
                         List<EducationInfo> educations, 
                         List<ExperienceInfo> experiences) {
            this.name = name;
            this.skills = skills;
            this.educations = educations;
            this.experiences = experiences;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public List<String> getSkills() { return skills; }
        public void setSkills(List<String> skills) { this.skills = skills; }
        public List<EducationInfo> getEducations() { return educations; }
        public void setEducations(List<EducationInfo> educations) { this.educations = educations; }
        public List<ExperienceInfo> getExperiences() { return experiences; }
        public void setExperiences(List<ExperienceInfo> experiences) { this.experiences = experiences; }
    }

    /**
     * 2233076 13주차 추가: 학력 정보
     */
    public static class EducationInfo {
        private String schoolName;
        private String major;
        private String degree;
        private LocalDate startDate;
        private LocalDate endDate;

        public EducationInfo() {}

        public EducationInfo(String schoolName, String major, String degree, 
                           LocalDate startDate, LocalDate endDate) {
            this.schoolName = schoolName;
            this.major = major;
            this.degree = degree;
            this.startDate = startDate;
            this.endDate = endDate;
        }

        public String getSchoolName() { return schoolName; }
        public void setSchoolName(String schoolName) { this.schoolName = schoolName; }
        public String getMajor() { return major; }
        public void setMajor(String major) { this.major = major; }
        public String getDegree() { return degree; }
        public void setDegree(String degree) { this.degree = degree; }
        public LocalDate getStartDate() { return startDate; }
        public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
        public LocalDate getEndDate() { return endDate; }
        public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    }

    /**
     * 2233076 13주차 추가: 경력 정보
     */
    public static class ExperienceInfo {
        private String companyName;
        private String positionTitle;
        private LocalDate startDate;
        private LocalDate endDate;
        private Boolean isCurrent;
        private String description;

        public ExperienceInfo() {}

        public ExperienceInfo(String companyName, String positionTitle, 
                            LocalDate startDate, LocalDate endDate, 
                            Boolean isCurrent, String description) {
            this.companyName = companyName;
            this.positionTitle = positionTitle;
            this.startDate = startDate;
            this.endDate = endDate;
            this.isCurrent = isCurrent;
            this.description = description;
        }

        public String getCompanyName() { return companyName; }
        public void setCompanyName(String companyName) { this.companyName = companyName; }
        public String getPositionTitle() { return positionTitle; }
        public void setPositionTitle(String positionTitle) { this.positionTitle = positionTitle; }
        public LocalDate getStartDate() { return startDate; }
        public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
        public LocalDate getEndDate() { return endDate; }
        public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
        public Boolean getIsCurrent() { return isCurrent; }
        public void setIsCurrent(Boolean isCurrent) { this.isCurrent = isCurrent; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }

    /**
     * 2233076 13주차 개선: 매칭 결과
     */
    public static class MatchResult {
        private final int matchScore;
        private final int skillMatch;
        private final int educationMatch;
        private final int careerMatch;
        private final int otherMatch;
        private final String summary;
        private final List<String> strengths;
        private final List<String> weaknesses;

        public MatchResult(int matchScore, int skillMatch, int educationMatch, 
                          int careerMatch, int otherMatch, String summary,
                          List<String> strengths, List<String> weaknesses) {
            this.matchScore = matchScore;
            this.skillMatch = skillMatch;
            this.educationMatch = educationMatch;
            this.careerMatch = careerMatch;
            this.otherMatch = otherMatch;
            this.summary = summary;
            this.strengths = strengths;
            this.weaknesses = weaknesses;
        }

        public int getMatchScore() { return matchScore; }
        public int getSkillMatch() { return skillMatch; }
        public int getEducationMatch() { return educationMatch; }
        public int getCareerMatch() { return careerMatch; }
        public int getOtherMatch() { return otherMatch; }
        public String getSummary() { return summary; }
        public List<String> getStrengths() { return strengths; }
        public List<String> getWeaknesses() { return weaknesses; }
    }
}
