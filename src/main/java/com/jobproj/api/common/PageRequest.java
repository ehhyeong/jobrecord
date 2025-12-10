package com.jobproj.api.common;

public class PageRequest {
  private final int page; // 0-based
  private final int size; // 1..N
  private final String sort; // e.g. "created_at,desc"

  public PageRequest(Integer page, Integer size, String sort) {
    this.page = (page == null || page < 0) ? 0 : page;
    this.size = (size == null || size <= 0) ? 10 : Math.min(size, 100);
    this.sort = (sort == null || sort.isBlank()) ? "created_at,desc" : sort;
  }

  public int getPage() {
    return page;
  }

  public int getSize() {
    return size;
  }

  public String getSort() {
    return sort;
  }

  public int offset() {
    return page * size;
  }
}
