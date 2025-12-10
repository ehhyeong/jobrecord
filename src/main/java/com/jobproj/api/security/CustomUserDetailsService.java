package com.jobproj.api.security;

import com.jobproj.api.repo.UserRepo;
import com.jobproj.api.repo.UserRepo.UserRow;
import java.util.Collections;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User; // Spring Security가 제공하는 User 객체
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/** Spring Security가 사용자를 인증할 때 DB와 연동하기 위한 서비스 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

  private final UserRepo userRepo;

  @Override
  public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
    // 1. UserRepo를 이용해 DB에서 이메일로 사용자를 조회합니다.
    return userRepo
        .findByEmail(email)
        // 2. 찾았다면(ifPresent), UserDetails 객체로 변환합니다.
        .map(this::createUserDetails)
        // 3. 못 찾았다면(orElseThrow), Spring Security에 예외를 던집니다.
        .orElseThrow(() -> new UsernameNotFoundException("DB에서 사용자를 찾을 수 없습니다: " + email));
  }

  // DB에서 찾은 UserRow 객체를 Spring Security가 이해할 수 있는 UserDetails 객체로 변환합니다.
  private UserDetails createUserDetails(UserRow user) {
    // 1. "ROLE_USER", "ROLE_ADMIN" 같은 권한 정보를 GrantedAuthority 객체로 변환합니다.
    GrantedAuthority grantedAuthority = new SimpleGrantedAuthority(user.role.name());
    // 2. Spring Security의 User 객체를 생성하여 반환합니다.
    return new User(
        user.email, // principal (사용자 식별자, 여기서는 이메일)
        user.pwdHash, // credentials (비밀번호)
        Collections.singleton(grantedAuthority) // authorities (권한 목록)
        );
  }
}
