package com.jobproj.api.section.project;

import com.jobproj.api.section.project.ProjectDto.*;
import java.util.*;
import org.springframework.stereotype.Service;

@Service
public class ProjectService {
  private final ProjectRepository repo;

  public ProjectService(ProjectRepository repo) {
    this.repo = repo;
  }

  public long create(CreateRequest r) {
    return repo.create(r);
  }

  public List<Response> listByResume(long resumeId) {
    return repo.listByResume(resumeId);
  }

  public Optional<Response> get(long id) {
    return repo.get(id);
  }

  public boolean update(long id, UpdateRequest r) {
    return repo.update(id, r) > 0;
  }

  public boolean delete(long id) {
    return repo.delete(id) > 0;
  }
}
