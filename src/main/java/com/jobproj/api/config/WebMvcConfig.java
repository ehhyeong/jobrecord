package com.jobproj.api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

  @Override
  public void addViewControllers(ViewControllerRegistry registry) {
    registry.addViewController("/auth/login").setViewName("auth/login");
    registry.addViewController("/auth/signup").setViewName("auth/signup");
    registry.addViewController("/mypage").setViewName("user_page/mypage");
    registry.addViewController("/resume-job-recommendations").setViewName("job/resume-job-recommendations");

    // 옛 URL 대응용 (필요 없으면 지워도 됨)
    registry.addViewController("/Make").setViewName("resume/Make");

    // /resume/edit 매핑은 ResumePageController에서 처리하므로 여기서는 제거
    // registry.addViewController("/resume/edit").setViewName("resume/resume-edit");
  }

  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    // 실행 디렉토리 기준 uploads 폴더 절대 경로
    String rootDir = System.getProperty("user.dir").replace("\\", "/");
    String uploadsLocation = "file:" + rootDir + "/uploads/";

    registry
        .addResourceHandler("/uploads/**")
        .addResourceLocations(uploadsLocation);
  }
}
