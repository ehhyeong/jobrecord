package com.jobproj.api.resume;

import com.jobproj.api.common.OwnerMismatchException;
import com.jobproj.api.common.PageRequest;
import com.jobproj.api.common.PageResponse;
import com.jobproj.api.dto.ResumeDto;
import com.jobproj.api.dto.ResumeDto.CreateRequest;
import com.jobproj.api.dto.ResumeDto.Response;
import com.jobproj.api.dto.ResumeDto.UpdateRequest;
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
        if (usersId == null) {
            throw new IllegalArgumentException("usersId required");
        }
        if (req == null || req.title == null || req.title.isBlank()) {
            throw new IllegalArgumentException("title required");
        }
        return repo.create(usersId, req);
    }

    // 단건 조회 (404 / 403 분리)
    @Transactional(readOnly = true)
    public Optional<Response> get(Long id, Long usersId) {
        if (usersId == null) {
            throw new IllegalArgumentException("usersId required");
        }
        var ownerOpt = repo.findOwnerId(id);
        if (ownerOpt.isEmpty()) {
            // 404: 이력서 없음
            return Optional.empty();
        }
        if (!ownerOpt.get().equals(usersId)) {
            // 403: 소유권 위반
            throw new OwnerMismatchException("resume owner != me");
        }
        return repo.findById(id);
    }

    // 소유자만 업데이트
    @Transactional
    public boolean update(Long id, Long usersId, UpdateRequest req) {
        verifyOwnerOrThrow(id, usersId);
        return repo.updateByOwner(id, usersId, req) > 0;
    }

    // 소유자만 삭제
    @Transactional
    public boolean delete(Long id, Long usersId) {
        verifyOwnerOrThrow(id, usersId);
        return repo.deleteByOwner(id, usersId) > 0;
    }

    // 목록 조회
    @Transactional(readOnly = true)
    public PageResponse<Response> list(PageRequest pr, Long usersId, String keyword) {
        if (usersId == null) {
            throw new IllegalArgumentException("usersId required");
        }
        var items = repo.search(pr, usersId, keyword);
        var total = repo.count(usersId, keyword);
        return new PageResponse<>(items, pr.getPage(), pr.getSize(), total);
    }

    // 프로필 사진 URL 업데이트
    @Transactional
    public String updateProfileImage(Long id, Long usersId, String profileImageUrl) {
        verifyOwnerOrThrow(id, usersId);
        int updated = repo.updateProfileImageUrlByOwner(id, usersId, profileImageUrl);
        if (updated <= 0) {
            throw new IllegalArgumentException("resume not found");
        }
        return profileImageUrl;
    }

    // 공통 소유권 검사
    private void verifyOwnerOrThrow(Long resumeId, Long usersId) {
        if (usersId == null) {
            throw new IllegalArgumentException("usersId required");
        }
        var ownerOpt = repo.findOwnerId(resumeId);
        if (ownerOpt.isEmpty()) {
            throw new IllegalArgumentException("resume not found");
        }
        if (!ownerOpt.get().equals(usersId)) {
            throw new OwnerMismatchException();
        }
    }
}
