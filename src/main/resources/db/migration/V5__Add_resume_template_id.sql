-- 2233076 12주차 추가: 이력서 템플릿 ID 저장 기능
-- 이력서 테이블에 template_id 컬럼 추가
-- 사용자가 선택한 템플릿 번호(1-6)를 저장

ALTER TABLE jobproject_resume 
ADD COLUMN template_id INT DEFAULT 1 COMMENT '선택한 템플릿 번호 (1-6)';
