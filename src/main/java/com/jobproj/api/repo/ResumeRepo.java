package com.jobproj.api.repo;

import com.jobproj.api.dto.ResumeDto;
import java.sql.*;
import java.util.*;
import org.springframework.jdbc.core.*;
import org.springframework.stereotype.Repository;

@Repository
public class ResumeRepo {
  private final JdbcTemplate jdbc;

  public ResumeRepo(JdbcTemplate j) {
    this.jdbc = j;
  }

  private static class Mapper implements RowMapper<ResumeDto> {
    @Override
    public ResumeDto mapRow(ResultSet rs, int n) throws SQLException {
      return new ResumeDto(
          rs.getLong("resume_id"),
          rs.getLong("users_id"),
          rs.getString("title"),
          rs.getString("summary"),
          rs.getBoolean("is_public"));
    }
  }

  public List<ResumeDto> findByUserId(Long userId) {
    String sql =
        "SELECT resume_id, users_id, title, summary, is_public "
            + "FROM jobpoject_resume WHERE users_id=? "
            + "ORDER BY resume_created_at DESC";
    return jdbc.query(sql, new Mapper(), userId);
  }

  public Long insert(Long userId, String title, String summary, boolean isPublic) {
    String sql =
        "INSERT INTO jobpoject_resume " + "(users_id, title, summary, is_public) VALUES (?,?,?,?)";
    jdbc.update(sql, userId, title, summary, isPublic ? 1 : 0);
    Long id = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    return id;
  }

  public int update(Long resumeId, String title, String summary, boolean isPublic) {
    String sql =
        "UPDATE jobpoject_resume SET title=?, summary=?, is_public=?, "
            + "resume_updated_at=NOW() WHERE resume_id=?";
    return jdbc.update(sql, title, summary, isPublic ? 1 : 0, resumeId);
  }

  public int delete(Long resumeId) {
    String sql = "DELETE FROM jobpoject_resume WHERE resume_id=?";
    return jdbc.update(sql, resumeId);
  }
}
