-- Run after V1__Init_Tables.sql.sql
INSERT INTO jobpoject_users
(users_email, users_password_hash, users_name, users_phone,
 users_role, users_status)
VALUES
('test@example.com',
 '$2b$10$TNFB0jG1fpQvOagQUbun9OaeT3fOHCMPSDUgyFd9PF9ypLzkfCiYy',
 '테스트사용자','010-0000-0000','USER','ACTIVE');
INSERT INTO jobpoject_company (company_name, company_domain,
  company_description, company_hompage_url)
VALUES
('Acme Corp','acme.com','가상의 회사입니다.','https://acme.com'),
('Nexon','nexon.com','게임회사.','https://www.nexon.com');
INSERT INTO jobpoject_job_posting
(company_id, job_posting_title, job_posting_employment_type,
 job_posting_location, job_posting_description, job_posting_is_active)
VALUES
(1,'주니어 백엔드 개발자','FULL_TIME','서울',
 'Spring Boot 사용 경력 1년 이상',1),
(2,'게임 클라이언트 개발자','FULL_TIME','판교',
 'C++/Unity 경험 우대',1);
INSERT INTO jobpoject_resume (users_id, title, summary, is_public)
VALUES (1,'첫 이력서','간단 소개입니다.',1);
