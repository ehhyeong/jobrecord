package com.jobproj.api.section.skill;

import com.jobproj.api.section.skill.SkillDto.*;
import java.util.*;
import org.springframework.stereotype.Service;

@Service
public class SkillService {
  private final SkillRepository repo;

  public SkillService(SkillRepository repo) {
    this.repo = repo;
  }

  // master
  public long create(SkillCreate r) {
    return repo.createSkill(r);
  }

  public List<SkillResponse> search(String q, int limit) {
    return repo.searchSkills(q, limit);
  }

  public Optional<SkillResponse> get(long id) {
    return repo.getSkill(id);
  }

  public boolean update(long id, SkillUpdate r) {
    return repo.updateSkill(id, r) > 0;
  }

  public boolean delete(long id) {
    return repo.deleteSkill(id) > 0;
  }

  // mapping
  public boolean upsert(long resumeId, long skillId, int prof) {
    return repo.upsertResumeSkill(resumeId, skillId, prof) > 0;
  }

  public List<ResumeSkillResponse> listByResume(long resumeId) {
    return repo.listResumeSkills(resumeId);
  }

  public boolean remove(long resumeId, long skillId) {
    return repo.deleteResumeSkill(resumeId, skillId) > 0;
  }
}
