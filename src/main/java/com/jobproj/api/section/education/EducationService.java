package com.jobproj.api.section.education;

import com.jobproj.api.common.PageRequest;
import com.jobproj.api.common.PageResponse;
import com.jobproj.api.section.education.EducationDto.CreateRequest;
import com.jobproj.api.section.education.EducationDto.Response;
import com.jobproj.api.section.education.EducationDto.UpdateRequest;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EducationService {

  private final EducationRepository repo;

  public EducationService(EducationRepository repo) {
    this.repo = repo;
  }

  @Transactional
  public Long create(CreateRequest r) {
    if (r.resumeId == null) throw new IllegalArgumentException("resumeId required");
    if (r.schoolName == null || r.schoolName.isBlank())
      throw new IllegalArgumentException("schoolName required");
    return repo.create(r);
  }

  @Transactional(readOnly = true)
  public Optional<Response> get(long id) {
    return repo.get(id);
  }

  @Transactional
  public boolean update(long id, UpdateRequest r) {
    return repo.update(id, r) > 0;
  }

  @Transactional
  public boolean delete(long id) {
    return repo.delete(id) > 0;
  }

  @Transactional(readOnly = true)
  public PageResponse<Response> listByResume(long resumeId, PageRequest pr) {
    var items = repo.listByResume(resumeId, pr); // List<Response>
    var total = repo.countByResume(resumeId); // long
    return new PageResponse<>(items, pr.getPage(), pr.getSize(), total);
  }
}
