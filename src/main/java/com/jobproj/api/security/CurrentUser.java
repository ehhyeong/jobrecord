package com.jobproj.api.security;

import com.jobproj.api.repo.UserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CurrentUser {

  private final UserRepo userRepo;

  /** SecurityContext의 principal(=이메일) → DB users_id 로 변환 */
  public Long id() {
    var auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || auth.getName() == null) {
      throw new IllegalStateException("unauthenticated");
    }
    var email = auth.getName();
    return userRepo
        .findIdByEmail(email)
        .orElseThrow(() -> new IllegalStateException("user not found: " + email));
  }

  public String email() {
    var auth = SecurityContextHolder.getContext().getAuthentication();
    return auth != null ? auth.getName() : null;
  }
}
