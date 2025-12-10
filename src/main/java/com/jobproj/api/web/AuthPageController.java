package com.jobproj.api.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AuthPageController {

    @GetMapping("/auth/login")
    public String loginPage() {
        return "auth/login"; // templates/auth/login.html
    }

    @GetMapping("/auth/signup")
    public String signupPage() {
        return "auth/signup"; // templates/auth/signup.html
    }
}
