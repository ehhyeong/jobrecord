package com.jobproj.api.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtTokenProvider jwtTokenProvider;
  private final CustomUserDetailsService customUserDetailsService;

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {

    // 1) CORS Preflight는 무조건 통과
    if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
      chain.doFilter(request, response);
      return;
    }

    // 2) 헤더에서 토큰 추출 (없으면 그대로 통과 -> permitAll 경로 열림)
    String token = jwtTokenProvider.resolveToken(request);
    if (token == null || token.isBlank()) {
      chain.doFilter(request, response);
      return;
    }

    // 3) 토큰이 있을 때만 검증 & SecurityContext 설정
    try {
      if (jwtTokenProvider.validateToken(token)) {
        String email = jwtTokenProvider.getEmail(token);
        UserDetails userDetails = customUserDetailsService.loadUserByUsername(email);

        UsernamePasswordAuthenticationToken auth =
            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        SecurityContextHolder.getContext().setAuthentication(auth);
      }
    } catch (Exception ignore) {
      // 유효하지 않은 토큰이면 컨텍스트만 비우고 계속 진행(여기서 401 쓰지 않음)
      SecurityContextHolder.clearContext();
    }

    chain.doFilter(request, response);
  }
}
