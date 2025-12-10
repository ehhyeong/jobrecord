package com.jobproj.api.ctrl;

import com.jobproj.api.dto.JobPostingDto;
import com.jobproj.api.repo.JobPostingRepo;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class JobPostingCtrl {
  private final JobPostingRepo jobs;

  public JobPostingCtrl(JobPostingRepo jobs) {
    this.jobs = jobs;
  }

  @GetMapping("/job-postings/active")
  public List<JobPostingDto> listActive(
      @RequestParam(defaultValue = "10") int limit, @RequestParam(defaultValue = "0") int offset) {
    return jobs.findActive(limit, offset);
  }
}
