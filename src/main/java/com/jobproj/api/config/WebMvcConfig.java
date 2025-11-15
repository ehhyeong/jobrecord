package com.jobproj.api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/auth/login").setViewName("auth/login");
        registry.addViewController("/auth/signup").setViewName("auth/signup");
        // 2233076 10주차 추가: 비밀번호 재설정 페이지
        registry.addViewController("/auth/password-reset").setViewName("auth/password-reset");
        // 필요 시 /home.html 직접 접근도 열고 싶다면 아래 추가
        // registry.addViewController("/home.html").setViewName("home");
    }
}
