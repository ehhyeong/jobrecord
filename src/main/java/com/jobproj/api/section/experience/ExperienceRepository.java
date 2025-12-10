package com.jobproj.api.section.experience;

import com.jobproj.api.section.experience.ExperienceDto.*;
import java.sql.ResultSet;
import java.util.*;
import org.springframework.jdbc.core.namedparam.*;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class ExperienceRepository {
  private final NamedParameterJdbcTemplate jdbc;

  public ExperienceRepository(NamedParameterJdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  private Response map(ResultSet rs, int i) throws java.sql.SQLException {
    return new Response(
        rs.getLong("experience_id"),
        rs.getLong("resume_id"),
        rs.getString("experience_company_name"),
        rs.getString("experience_position_title"),
        rs.getObject("experience_start_date", java.time.LocalDate.class),
        rs.getObject("experience_end_date", java.time.LocalDate.class),
        rs.getInt("experience_is_current") == 1,
        rs.getString("experience_description"),
        rs.getTimestamp("experience_created_at").toLocalDateTime(),
        rs.getTimestamp("experience_updated_at").toLocalDateTime());
  }

  public long create(CreateRequest r) {
    var sql =
        """
      INSERT INTO jobproject_experience
      (resume_id,experience_company_name,experience_position_title,
       experience_start_date,experience_end_date,experience_is_current,
       experience_description)
      VALUES(:rid,:company,:title,:sd,:ed,:cur,:desc)
    """;
    var ps =
        new MapSqlParameterSource()
            .addValue("rid", r.resumeId())
            .addValue("company", r.companyName())
            .addValue("title", r.positionTitle())
            .addValue("sd", r.startDate())
            .addValue("ed", r.endDate())
            .addValue("cur", Boolean.TRUE.equals(r.isCurrent()) ? 1 : 0)
            .addValue("desc", r.description());
    var kh = new GeneratedKeyHolder();
    jdbc.update(sql, ps, kh, new String[] {"experience_id"});
    return Optional.ofNullable(kh.getKey()).map(Number::longValue).orElseThrow();
  }

  public List<Response> listByResume(long resumeId) {
    var sql =
        "SELECT * FROM jobproject_experience WHERE resume_id=:rid "
            + "ORDER BY experience_start_date DESC, experience_id DESC";
    return jdbc.query(sql, Map.of("rid", resumeId), this::map);
  }

  public Optional<Response> get(long id) {
    var sql = "SELECT * FROM jobproject_experience WHERE experience_id=:id";
    var list = jdbc.query(sql, Map.of("id", id), this::map);
    return list.stream().findFirst();
  }

  public int update(long id, UpdateRequest r) {
    var sql =
        """
      UPDATE jobproject_experience
      SET experience_company_name=:company,
          experience_position_title=:title,
          experience_start_date=:sd,
          experience_end_date=:ed,
          experience_is_current=:cur,
          experience_description=:desc
      WHERE experience_id=:id
    """;
    return jdbc.update(
        sql,
        new MapSqlParameterSource()
            .addValue("company", r.companyName())
            .addValue("title", r.positionTitle())
            .addValue("sd", r.startDate())
            .addValue("ed", r.endDate())
            .addValue("cur", Boolean.TRUE.equals(r.isCurrent()) ? 1 : 0)
            .addValue("desc", r.description())
            .addValue("id", id));
  }

  public int delete(long id) {
    return jdbc.update(
        "DELETE FROM jobproject_experience WHERE experience_id=:id", Map.of("id", id));
  }
}
