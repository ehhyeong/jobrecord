package com.jobproj.api.domain;

public enum Role {
  USER,
  ADMIN;

  public static Role fromString(String roleString) {
    if (roleString == null) {
      return USER; // 기본값
    }
    try {
      // "role_user" -> "USER"
      return Role.valueOf(roleString.toUpperCase());
    } catch (IllegalArgumentException e) {
      // DB에 "USER" 또는 "ADMIN"이 아닌 값이 들어있을 경우
      return USER; // 기본값으로 처리
    }
  }
}
