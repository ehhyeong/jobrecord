package com.jobproj.api.resume;

import com.jobproj.api.common.JdbcUtils;
import com.jobproj.api.common.PageRequest;
import com.jobproj.api.resume.ResumeDto.CreateRequest;
import com.jobproj.api.resume.ResumeDto.Response;
import com.jobproj.api.resume.ResumeDto.UpdateRequest;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.*;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class ResumeRepository {

  private final NamedParameterJdbcTemplate jdbc;

  public ResumeRepository(NamedParameterJdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  private static final RowMapper<Response> MAPPER =
      (ResultSet rs, int i) ->
          new Response(
              rs.getLong("resume_id"),
              rs.getLong("users_id"),
              rs.getString("title"),
              rs.getString("summary"),
              rs.getInt("is_public") == 1,
              rs.getTimestamp("resume_created_at").toLocalDateTime(),
              rs.getTimestamp("resume_updated_at").toLocalDateTime());

  /** usersId를 인자로 받아 INSERT */
  public Long create(Long usersId, CreateRequest req) {
    String sql =
        """
      INSERT INTO resume (users_id, title, summary, is_public, resume_created_at, resume_updated_at)
      VALUES (:usersId, :title, :summary, :isPublic, :now, :now)
      """;
    var params =
        new MapSqlParameterSource()
            .addValue("usersId", usersId)
            .addValue("title", req.title)
            .addValue("summary", req.summary)
            .addValue("isPublic", Boolean.TRUE.equals(req.isPublic) ? 1 : 0)
            .addValue("now", Timestamp.valueOf(LocalDateTime.now()));
    var kh = new GeneratedKeyHolder();
    jdbc.update(sql, params, kh, new String[] {"resume_id"});
    return kh.getKey().longValue();
  }

  // --- 기존 단건조회(소유권 미검증): 남겨두되 서비스에서는 더이상 직접 사용 X
  public Optional<Response> findById(Long id) {
    try {
      String sql =
          """
        SELECT resume_id, users_id, title, summary, is_public,
               resume_created_at, resume_updated_at
        FROM resume
        WHERE resume_id = :id
        """;
      return Optional.ofNullable(jdbc.queryForObject(sql, Map.of("id", id), MAPPER));
    } catch (EmptyResultDataAccessException e) {
      return Optional.empty();
    }
  }

  // 소유자까지 조건을 포함한 단건 조회
  public Optional<Response> findByIdAndUsersId(Long id, Long usersId) {
    try {
      String sql =
          """
        SELECT resume_id, users_id, title, summary, is_public,
               resume_created_at, resume_updated_at
        FROM resume
        WHERE resume_id = :id AND users_id = :usersId
        """;
      var params = new MapSqlParameterSource()
          .addValue("id", id)
          .addValue("usersId", usersId);
      return Optional.ofNullable(jdbc.queryForObject(sql, params, MAPPER));
    } catch (EmptyResultDataAccessException e) {
      return Optional.empty();
    }
  }

  // owner 빠른 확인(404/403 분기용)
  public Optional<Long> findOwnerId(Long resumeId) {
  try {
    String sql = "SELECT users_id FROM resume WHERE resume_id = :id";
    Long uid = jdbc.queryForObject(sql, Map.of("id", resumeId), Long.class);
    return Optional.ofNullable(uid);
  } catch (EmptyResultDataAccessException e) {
    return Optional.empty();
  }
}

  // --- 기존 update/delete(소유권 미검증): 남겨두되 서비스에서 사용하지 않도록 함
  public int update(Long id, UpdateRequest req) {
    String sql =
        """
      UPDATE resume
         SET title = :title,
             summary = :summary,
             is_public = :isPublic,
             resume_updated_at = :now
       WHERE resume_id = :id
      """;
    return jdbc.update(
        sql,
        new MapSqlParameterSource()
            .addValue("title", req.title)
            .addValue("summary", req.summary)
            .addValue("isPublic", Boolean.TRUE.equals(req.isPublic) ? 1 : 0)
            .addValue("now", Timestamp.valueOf(LocalDateTime.now()))
            .addValue("id", id));
  }

  public int delete(Long id) {
    String sql = "DELETE FROM resume WHERE resume_id = :id";
    return jdbc.update(sql, Map.of("id", id));
  }

  // 소유자 조건 포함 업데이트
  public int updateByOwner(Long id, Long usersId, UpdateRequest req) {
    String sql =
        """
      UPDATE resume
         SET title = :title,
             summary = :summary,
             is_public = :isPublic,
             resume_updated_at = :now
       WHERE resume_id = :id AND users_id = :usersId
      """;
    return jdbc.update(
        sql,
        new MapSqlParameterSource()
            .addValue("title", req.title)
            .addValue("summary", req.summary)
            .addValue("isPublic", Boolean.TRUE.equals(req.isPublic) ? 1 : 0)
            .addValue("now", Timestamp.valueOf(LocalDateTime.now()))
            .addValue("id", id)
            .addValue("usersId", usersId));
  }

  // 소유자 조건 포함 삭제
  public int deleteByOwner(Long id, Long usersId) {
    String sql = "DELETE FROM resume WHERE resume_id = :id AND users_id = :usersId";
    return jdbc.update(sql, new MapSqlParameterSource()
        .addValue("id", id)
        .addValue("usersId", usersId));
  }

  public List<Response> search(PageRequest pr, Long usersId, String keyword) {
    Map<String, String> sortMap =
        Map.of(
            "created_at", "resume_created_at",
            "updated_at", "resume_updated_at",
            "title", "title");
    String where = " WHERE users_id = :usersId ";
    where += JdbcUtils.whereLike(keyword, "title", "summary");

    String sql =
        """
      SELECT resume_id, users_id, title, summary, is_public,
             resume_created_at, resume_updated_at
      FROM resume
      """
            + where
            + JdbcUtils.orderBy(pr.getSort(), sortMap)
            + " LIMIT :limit OFFSET :offset";

    var params =
        new MapSqlParameterSource()
            .addValue("usersId", usersId)
            .addValue("kw", (keyword == null || keyword.isBlank()) ? null : "%" + keyword + "%")
            .addValue("limit", pr.getSize())
            .addValue("offset", pr.offset());

    return jdbc.query(sql, params, MAPPER);
  }

  public long count(Long usersId, String keyword) {
    String where = " WHERE users_id = :usersId ";
    where += JdbcUtils.whereLike(keyword, "title", "summary");
    String sql = "SELECT COUNT(*) FROM resume " + where;
    var params =
        new MapSqlParameterSource()
            .addValue("usersId", usersId)
            .addValue("kw", (keyword == null || keyword.isBlank()) ? null : "%" + keyword + "%");
    return jdbc.queryForObject(sql, params, Long.class);
  }
}
