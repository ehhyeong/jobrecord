package com.jobproj.api.common;

import java.util.Map;
import java.util.StringJoiner;

public class JdbcUtils {

  public static String orderBy(String sort, Map<String, String> whitelist) {
    // sort: "field,asc" or "field,desc"
    if (sort == null || sort.isBlank()) return "";
    String[] parts = sort.split(",");
    String col = whitelist.getOrDefault(parts[0], null);
    if (col == null) return "";
    String dir = (parts.length > 1 && "asc".equalsIgnoreCase(parts[1])) ? "ASC" : "DESC";
    return " ORDER BY " + col + " " + dir + " ";
  }

  public static String whereLike(String keyword, String... columns) {
    if (keyword == null || keyword.isBlank() || columns.length == 0) return "";
    StringJoiner sj = new StringJoiner(" OR ");
    for (String c : columns) {
      sj.add(c + " LIKE :kw");
    }
    return " AND (" + sj + ") ";
  }
}
