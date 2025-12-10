package com.jobproj.api.service;

import com.jobproj.api.domain.Role;
import com.jobproj.api.dto.LoginResponse;
import com.jobproj.api.repo.UserRepo;
import com.jobproj.api.repo.UserRepo.UserRow;
import com.jobproj.api.security.JwtTokenProvider;
import java.security.SecureRandom;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    // 의존성 주입 ---
    private final EmailService emailService;
    private final RedisTemplate<String, String> redisTemplate;

    private final UserRepo userRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    // 인증번호 Redis 키 접두사 및 유효 시간 (3분)
    private static final String RESET_CODE_PREFIX = "RESET_CODE:";
    private static final long CODE_TTL_MINUTES = 3;

    @Value("${jwt.expiration-ms:3600000}")
    private long accessTokenTtlMs;

    /** 로그인 */
    public LoginResponse login(String email, String password) {
        UserRow user = userRepo.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("invalid credentials"));
        if (!passwordEncoder.matches(password, user.pwdHash)) {
            throw new BadCredentialsException("invalid credentials");
        }
        String token = jwtTokenProvider.createAccessToken(user);
        long expiresIn = accessTokenTtlMs;
        return LoginResponse.of(token, expiresIn);
    }

    // 이메일 중복체크 : 회원가입 전 이메일 사용 여부 확인 서비스
    @Transactional(readOnly = true)
    public boolean isEmailDuplicate(String email) {
        return userRepo.existsByEmail(email);
    }

    // 🔽 전화번호 중복체크 : 회원가입 전 전화번호 사용 여부 확인 서비스
    @Transactional(readOnly = true)
    public boolean isPhoneDuplicate(String phone) {
        return userRepo.existsByPhone(phone);
    }
    /** 회원가입 */
    @Transactional
    public void signup(String email, String rawPassword, String name, String phone) { // 🔽 phone 추가
        // 기존의 userRepo.existsByEmail(email) 호출을 isEmailDuplicate 재사용으로 변경
        if (isEmailDuplicate(email)) {
            throw new IllegalStateException("이미 사용 중인 이메일입니다.");
        }
        String encodedPassword = passwordEncoder.encode(rawPassword);
        Role role = Role.USER;
        // 🔽 전화번호까지 함께 저장
        userRepo.save(email, encodedPassword, name, phone, role);
    }

    // =======================================================
    // 비밀번호 찾기 기능
    // =======================================================

    /**
     * 1. 비밀번호 재설정 인증번호 발송 (API 1)
     */
    public void sendPasswordResetCode(String email) {
        // 1. 가입된 유저인지 확인
        if (!userRepo.existsByEmail(email)) {
            // (보안) 가입되지 않은 이메일이라도, "성공"처럼 응답해야
            // 이메일 스캔 공격(Enumeration Attack)을 방지할 수 있습니다.
            log.warn("EmailService: 존재하지 않는 이메일에 대한 비밀번호 재설정 시도: {}", email);
            return; // 에러를 던지지 않고 그냥 리턴
        }

        // 2. 6자리 인증번호 생성
        String code = createRandomCode();
        String redisKey = RESET_CODE_PREFIX + email;

        // 3. Redis에 인증번호 저장 (3분 유효)
        redisTemplate.opsForValue().set(redisKey, code, CODE_TTL_MINUTES, TimeUnit.MINUTES);

        // 4. 이메일 발송 (비동기)
        String subject = "[JobRecord] 비밀번호 재설정 인증번호";
        String text = "인증번호: " + code + "\n\n(유효 시간: 3분)";
        emailService.sendEmail(email, subject, text);

        log.info("EmailService: {}님에게 인증번호 발송", email);
    }

    // 비밀번호 재설정 : 인증번호 검증 공통 로직
    private void validateResetCodeOrThrow(String email, String code) {
        String redisKey = RESET_CODE_PREFIX + email;
        String storedCode = redisTemplate.opsForValue().get(redisKey);

        if (storedCode == null) {
            throw new IllegalStateException("인증번호가 만료되었거나 유효하지 않습니다.");
        }
        if (!storedCode.equals(code)) {
            throw new IllegalStateException("인증번호가 일치하지 않습니다.");
        }
    }

    // 비밀번호 재설정 : 인증번호만 먼저 확인하는 서비스
    @Transactional(readOnly = true)
    public void verifyPasswordResetCode(
                                             String email, String code
    ) {
        validateResetCodeOrThrow(email, code);
    }

    /**
     * 2. 비밀번호 재설정 (API 2)
     */
    @Transactional
    public void resetPassword(String email, String code, String newPassword) {
        // 비밀번호 재설정 : 공통 검증 메서드 재사용
        validateResetCodeOrThrow(email, code);
        String redisKey = RESET_CODE_PREFIX + email;

        // 3. (검증 성공) 새 비밀번호 암호화 및 DB 업데이트
        String encodedPassword = passwordEncoder.encode(newPassword);
        int updatedRows = userRepo.updatePasswordByEmail(email, encodedPassword);

        if (updatedRows == 0) {
            // (혹시 모를 동시성 문제) 인증은 성공했으나, 그 사이 유저가 탈퇴한 경우
            log.warn("UserService: 비밀번호 재설정 대상 유저를 찾지 못함: {}", email);
            throw new IllegalStateException("사용자 정보를 찾을 수 없습니다.");
        }

        // 4. (성공) Redis에서 인증번호 삭제 (재사용 방지)
        redisTemplate.delete(redisKey);
        log.info("UserService: {}님의 비밀번호가 재설정되었습니다.", email);
    }

    /** 6자리 숫자 인증번호 생성 헬퍼 */
    private String createRandomCode() {
        Random random = new SecureRandom();
        int code = 100000 + random.nextInt(900000); // 100000 ~ 999999
        return String.valueOf(code);
    }

    public UserRow loadUserRowByEmail(String email) {
        return userRepo.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("사용자를 찾을 수 없습니다."));
    }
    // =======================================================
    // 2233076 12주차 추가: 회원 탈퇴 로직
    // =======================================================
    @Transactional
    public void withdraw(String email, String password) {
        // 1. 현재 사용자 정보 가져오기
        UserRepo.UserRow user = loadUserRowByEmail(email);

        // 2. 비밀번호 검증
        if (!passwordEncoder.matches(password, user.pwdHash)) {
            throw new BadCredentialsException("비밀번호가 일치하지 않습니다.");
        }

        // 3. DB에서 삭제
        userRepo.deleteByEmail(email);
    }
    public long getAccessTokenTtlMs() {
        return accessTokenTtlMs;
    }
}
