
SET @PW_HASH = '$2a$10$DV.cT754FI13bue64jWFDuW/FCiC0qhsKIVICVBrBu6xav5BMaNeO';
-- 'test1234'를 Bcrypt로 해싱한 값.
-- 9주차 추가(2233076)


-- 1. ADMIN 계정 (ID: 1)
-- (로그인: admin@test.com / test1234)
INSERT INTO jobproject_users (users_email, users_password_hash, users_name, users_role, users_status)
VALUES ('admin@test.com', @PW_HASH, '관리자', 'ADMIN', 'ACTIVE');

-- 2. USER 계정 (ID: 2)
-- (로그인: user@test.com / test1234)
INSERT INTO jobproject_users (users_email, users_password_hash, users_name, users_role, users_status)
VALUES ('user@test.com', @PW_HASH, '일반사용자', 'USER', 'ACTIVE');

-- 3. 샘플 이력서 (USER 계정(ID: 2) 소유)
INSERT INTO jobproject_resume (users_id, title, summary, is_public)
VALUES (2, '샘플 이력서', '시드 데이터 테스트 완료', 1);