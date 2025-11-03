package com.jobproj.api.attachment;

import com.jobproj.api.attachment.AttachmentDto.*;
import java.sql.ResultSet;
import java.util.*;
import org.springframework.jdbc.core.namedparam.*;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class AttachmentRepository {
  private final NamedParameterJdbcTemplate jdbc;

  public AttachmentRepository(NamedParameterJdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  private Response map(ResultSet rs, int i) throws java.sql.SQLException {
    return new Response(
        rs.getLong("attachment_id"),
        rs.getLong("resume_id"),
        rs.getString("filename"),
        rs.getString("content_type"),
        rs.getLong("size_bytes"),
        rs.getString("storage_key"),
        rs.getInt("is_profile_image") == 1,
        rs.getTimestamp("attachment_created_at").toLocalDateTime());
  }

  public long create(CreateRequest r) {
    var sql =
        """
        INSERT INTO jobproject_attachment
        (resume_id,filename,content_type,size_bytes,storage_key,is_profile_image)
        VALUES(:rid,:fn,:ct,:sz,:key,:pi)
        """;
    var ps =
        new MapSqlParameterSource()
            .addValue("rid", r.resumeId())
            .addValue("fn", r.filename())
            .addValue("ct", r.contentType())
            .addValue("sz", r.sizeBytes())
            .addValue("key", r.storageKey())
            .addValue("pi", Boolean.TRUE.equals(r.isProfileImage()) ? 1 : 0);
    var kh = new GeneratedKeyHolder();
    jdbc.update(sql, ps, kh, new String[] {"attachment_id"});
    return Optional.ofNullable(kh.getKey()).map(Number::longValue).orElseThrow();
  }

  public List<Response> listByResume(long resumeId) {
    var sql =
        "SELECT * FROM jobproject_attachment WHERE resume_id=:rid "
            + "ORDER BY is_profile_image DESC, attachment_id DESC";
    return jdbc.query(sql, Map.of("rid", resumeId), this::map);
  }

  public Optional<Response> findById(long id) {
    var sql = "SELECT * FROM jobproject_attachment WHERE attachment_id=:id";
    var list = jdbc.query(sql, Map.of("id", id), this::map);
    return list.stream().findFirst();
  }

  public int delete(long id) {
    return jdbc.update(
        "DELETE FROM jobproject_attachment WHERE attachment_id=:id", Map.of("id", id));
  }

  public int setProfile(long resumeId, long attachmentId) {
    // 프로필 단일성: 모두 0 -> 대상만 1
    jdbc.update(
        """
        UPDATE jobproject_attachment
        SET is_profile_image=0 WHERE resume_id=:rid
        """,
        Map.of("rid", resumeId));
    return jdbc.update(
        """
        UPDATE jobproject_attachment
        SET is_profile_image=1
        WHERE resume_id=:rid AND attachment_id=:id
        """,
        new MapSqlParameterSource().addValue("rid", resumeId).addValue("id", attachmentId));
  }
}
