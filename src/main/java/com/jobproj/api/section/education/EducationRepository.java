package com.jobproj.api.section.education;

import com.jobproj.api.common.JdbcUtils;
import com.jobproj.api.common.PageRequest;
import com.jobproj.api.section.education.EducationDto.CreateRequest;
import com.jobproj.api.section.education.EducationDto.Response;
import com.jobproj.api.section.education.EducationDto.UpdateRequest;
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
public class EducationRepository {

  private final NamedParameterJdbcTemplate jdbc;

  public EducationRepository(NamedParameterJdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  // --- mapper ---
  private static final RowMapper<Response> MAPPER =
      (ResultSet rs, int i) ->
          new Response(
              rs.getLong("education_id"),
              rs.getLong("resume_id"),
              rs.getString("education_school_name"),
              rs.getString("education_major"),
              rs.getString("education_degree"),
              rs.getDate("education_start_date") != null
                  ? rs.getDate("education_start_date").toLocalDate()
                  : null,
              rs.getDate("education_end_date") != null
                  ? rs.getDate("education_end_date").toLocalDate()
                  : null,
              rs.getInt("education_is_current") == 1,
              rs.getTimestamp("education_created_at").toLocalDateTime(),
              rs.getTimestamp("education_updated_at").toLocalDateTime());

  // --- create ---
  public Long create(CreateRequest r) {
    String sql =
        """
        INSERT INTO jobproject_education
          (resume_id, education_school_name, education_major, education_degree,
           education_start_date, education_end_date, education_is_current,
           education_created_at, education_updated_at)
        VALUES
          (:rid, :school, :major, :degree, :start, :end, :current, :now, :now)
        """;
    var ps =
        new MapSqlParameterSource()
            .addValue("rid", r.resumeId)
            .addValue("school", r.schoolName)
            .addValue("major", r.major)
            .addValue("degree", r.degree)
            .addValue("start", r.startDate)
            .addValue("end", r.endDate)
            .addValue("current", Boolean.TRUE.equals(r.current) ? 1 : 0)
            .addValue("now", Timestamp.valueOf(LocalDateTime.now()));
    var kh = new GeneratedKeyHolder();
    jdbc.update(sql, ps, kh, new String[] {"education_id"});
    return kh.getKey().longValue();
  }

  // --- get ---
  public Optional<Response> get(long id) {
    try {
      String sql =
          """
          SELECT * FROM jobproject_education WHERE education_id = :id
          """;
      return Optional.ofNullable(jdbc.queryForObject(sql, Map.of("id", id), MAPPER));
    } catch (EmptyResultDataAccessException e) {
      return Optional.empty();
    }
  }

  // --- list by resume with paging & sort ---
  public List<Response> listByResume(long resumeId, PageRequest pr) {
    // sort 화이트리스트 (클라이언트가 지정할 수 있는 키 → 실제 컬럼)
    Map<String, String> whitelist =
        Map.of(
            "start_date", "education_start_date",
            "end_date", "education_end_date",
            "created_at", "education_created_at",
            "updated_at", "education_updated_at",
            "id", "education_id");
    String order = JdbcUtils.orderBy(pr.getSort(), whitelist); // ← 두 번째 인자가 문자열 아님!

    String sql =
        """
        SELECT *
          FROM jobproject_education
         WHERE resume_id = :rid
        """
            + (order.isBlank() ? " ORDER BY education_start_date DESC, education_id DESC " : order)
            + " LIMIT :limit OFFSET :offset";

    var ps =
        new MapSqlParameterSource()
            .addValue("rid", resumeId)
            .addValue("limit", pr.getSize())
            .addValue("offset", pr.offset());

    return jdbc.query(sql, ps, MAPPER);
  }

  public long countByResume(long resumeId) {
    String sql = "SELECT COUNT(*) FROM jobproject_education WHERE resume_id = :rid";
    return jdbc.queryForObject(sql, Map.of("rid", resumeId), Long.class);
  }

  // --- update ---
  public int update(long id, UpdateRequest r) {
    String sql =
        """
        UPDATE jobproject_education
           SET education_school_name = :school,
               education_major = :major,
               education_degree = :degree,
               education_start_date = :start,
               education_end_date = :end,
               education_is_current = :current,
               education_updated_at = :now
         WHERE education_id = :id
        """;
    var ps =
        new MapSqlParameterSource()
            .addValue("school", r.schoolName)
            .addValue("major", r.major)
            .addValue("degree", r.degree)
            .addValue("start", r.startDate)
            .addValue("end", r.endDate)
            .addValue("current", Boolean.TRUE.equals(r.current) ? 1 : 0)
            .addValue("now", Timestamp.valueOf(LocalDateTime.now()))
            .addValue("id", id);
    return jdbc.update(sql, ps);
  }

  // --- delete ---
  public int delete(long id) {
    return jdbc.update(
        "DELETE FROM jobproject_education WHERE education_id = :id", Map.of("id", id));
  }
}
