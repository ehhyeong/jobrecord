package com.jobproj.api.config;

import com.jobproj.api.security.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
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
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> {})
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // (static) 공통 정적 리소스 핸들러 (css, js, img)
                        .requestMatchers(PathRequest.toStaticResources().atCommonLocations()).permitAll()
                        // (static) 명시적 정적 경로 허용
                        .requestMatchers("/css/**", "/js/**", "/img/**", "/images/**",
                                "/webjars/**", "/favicon.ico").permitAll()

                        // (templates) WebMvcConfig가 서빙하는 HTML 페이지 경로
                        .requestMatchers("/", "/home", "/home.html", "/error").permitAll()
                        // 2233076 10주차 수정 (WebMvcConfig에 등록된 경로 허용)
                        .requestMatchers("/auth/login", "/auth/signup", "/auth/password-reset").permitAll()

                        // (API) /auth/ API 경로 (POST /auth/login, POST /auth/password-reset/request 등)
                        .requestMatchers("/auth/**").permitAll()

                        // Swagger 경로
                        .requestMatchers("/api-docs/**", "/docs/**").permitAll()

                        // Actuator
                        .requestMatchers("/api/actuator/**", "/actuator/**").permitAll()

                        // Preflight
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // 나머지 보호
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((req, res, e) -> {
                            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            res.setContentType("application/json;charset=UTF-8");
                            res.getWriter().write("{\"errorCode\":\"A001\",\"message\":\"인증이 필요합니다.\",\"status\":401}");
                        })
                        .accessDeniedHandler((req, res, e) -> {
                            res.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            res.setContentType("application/json;charset=UTF-8");
                            res.getWriter().write("{\"errorCode\":\"A002\",\"message\":\"권한이 없습니다.\",\"status\":403}");
                        })
                )
                // JWT 필터
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /** 2233076 10주차: 쿠키/도메인 분리 환경용 CORS */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOrigins(List.of(
                "http://localhost:3000", "http://127.0.0.1:3000",
                "http://localhost:5173", "http://127.0.0.1:5173",
                "http://localhost:8080" // 2233076 10주차 추가 (서버 자체 HTML 테스트용)
        ));
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("Authorization","Content-Type","X-Requested-With","Accept","Origin","Cache-Control","Pragma"));
        cfg.setExposedHeaders(List.of("Content-Disposition"));
        cfg.setAllowCredentials(true);
        cfg.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }
}