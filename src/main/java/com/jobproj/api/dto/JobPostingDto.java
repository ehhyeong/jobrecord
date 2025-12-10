package com.jobproj.api.dto;

public class JobPostingDto {
  private Long id;
  private String title;
  private String employmentType;
  private String location;
  private String description;

  public JobPostingDto(Long id, String t, String e, String l, String d) {
    this.id = id;
    this.title = t;
    this.employmentType = e;
    this.location = l;
    this.description = d;
  }

  public Long getId() {
    return id;
  }

  public String getTitle() {
    return title;
  }

  public String getEmploymentType() {
    return employmentType;
  }

  public String getLocation() {
    return location;
  }

  public String getDescription() {
    return description;
  }
}
