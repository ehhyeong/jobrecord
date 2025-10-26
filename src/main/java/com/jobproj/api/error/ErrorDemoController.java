package com.jobproj.api.error;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ErrorDemoController {

  @GetMapping("/api/error-demo")
  public String errorDemo() {
    throw new IllegalArgumentException("invalid_parameter");
  }
}
