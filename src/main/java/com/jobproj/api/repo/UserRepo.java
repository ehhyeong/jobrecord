package com.jobproj.api.repo;

import com.jobproj.api.domain.Role;
import java.sql.*;
import java.util.Optional;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.*;
import org.springframework.stereotype.Repository;

@Repository
public class UserRepo {
  private final JdbcTemplate jdbc;

  public UserRepo(JdbcTemplate j) {
    this.jdbc = j;
  }

  // DB 조회 결과를 담을 객체
  public static class UserRow implements RowMapper<UserRow> {
    public Long id;
    public String email;
    public String pwdHash; // DB의 password_hash
    public String name;
    public String phone;   // 🔽 전화번호 필드 추가
    public Role role;
    public Timestamp createdAt; // 가입일
    public Timestamp updatedAt; // 최근 수정일

    @Override
    public UserRow mapRow(ResultSet rs, int n) throws SQLException {
      UserRow u = new UserRow();
      u.id = rs.getLong("users_id");
      u.email = rs.getString("users_email");
      u.pwdHash = rs.getString("users_password_hash");
      u.name = rs.getString("users_name");
      u.phone = rs.getString("users_phone"); // 🔽 phone 매핑
      u.role = Role.fromString(rs.getString("users_role"));
      u.createdAt = rs.getTimestamp("users_created_at");
      u.updatedAt = rs.getTimestamp("users_updated_at");
      return u;
    }
  }

  // 이메일로 사용자 찾기 (로그인 시 사용)
  public Optional<UserRow> findByEmail(String email) {
    String sql =
            "SELECT users_id, users_email, users_password_hash, "
                    + "users_name, users_phone, users_role, "
                    + "users_created_at, users_updated_at "
                    + "FROM jobproject_users WHERE users_email=?";
    return jdbc.query(sql, new UserRow(), email).stream().findFirst();
  }

  // 이메일로 users_id만 조회 (컨텍스트 사용자 ID 변환에 사용)
  public Optional<Long> findIdByEmail(String email) {
    String sql = "SELECT users_id FROM jobproject_users WHERE users_email = ?";
    try {
      Long id = jdbc.queryForObject(sql, Long.class, email);
      return Optional.ofNullable(id);
    } catch (EmptyResultDataAccessException e) {
      return Optional.empty();
    }
  }

  // 이메일 중복 확인 (회원가입 시 사용)
  public boolean existsByEmail(String email) {
    String sql = "SELECT COUNT(*) FROM jobproject_users WHERE users_email=?";
    Integer count = jdbc.queryForObject(sql, Integer.class, email);
    return count != null && count > 0;
  }

  // 🔽 전화번호 중복 확인 (회원가입 시 사용)
  public boolean existsByPhone(String phone) {
    String sql = "SELECT COUNT(*) FROM jobproject_users WHERE users_phone = ?";
    Integer count = jdbc.queryForObject(sql, Integer.class, phone);
    return count != null && count > 0;
  }

  // 🔽 phone까지 저장하도록 수정
  public void save(String email, String encodedPassword, String name, String phone, Role role) {
    String sql =
            "INSERT INTO jobproject_users "
                    + "(users_email, users_password_hash, users_name, users_phone, users_role) "
                    + "VALUES (?, ?, ?, ?, ?)";
    jdbc.update(sql, email, encodedPassword, name, phone, role.name());
  }

  // 이메일을 기준으로 비밀번호 해시 업데이트.
  public int updatePasswordByEmail(String email, String encodedPassword) {
    String sql =
            "UPDATE jobproject_users SET users_password_hash = ? WHERE users_email = ?";
    return jdbc.update(sql, encodedPassword, email);
  }

  // users_id로 사용자 상세 정보 조회
  public Optional<UserRow> findById(Long usersId) {
    String sql =
            "SELECT users_id, users_email, users_password_hash, "
                    + "users_name, users_phone, users_role, "
                    + "users_created_at, users_updated_at "
                    + "FROM jobproject_users WHERE users_id=?";
    return jdbc.query(sql, new UserRow(), usersId).stream().findFirst();
  }

  // ========================================================
  // 2233076 12주차 추가: 내 정보 수정 및 탈퇴 기능 메서드
  // ========================================================

//회원 탈퇴기능
  public int deleteByEmail(String email) {
    String sql = "DELETE FROM jobproject_users WHERE users_email = ?";
    return jdbc.update(sql, email);
  }

}