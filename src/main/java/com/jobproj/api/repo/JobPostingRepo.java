package com.jobproj.api.repo;

import com.jobproj.api.dto.JobPostingDto;
import java.sql.*;
import java.util.*;
import org.springframework.jdbc.core.*;
import org.springframework.stereotype.Repository;

@Repository
public class JobPostingRepo {
  private final JdbcTemplate jdbc;

  public JobPostingRepo(JdbcTemplate j) {
    this.jdbc = j;
  }

  private static class Mapper implements RowMapper<JobPostingDto> {
    @Override
    public JobPostingDto mapRow(ResultSet rs, int n) throws SQLException {
      return new JobPostingDto(
          rs.getLong("job_posting_id"),
          rs.getString("job_posting_title"),
          rs.getString("job_posting_employment_type"),
          rs.getString("job_posting_location"),
          rs.getString("job_posting_description"));
    }
  }

  public List<JobPostingDto> findActive(int limit, int offset) {
    String sql =
        "SELECT job_posting_id, job_posting_title, "
            + "job_posting_employment_type, job_posting_location, "
            + "job_posting_description FROM jobpoject_job_posting "
            + "WHERE job_posting_is_active=1 "
            + "ORDER BY job_posting_created_at DESC LIMIT ? OFFSET ?";
    return jdbc.query(sql, new Mapper(), limit, offset);
  }
}
