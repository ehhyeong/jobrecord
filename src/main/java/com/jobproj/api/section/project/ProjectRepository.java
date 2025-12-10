package com.jobproj.api.section.project;

import com.jobproj.api.section.project.ProjectDto.*;
import java.sql.ResultSet;
import java.util.*;
import org.springframework.jdbc.core.namedparam.*;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class ProjectRepository {
  private final NamedParameterJdbcTemplate jdbc;

  public ProjectRepository(NamedParameterJdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  private Response map(ResultSet rs, int i) throws java.sql.SQLException {
    return new Response(
        rs.getLong("project_id"),
        rs.getLong("resume_id"),
        rs.getString("project_name"),
        rs.getString("project_role"),
        rs.getObject("project_start_date", java.time.LocalDate.class),
        rs.getObject("project_end_date", java.time.LocalDate.class),
        rs.getInt("project_is_current") == 1,
        rs.getString("project_summary"),
        rs.getString("project_tech_stack"),
        rs.getString("project_url"),
        rs.getTimestamp("project_created_at").toLocalDateTime(),
        rs.getTimestamp("project_updated_at").toLocalDateTime());
  }

  public long create(CreateRequest r) {
    var sql =
        """
      INSERT INTO jobproject_project
      (resume_id,project_name,project_role,project_start_date,
       project_end_date,project_is_current,project_summary,
       project_tech_stack,project_url)
      VALUES(:rid,:name,:role,:sd,:ed,:cur,:sum,:tech,:url)
    """;
    var ps =
        new MapSqlParameterSource()
            .addValue("rid", r.resumeId())
            .addValue("name", r.name())
            .addValue("role", r.role())
            .addValue("sd", r.startDate())
            .addValue("ed", r.endDate())
            .addValue("cur", Boolean.TRUE.equals(r.isCurrent()) ? 1 : 0)
            .addValue("sum", r.summary())
            .addValue("tech", r.techStack())
            .addValue("url", r.url());
    var kh = new GeneratedKeyHolder();
    jdbc.update(sql, ps, kh, new String[] {"project_id"});
    return Optional.ofNullable(kh.getKey()).map(Number::longValue).orElseThrow();
  }

  public List<Response> listByResume(long resumeId) {
    var sql =
        "SELECT * FROM jobproject_project WHERE resume_id=:rid "
            + "ORDER BY project_start_date DESC, project_id DESC";
    return jdbc.query(sql, Map.of("rid", resumeId), this::map);
  }

  public Optional<Response> get(long id) {
    var sql = "SELECT * FROM jobproject_project WHERE project_id=:id";
    var list = jdbc.query(sql, Map.of("id", id), this::map);
    return list.stream().findFirst();
  }

  public int update(long id, UpdateRequest r) {
    var sql =
        """
      UPDATE jobproject_project
      SET project_name=:name, project_role=:role,
          project_start_date=:sd, project_end_date=:ed,
          project_is_current=:cur, project_summary=:sum,
          project_tech_stack=:tech, project_url=:url
      WHERE project_id=:id
    """;
    return jdbc.update(
        sql,
        new MapSqlParameterSource()
            .addValue("name", r.name())
            .addValue("role", r.role())
            .addValue("sd", r.startDate())
            .addValue("ed", r.endDate())
            .addValue("cur", Boolean.TRUE.equals(r.isCurrent()) ? 1 : 0)
            .addValue("sum", r.summary())
            .addValue("tech", r.techStack())
            .addValue("url", r.url())
            .addValue("id", id));
  }

  public int delete(long id) {
    return jdbc.update("DELETE FROM jobproject_project WHERE project_id=:id", Map.of("id", id));
  }
}
