package com.jobproj.api.resume;

import com.jobproj.api.common.JdbcUtils;
import com.jobproj.api.common.PageRequest;
import com.jobproj.api.dto.ResumeDto;
import com.jobproj.api.dto.ResumeDto.CreateRequest;
import com.jobproj.api.dto.ResumeDto.Response;
import com.jobproj.api.dto.ResumeDto.UpdateRequest;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class ResumeRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public ResumeRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // 2233076 12주차 수정: 템플릿 ID 포함
    // 이력서 + 기본정보 + 프로필사진 + 템플릿ID까지 한 번에 매핑
    private static final RowMapper<Response> MAPPER =
            (ResultSet rs, int i) ->
                    new Response(
                            rs.getLong("resume_id"),
                            rs.getLong("users_id"),
                            rs.getString("title"),
                            rs.getString("summary"),
                            rs.getInt("is_public") == 1,
                            rs.getTimestamp("resume_created_at").toLocalDateTime(),
                            rs.getTimestamp("resume_updated_at").toLocalDateTime(),
                            rs.getString("resume_full_name"),
                            rs.getString("resume_phone"),
                            rs.getString("resume_email"),
                            rs.getDate("resume_birth_date") != null
                                    ? rs.getDate("resume_birth_date").toLocalDate()
                                    : null,
                            rs.getString("resume_profile_image_url"),
                            rs.getObject("template_id") != null 
                                    ? rs.getInt("template_id") 
                                    : 1  // 기본값 1
                    );

    // 2233076 12주차 수정: 템플릿 ID 포함
    // 생성 (JWT에서 받은 usersId 주입)
    public Long create(Long usersId, CreateRequest req) {
        String sql =
                """
                INSERT INTO jobproject_resume
                  (users_id,
                   title,
                   summary,
                   is_public,
                   resume_created_at,
                   resume_updated_at,
                   resume_full_name,
                   resume_phone,
                   resume_email,
                   resume_birth_date,
                   template_id)
                VALUES (:usersId,
                        :title,
                        :summary,
                        :isPublic,
                        :now,
                        :now,
                        :name,
                        :phone,
                        :email,
                        :birthDate,
                        :templateId)
                """;

        // templateId가 null이거나 1-6 범위 밖이면 1로 보정
        Integer templateId = req.templateId;
        if (templateId == null || templateId < 1 || templateId > 6) {
            templateId = 1;
        }

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("usersId", usersId)
                .addValue("title", req.title)
                .addValue("summary", req.summary)
                .addValue("isPublic", Boolean.TRUE.equals(req.isPublic) ? 1 : 0)
                .addValue("now", Timestamp.valueOf(LocalDateTime.now()))
                .addValue("name", req.name)
                .addValue("phone", req.phone)
                .addValue("email", req.email)
                .addValue("birthDate", req.birthDate)
                .addValue("templateId", templateId);

        GeneratedKeyHolder kh = new GeneratedKeyHolder();
        jdbc.update(sql, params, kh, new String[]{"resume_id"});
        return kh.getKey().longValue();
    }

    // 2233076 12주차 수정: 템플릿 ID 포함
    // 단건 조회 (소유권 검증 없음)
    public Optional<Response> findById(Long id) {
        try {
            String sql =
                    """
                    SELECT resume_id,
                           users_id,
                           title,
                           summary,
                           is_public,
                           resume_created_at,
                           resume_updated_at,
                           resume_full_name,
                           resume_phone,
                           resume_email,
                           resume_birth_date,
                           resume_profile_image_url,
                           template_id
                      FROM jobproject_resume
                     WHERE resume_id = :id
                    """;
            return Optional.ofNullable(
                    jdbc.queryForObject(sql, Map.of("id", id), MAPPER)
            );
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    // 2233076 12주차 수정: 템플릿 ID 포함
    // 소유자 조건 포함 단건 조회
    public Optional<Response> findByIdAndUsersId(Long id, Long usersId) {
        try {
            String sql =
                    """
                    SELECT resume_id,
                           users_id,
                           title,
                           summary,
                           is_public,
                           resume_created_at,
                           resume_updated_at,
                           resume_full_name,
                           resume_phone,
                           resume_email,
                           resume_birth_date,
                           resume_profile_image_url,
                           template_id
                      FROM jobproject_resume
                     WHERE resume_id = :id
                       AND users_id = :usersId
                    """;

            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("id", id)
                    .addValue("usersId", usersId);

            return Optional.ofNullable(jdbc.queryForObject(sql, params, MAPPER));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    // owner 빠른 확인 (404 / 403 분기용)
    public Optional<Long> findOwnerId(Long resumeId) {
        try {
            String sql =
                    "SELECT users_id FROM jobproject_resume WHERE resume_id = :id";
            Long uid = jdbc.queryForObject(sql, Map.of("id", resumeId), Long.class);
            return Optional.ofNullable(uid);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    // 2233076 12주차 수정: 템플릿 ID 포함
    // 기존 update (소유권 미검증)
    public int update(Long id, UpdateRequest req) {
        // templateId가 null이거나 1-6 범위 밖이면 1로 보정
        Integer templateId = req.templateId;
        if (templateId == null || templateId < 1 || templateId > 6) {
            templateId = 1;
        }

        String sql =
                """
                UPDATE jobproject_resume
                   SET title = :title,
                       summary = :summary,
                       is_public = :isPublic,
                       resume_updated_at = :now,
                       resume_full_name = :name,
                       resume_phone = :phone,
                       resume_email = :email,
                       resume_birth_date = :birthDate,
                       template_id = :templateId
                 WHERE resume_id = :id
                """;

        return jdbc.update(
                sql,
                new MapSqlParameterSource()
                        .addValue("title", req.title)
                        .addValue("summary", req.summary)
                        .addValue("isPublic", Boolean.TRUE.equals(req.isPublic) ? 1 : 0)
                        .addValue("now", Timestamp.valueOf(LocalDateTime.now()))
                        .addValue("name", req.name)
                        .addValue("phone", req.phone)
                        .addValue("email", req.email)
                        .addValue("birthDate", req.birthDate)
                        .addValue("templateId", templateId)
                        .addValue("id", id)
        );
    }

    // 2233076 12주차 수정: 템플릿 ID 포함
    // 소유자 조건 포함 업데이트
    public int updateByOwner(Long id, Long usersId, UpdateRequest req) {
        // templateId가 null이거나 1-6 범위 밖이면 1로 보정
        Integer templateId = req.templateId;
        if (templateId == null || templateId < 1 || templateId > 6) {
            templateId = 1;
        }

        String sql =
                """
                UPDATE jobproject_resume
                   SET title = :title,
                       summary = :summary,
                       is_public = :isPublic,
                       resume_updated_at = :now,
                       resume_full_name = :name,
                       resume_phone = :phone,
                       resume_email = :email,
                       resume_birth_date = :birthDate,
                       template_id = :templateId
                 WHERE resume_id = :id
                   AND users_id = :usersId
                """;

        return jdbc.update(
                sql,
                new MapSqlParameterSource()
                        .addValue("title", req.title)
                        .addValue("summary", req.summary)
                        .addValue("isPublic", Boolean.TRUE.equals(req.isPublic) ? 1 : 0)
                        .addValue("now", Timestamp.valueOf(LocalDateTime.now()))
                        .addValue("name", req.name)
                        .addValue("phone", req.phone)
                        .addValue("email", req.email)
                        .addValue("birthDate", req.birthDate)
                        .addValue("templateId", templateId)
                        .addValue("id", id)
                        .addValue("usersId", usersId)
        );
    }

    // 프로필 사진 URL만 소유자 조건으로 업데이트
    public int updateProfileImageUrlByOwner(Long id, Long usersId, String profileImageUrl) {
        String sql =
                """
                UPDATE jobproject_resume
                   SET resume_profile_image_url = :profileImageUrl,
                       resume_updated_at        = :now
                 WHERE resume_id = :id
                   AND users_id  = :usersId
                """;

        return jdbc.update(
                sql,
                new MapSqlParameterSource()
                        .addValue("profileImageUrl", profileImageUrl)
                        .addValue("now", Timestamp.valueOf(LocalDateTime.now()))
                        .addValue("id", id)
                        .addValue("usersId", usersId)
        );
    }

    // 삭제
    public int delete(Long id) {
        String sql = "DELETE FROM jobproject_resume WHERE resume_id = :id";
        return jdbc.update(sql, Map.of("id", id));
    }

    // 소유자 조건 포함 삭제
    public int deleteByOwner(Long id, Long usersId) {
        String sql =
                "DELETE FROM jobproject_resume " +
                        " WHERE resume_id = :id AND users_id = :usersId";

        return jdbc.update(
                sql,
                new MapSqlParameterSource()
                        .addValue("id", id)
                        .addValue("usersId", usersId)
        );
    }

    // 2233076 12주차 수정: 템플릿 ID 포함
    // 목록 조회
    public List<Response> search(PageRequest pr, Long usersId, String keyword) {
        Map<String, String> sortMap =
                Map.of(
                        "created_at", "resume_created_at",
                        "updated_at", "resume_updated_at",
                        "title", "title"
                );

        String where = " WHERE users_id = :usersId ";
        where += JdbcUtils.whereLike(keyword, "title", "summary");

        String sql =
                """
                SELECT resume_id,
                       users_id,
                       title,
                       summary,
                       is_public,
                       resume_created_at,
                       resume_updated_at,
                       resume_full_name,
                       resume_phone,
                       resume_email,
                       resume_birth_date,
                       resume_profile_image_url,
                       template_id
                  FROM jobproject_resume
                """
                        + where
                        + JdbcUtils.orderBy(pr.getSort(), sortMap)
                        + " LIMIT :limit OFFSET :offset";

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("usersId", usersId)
                .addValue(
                        "kw",
                        (keyword == null || keyword.isBlank())
                                ? null
                                : "%" + keyword + "%"
                )
                .addValue("limit", pr.getSize())
                .addValue("offset", pr.offset());

        return jdbc.query(sql, params, MAPPER);
    }

    public long count(Long usersId, String keyword) {
        String where = " WHERE users_id = :usersId ";
        where += JdbcUtils.whereLike(keyword, "title", "summary");

        String sql = "SELECT COUNT(*) FROM jobproject_resume " + where;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("usersId", usersId)
                .addValue(
                        "kw",
                        (keyword == null || keyword.isBlank())
                                ? null
                                : "%" + keyword + "%"
                );

        return jdbc.queryForObject(sql, params, Long.class);
    }
}
