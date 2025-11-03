package com.jobproj.api.resume;

import com.jobproj.api.common.OwnerMismatchException;
import com.jobproj.api.common.PageRequest;
import com.jobproj.api.common.PageResponse;
import com.jobproj.api.resume.ResumeDto.CreateRequest;
import com.jobproj.api.resume.ResumeDto.Response;
import com.jobproj.api.resume.ResumeDto.UpdateRequest;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ResumeService {

  private final ResumeRepository repo;

  /** JWT에서 구한 usersId를 인자로 받아 생성 */
  @Transactional
  public Long create(Long usersId, CreateRequest req) {
    // 생성 시 소유자(usersId) 고정
    if (usersId == null) throw new IllegalArgumentException("usersId required");
    if (req.title == null || req.title.isBlank())
      throw new IllegalArgumentException("title required");
    return repo.create(usersId, req);
  }

  // 단건 조회 시 404/403을 명확히 분리
  @Transactional(readOnly = true)
  public Optional<Response> get(Long id, Long usersId) {
    if (usersId == null) throw new IllegalArgumentException("usersId required");
    var ownerOpt = repo.findOwnerId(id);
    if (ownerOpt.isEmpty()) return Optional.empty();
    if (!ownerOpt.get().equals(usersId))
      throw new OwnerMismatchException("resume owner != me");
    return repo.findById(id);
  }

  // 소유자만 업데이트(404/403 분기)
  @Transactional
  public boolean update(Long id, Long usersId, UpdateRequest req) {
    if (usersId == null) throw new IllegalArgumentException("usersId required");

    // 1) 존재 확인 (404)
    var ownerOpt = repo.findOwnerId(id);
    if (ownerOpt.isEmpty()) throw new IllegalArgumentException("resume not found");

    // 2) 소유권 확인 (403)
    if (!ownerOpt.get().equals(usersId)) throw new OwnerMismatchException();

    // 3) 소유자 조건으로 업데이트
    return repo.updateByOwner(id, usersId, req) > 0;
  }

  // 소유자만 삭제(404/403 분기)
  @Transactional
  public boolean delete(Long id, Long usersId) {
    if (usersId == null) throw new IllegalArgumentException("usersId required");

    var ownerOpt = repo.findOwnerId(id);
    if (ownerOpt.isEmpty()) throw new IllegalArgumentException("resume not found");
    if (!ownerOpt.get().equals(usersId)) throw new OwnerMismatchException();

    return repo.deleteByOwner(id, usersId) > 0;
  }

  @Transactional(readOnly = true)
  public PageResponse<Response> list(PageRequest pr, Long usersId, String keyword) {
    var items = repo.search(pr, usersId, keyword);
    var total = repo.count(usersId, keyword);
    return new PageResponse<>(items, pr.getPage(), pr.getSize(), total);
  }
}
