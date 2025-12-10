package com.jobproj.api.ctrl;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthCtrl {
  @GetMapping("/health")
  public Map<String, String> getHealth() {
    return Map.of("status", "ok");
  }
}
