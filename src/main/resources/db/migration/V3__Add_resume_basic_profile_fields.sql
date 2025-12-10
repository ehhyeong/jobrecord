-- 융합프로젝트 김태형 12주차 : 이력서 기본 프로필 정보 컬럼 추가
--  - 이름 / 전화번호 / 이메일 / 생년월일을 jobproject_resume 테이블에 저장
--  - NULL 허용 (기존 데이터와 호환)

ALTER TABLE jobproject_resume
  ADD COLUMN resume_full_name   VARCHAR(100)  NULL AFTER users_id,
  ADD COLUMN resume_phone       VARCHAR(30)   NULL AFTER resume_full_name,
  ADD COLUMN resume_email       VARCHAR(255)  NULL AFTER resume_phone,
  ADD COLUMN resume_birth_date  DATE          NULL AFTER resume_email;

-- 융합프로젝트 김태형 12주차 : 호환용 VIEW 갱신
--  V1에서 만들어 둔 `resume` 뷰는 SELECT * 를 사용하고 있어서
--  테이블 구조가 바뀐 뒤 다시 재생성해 주면 새 컬럼까지 포함된다.:contentReference[oaicite:1]{index=1}
DROP VIEW IF EXISTS `resume`;

CREATE OR REPLACE VIEW `resume` AS
SELECT *
FROM `jobproject_resume`;
