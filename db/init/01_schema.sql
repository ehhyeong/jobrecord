-- DROP TABLE IF EXISTS `users`,`resume`,`education`,`experience`,`project`,
--                      `skill`,`resume_skill`,`attachment`,`company`,
--                      `job_posting`,`application`,`job_bookmark`;

-- 세션 설정
SET NAMES utf8mb4;
SET time_zone = '+09:00';

-- USERS
CREATE TABLE IF NOT EXISTS `jobproject_users` (
  `users_id` BIGINT NOT NULL AUTO_INCREMENT,
  `users_email` VARCHAR(255) NOT NULL,
  `users_password_hash` VARCHAR(255) NOT NULL,
  `users_name` VARCHAR(100) NOT NULL,
  `users_phone` VARCHAR(30) DEFAULT NULL,
  `users_created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `users_updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `users_role` ENUM('USER','ADMIN') NOT NULL DEFAULT 'USER',
  `users_status` ENUM('ACTIVE','SUSPENDED') NOT NULL DEFAULT 'ACTIVE',
  `users_last_login` DATETIME DEFAULT NULL,
  PRIMARY KEY (`users_id`),
  UNIQUE KEY `uq_users_email` (`users_email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- RESUME
CREATE TABLE IF NOT EXISTS `jobproject_resume` (
  `resume_id` BIGINT NOT NULL AUTO_INCREMENT,
  `users_id` BIGINT NOT NULL,
  `title` VARCHAR(200) NOT NULL,
  `summary` TEXT NULL,
  `is_public` TINYINT(1) NOT NULL DEFAULT 0,
  `resume_created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `resume_updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`resume_id`),
  KEY `ix_resume_users` (`users_id`),
  CONSTRAINT `fk_resume_to_users`
    FOREIGN KEY (`users_id`)
    REFERENCES `jobproject_users` (`users_id`)
    ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- COMPANY
CREATE TABLE IF NOT EXISTS `jobproject_company` (
  `company_id` BIGINT NOT NULL AUTO_INCREMENT,
  `company_name` VARCHAR(200) NOT NULL,
  `company_domain` VARCHAR(255) DEFAULT NULL,
  `company_description` VARCHAR(255) DEFAULT NULL,
  `company_homepage_url` VARCHAR(255) DEFAULT NULL,
  `company_created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `company_updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`company_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- JOB POSTING
CREATE TABLE IF NOT EXISTS `jobproject_job_posting` (
  `job_posting_id` BIGINT NOT NULL AUTO_INCREMENT,
  `company_id` BIGINT NOT NULL,
  `job_posting_title` VARCHAR(200) DEFAULT NULL,
  `job_posting_employment_type` ENUM('FULL_TIME','PART_TIME','CONTRACT','TEMPORARY','OTHER') NOT NULL DEFAULT 'FULL_TIME',
  `job_posting_location` VARCHAR(200) DEFAULT NULL,
  `job_posting_description` MEDIUMTEXT NOT NULL,
  `job_posting_is_active` TINYINT(1) NOT NULL DEFAULT 1,
  `job_posting_posted_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `job_posting_closed_at` DATETIME DEFAULT NULL,
  `job_posting_created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `job_posting_updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`job_posting_id`),
  KEY `ix_posting_company` (`company_id`),
  CONSTRAINT `fk_job_posting_to_company`
    FOREIGN KEY (`company_id`)
    REFERENCES `jobproject_company` (`company_id`)
    ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- EDUCATION (section)
CREATE TABLE IF NOT EXISTS `jobproject_education` (
  `education_id` BIGINT NOT NULL AUTO_INCREMENT,
  `resume_id` BIGINT NOT NULL,
  `education_school_name` VARCHAR(200) NOT NULL,
  `education_major` VARCHAR(200) DEFAULT NULL,
  `education_degree` VARCHAR(100) DEFAULT NULL,
  `education_start_date` DATE DEFAULT NULL,
  `education_end_date` DATE DEFAULT NULL,
  `education_is_current` TINYINT(1) NOT NULL DEFAULT 0,
  `education_created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `education_updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`education_id`),
  KEY `ix_edu_resume` (`resume_id`),
  CONSTRAINT `fk_education_to_resume`
    FOREIGN KEY (`resume_id`)
    REFERENCES `jobproject_resume` (`resume_id`)
    ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- EXPERIENCE (section)
CREATE TABLE IF NOT EXISTS `jobproject_experience` (
  `experience_id` BIGINT NOT NULL AUTO_INCREMENT,
  `resume_id` BIGINT NOT NULL,
  `experience_company_name` VARCHAR(200) NOT NULL,
  `experience_position_title` VARCHAR(200) DEFAULT NULL,
  `experience_start_date` DATE DEFAULT NULL,
  `experience_end_date` DATE DEFAULT NULL,
  `experience_is_current` TINYINT(1) NOT NULL DEFAULT 0,
  `experience_description` MEDIUMTEXT,
  `experience_created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `experience_updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`experience_id`),
  KEY `ix_exp_resume` (`resume_id`),
  CONSTRAINT `fk_experience_to_resume`
    FOREIGN KEY (`resume_id`)
    REFERENCES `jobproject_resume` (`resume_id`)
    ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- SKILL (master)
CREATE TABLE IF NOT EXISTS `jobproject_skill` (
  `skill_id` BIGINT NOT NULL AUTO_INCREMENT,
  `skill_name` VARCHAR(100) NOT NULL,
  `skill_created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `skill_updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`skill_id`),
  UNIQUE KEY `uq_skill_name` (`skill_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- RESUME_SKILL (mapping)
CREATE TABLE IF NOT EXISTS `jobproject_resume_skill` (
  `resume_id` BIGINT NOT NULL,
  `skill_id` BIGINT NOT NULL,
  `proficiency` TINYINT DEFAULT NULL, -- 0~100 권장
  `resume_skill_created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `resume_skill_updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`resume_id`, `skill_id`),
  KEY `ix_rs_skill` (`skill_id`),
  CONSTRAINT `fk_resume_skill_to_resume`
    FOREIGN KEY (`resume_id`)
    REFERENCES `jobproject_resume` (`resume_id`)
    ON DELETE CASCADE,
  CONSTRAINT `fk_resume_skill_to_skill`
    FOREIGN KEY (`skill_id`)
    REFERENCES `jobproject_skill` (`skill_id`)
    ON DELETE CASCADE,
  CONSTRAINT `ck_rs_proficiency` CHECK (`proficiency` BETWEEN 0 AND 100)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- APPLICATION
CREATE TABLE IF NOT EXISTS `jobproject_application` (
  `application_id` BIGINT NOT NULL AUTO_INCREMENT,
  `users_id` BIGINT NOT NULL,
  `job_posting_id` BIGINT NOT NULL,
  `status` ENUM('APPLIED','UNDER_REVIEW','INTERVIEW','OFFER','REJECTED','WITHDRAWN') NOT NULL DEFAULT 'APPLIED',
  `application_created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `application_updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`application_id`),
  KEY `ix_app_users` (`users_id`),
  KEY `ix_app_posting` (`job_posting_id`),
  CONSTRAINT `fk_application_to_users`
    FOREIGN KEY (`users_id`)
    REFERENCES `jobproject_users` (`users_id`)
    ON DELETE CASCADE,
  CONSTRAINT `fk_application_to_job_posting`
    FOREIGN KEY (`job_posting_id`)
    REFERENCES `jobproject_job_posting` (`job_posting_id`)
    ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- JOB BOOKMARK
CREATE TABLE IF NOT EXISTS `jobproject_job_bookmark` (
  `users_id` BIGINT NOT NULL,
  `job_posting_id` BIGINT NOT NULL,
  `job_bookmark_created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`users_id`, `job_posting_id`),
  KEY `ix_jb_users` (`users_id`),
  KEY `ix_jb_posting` (`job_posting_id`),
  CONSTRAINT `fk_job_bookmark_to_users`
    FOREIGN KEY (`users_id`)
    REFERENCES `jobproject_users` (`users_id`)
    ON DELETE CASCADE,
  CONSTRAINT `fk_job_bookmark_to_job_posting`
    FOREIGN KEY (`job_posting_id`)
    REFERENCES `jobproject_job_posting` (`job_posting_id`)
    ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- PROJECT (section)
CREATE TABLE IF NOT EXISTS `jobproject_project` (
  `project_id` BIGINT NOT NULL AUTO_INCREMENT,
  `resume_id` BIGINT NOT NULL,
  `project_name` VARCHAR(200) NOT NULL,
  `project_role` VARCHAR(200) DEFAULT NULL,
  `project_start_date` DATE DEFAULT NULL,
  `project_end_date` DATE DEFAULT NULL,
  `project_is_current` TINYINT(1) NOT NULL DEFAULT 0,
  `project_summary` TEXT,
  `project_tech_stack` VARCHAR(255) DEFAULT NULL,
  `project_url` VARCHAR(255) DEFAULT NULL,
  `project_created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `project_updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`project_id`),
  KEY `ix_project_resume` (`resume_id`),
  CONSTRAINT `fk_project_to_resume`
    FOREIGN KEY (`resume_id`) REFERENCES `jobproject_resume` (`resume_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ATTACHMENT (메타)
CREATE TABLE IF NOT EXISTS `jobproject_attachment` (
  `attachment_id` BIGINT NOT NULL AUTO_INCREMENT,
  `resume_id` BIGINT NOT NULL,
  `filename` VARCHAR(200) NOT NULL,
  `content_type` VARCHAR(100) NOT NULL,
  `size_bytes` BIGINT NOT NULL,
  `storage_key` VARCHAR(300) NOT NULL,     -- s3://bucket/key 또는 로컬 경로
  `is_profile_image` TINYINT(1) NOT NULL DEFAULT 0,
  `attachment_created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `attachment_updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`attachment_id`),
  KEY `ix_attachment_resume` (`resume_id`),
  CONSTRAINT `fk_attachment_to_resume`
    FOREIGN KEY (`resume_id`) REFERENCES `jobproject_resume` (`resume_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 호환용 VIEW (비접두 이름 → 접두 테이블 매핑)
--     CREATE OR REPLACE VIEW 사용: 기존 뷰가 있으면 교체
CREATE OR REPLACE VIEW `users`        AS SELECT * FROM `jobproject_users`;
CREATE OR REPLACE VIEW `resume`       AS SELECT * FROM `jobproject_resume`;
CREATE OR REPLACE VIEW `education`    AS SELECT * FROM `jobproject_education`;
CREATE OR REPLACE VIEW `experience`   AS SELECT * FROM `jobproject_experience`;
CREATE OR REPLACE VIEW `project`      AS SELECT * FROM `jobproject_project`;
CREATE OR REPLACE VIEW `skill`        AS SELECT * FROM `jobproject_skill`;
CREATE OR REPLACE VIEW `resume_skill` AS SELECT * FROM `jobproject_resume_skill`;
CREATE OR REPLACE VIEW `attachment`   AS SELECT * FROM `jobproject_attachment`;
CREATE OR REPLACE VIEW `company`      AS SELECT * FROM `jobproject_company`;
CREATE OR REPLACE VIEW `job_posting`  AS SELECT * FROM `jobproject_job_posting`;
CREATE OR REPLACE VIEW `application`  AS SELECT * FROM `jobproject_application`;
CREATE OR REPLACE VIEW `job_bookmark` AS SELECT * FROM `jobproject_job_bookmark`;
