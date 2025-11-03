package com.jobproj.api.config;

import com.jobproj.api.security.JwtAuthenticationFilter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

  private final JwtAuthenticationFilter jwtAuthenticationFilter;

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http.csrf(csrf -> csrf.disable())
        .cors(cors -> {}) // CorsConfigurationSource 빈을 쓰도록 활성화
        .authorizeHttpRequests(auth -> auth
            // 스웨거/문서
            .requestMatchers("/v3/api-docs/**", "/swagger-ui/**",
                             "/swagger-ui.html", "/webjars/**").permitAll()
            // 헬스체크
            .requestMatchers("/api/actuator/**").permitAll()
            // 로그인/회원가입/리프레시/로그아웃은 토큰 없이 허용
            .requestMatchers("/auth/login", "/auth/signup").permitAll()
            // refresh/logout 명시 허용
            .requestMatchers("/auth/refresh", "/auth/logout").permitAll()
            // 프리플라이트(브라우저 OPTIONS)
            .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
            //  - 기본은 인증 필요. 공개 다운로드로 바꾸려면 `.permitAll()`로 변경.
            .requestMatchers(HttpMethod.GET,
                "/attachments/*/download", "/api/attachments/*/download")
            .authenticated()
            // 업로드는 인증 필요하게 보호하려면 아래 주석 해제
            // .requestMatchers(HttpMethod.POST, "/attachments", "/api/attachments").authenticated()
            // 관리자 전용 API가 있다면 예시처럼 보호
            // .requestMatchers("/admin/**").hasRole("ADMIN")
            // 나머지는 인증 필요
            .anyRequest().authenticated())
        .sessionManagement(session ->
            session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        // 인증/인가 실패시 JSON 바디로 401/403 내려주기 (수정)
        // 여기 entry point는 "AuthenticationException"일 때만 작동 -> 비인증 예외를 401로 덮어쓰지 않음 (설명 추가)
        .exceptionHandling(ex -> ex
            .authenticationEntryPoint((req, res, e) -> { // 401
              res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
              res.setContentType("application/json;charset=UTF-8");
              res.getWriter().write("""
                  {"errorCode":"A001","message":"인증이 필요합니다.","status":401}
                  """);
            })
            .accessDeniedHandler((req, res, e) -> { // 403
              res.setStatus(HttpServletResponse.SC_FORBIDDEN);
              res.setContentType("application/json;charset=UTF-8");
              res.getWriter().write("""
                  {"errorCode":"A002","message":"권한이 없습니다.","status":403}
                  """);
            }))
        // JWT 필터 체인에 등록
        .addFilterBefore(jwtAuthenticationFilter,
            UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }

  /** 개발용 CORS 설정 */
  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration cfg = new CorsConfiguration();
    cfg.setAllowedOrigins(List.of(
        "http://localhost:3000",
        "http://127.0.0.1:3000",
        "http://localhost:5173",
        "http://127.0.0.1:5173"));
    cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    cfg.setAllowedHeaders(List.of("*"));
    cfg.setExposedHeaders(List.of("Authorization", "Location", "Content-Disposition"));
    cfg.setAllowCredentials(true);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", cfg);
    return source;
  }
}
