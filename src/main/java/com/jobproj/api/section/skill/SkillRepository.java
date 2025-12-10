package com.jobproj.api.section.skill;

import com.jobproj.api.section.skill.SkillDto.*;
import java.sql.ResultSet;
import java.util.*;
import org.springframework.jdbc.core.namedparam.*;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class SkillRepository {
  private final NamedParameterJdbcTemplate jdbc;

  public SkillRepository(NamedParameterJdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  private SkillResponse mapSkill(ResultSet rs, int i) throws java.sql.SQLException {
    return new SkillResponse(
        rs.getLong("skill_id"),
        rs.getString("skill_name"),
        rs.getTimestamp("skill_created_at").toLocalDateTime(),
        rs.getTimestamp("skill_updated_at").toLocalDateTime());
  }

  private ResumeSkillResponse mapResumeSkill(ResultSet rs, int i) throws java.sql.SQLException {
    return new ResumeSkillResponse(
        rs.getLong("resume_id"),
        rs.getLong("skill_id"),
        rs.getString("skill_name"),
        (Integer) rs.getObject("proficiency"));
  }

  // ----- Skill master -----
  public long createSkill(SkillCreate r) {
    var sql = "INSERT INTO jobproject_skill (skill_name) VALUES (:n)";
    var kh = new GeneratedKeyHolder();
    jdbc.update(
        sql, new MapSqlParameterSource().addValue("n", r.name()), kh, new String[] {"skill_id"});
    return Optional.ofNullable(kh.getKey()).map(Number::longValue).orElseThrow();
  }

  public List<SkillResponse> searchSkills(String q, int limit) {
    var sql =
        """
      SELECT * FROM jobproject_skill
      WHERE skill_name LIKE :q
      ORDER BY skill_name ASC
      LIMIT :limit
    """;
    var ps =
        new MapSqlParameterSource()
            .addValue("q", "%" + (q == null ? "" : q.trim()) + "%")
            .addValue("limit", limit);
    return jdbc.query(sql, ps, this::mapSkill);
  }

  public Optional<SkillResponse> getSkill(long id) {
    var list =
        jdbc.query(
            "SELECT * FROM jobproject_skill WHERE skill_id=:id", Map.of("id", id), this::mapSkill);
    return list.stream().findFirst();
  }

  public int updateSkill(long id, SkillUpdate r) {
    return jdbc.update(
        "UPDATE jobproject_skill SET skill_name=:n WHERE skill_id=:id",
        new MapSqlParameterSource().addValue("n", r.name()).addValue("id", id));
  }

  public int deleteSkill(long id) {
    return jdbc.update("DELETE FROM jobproject_skill WHERE skill_id=:id", Map.of("id", id));
  }

  // ----- Resume-Skill mapping -----
  public int upsertResumeSkill(long resumeId, long skillId, int prof) {
    var sql =
        """
      INSERT INTO jobproject_resume_skill (resume_id,skill_id,proficiency)
      VALUES (:rid,:sid,:p)
      ON DUPLICATE KEY UPDATE proficiency=VALUES(proficiency)
    """;
    return jdbc.update(
        sql,
        new MapSqlParameterSource()
            .addValue("rid", resumeId)
            .addValue("sid", skillId)
            .addValue("p", prof));
  }

  public List<ResumeSkillResponse> listResumeSkills(long resumeId) {
    var sql =
        """
      SELECT rs.resume_id, s.skill_id, s.skill_name, rs.proficiency
      FROM jobproject_resume_skill rs
      JOIN jobproject_skill s ON s.skill_id=rs.skill_id
      WHERE rs.resume_id=:rid
      ORDER BY rs.proficiency DESC, s.skill_name ASC
    """;
    return jdbc.query(sql, Map.of("rid", resumeId), this::mapResumeSkill);
  }

  public int deleteResumeSkill(long resumeId, long skillId) {
    return jdbc.update(
        """
      DELETE FROM jobproject_resume_skill
      WHERE resume_id=:rid AND skill_id=:sid
    """,
        new MapSqlParameterSource().addValue("rid", resumeId).addValue("sid", skillId));
  }
}
