package com.jobproj.api.config;

import com.jobproj.api.security.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
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
        .cors(cors -> {}) // 아래 CorsConfigurationSource 빈만 사용
        .authorizeHttpRequests(auth -> auth
            // Swagger
            .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/webjars/**").permitAll()
            // Actuator (health)
            .requestMatchers("/api/actuator/**").permitAll()
            // Auth (login/signup/refresh/logout)
            .requestMatchers("/auth/**").permitAll()
            // Jobs 더미 (데모 기간 공개)
            .requestMatchers("/jobs/**").permitAll()
            // Preflight
            .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
            // 다운로드는 인증 필요(공개로 바꾸려면 permitAll)
            .requestMatchers(HttpMethod.GET, "/attachments/*/download", "/api/attachments/*/download").authenticated()
            // 그 외는 인증
            .anyRequest().authenticated())
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
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
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }

  /** 쿠키 기반 CORS (credentials + 화이트리스트 고정) */
  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration cfg = new CorsConfiguration();

    // 개발 + (운영 도메인 추가 주석)
    cfg.setAllowedOrigins(List.of(
        "http://localhost:3000",
        "http://127.0.0.1:3000",
        "http://localhost:5173",
        "http://127.0.0.1:5173"
        // "https://front.example.com" // 운영 배포 시 추가
    ));

    cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));

    // 꼭 필요한 헤더만 허용(불가피하면 "*"로 전환 가능)
    cfg.setAllowedHeaders(List.of("Content-Type", "X-Requested-With"));

    // 파일 다운로드 파일명 노출만 필요
    cfg.setExposedHeaders(List.of("Content-Disposition"));

    // 쿠키 인증 핵심
    cfg.setAllowCredentials(true);

    // 프리플라이트 캐시(1시간)
    cfg.setMaxAge(3600L);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", cfg);
    return source;
  }
}
