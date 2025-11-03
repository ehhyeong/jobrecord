package com.jobproj.api.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtTokenProvider jwtTokenProvider;
  private final CustomUserDetailsService customUserDetailsService;

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    String path = request.getRequestURI();

    // Swagger/Docs/Actuator + 로그인/회원가입은 필터 통과(토큰 검사 안함)
    if (path.startsWith("/v3/api-docs")
        || path.startsWith("/swagger-ui")
        || path.startsWith("/swagger-ui.html")
        || path.startsWith("/webjars")
        || path.startsWith("/api/actuator")
        || path.startsWith("/auth/")) {
      filterChain.doFilter(request, response);
      return;
    }

    String token = jwtTokenProvider.resolveToken(request);
    if (token != null && jwtTokenProvider.validateToken(token)) {
      String email = jwtTokenProvider.getEmail(token);
      UserDetails userDetails = customUserDetailsService.loadUserByUsername(email);
      Authentication authentication =
          new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
      // 요청 기반 세부정보 부여(감사/추적에 유용)
      ((UsernamePasswordAuthenticationToken) authentication)
          .setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
      SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    filterChain.doFilter(request, response);
  }
}
