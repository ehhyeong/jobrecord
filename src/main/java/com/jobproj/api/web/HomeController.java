package com.jobproj.api.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping({"/", "/home"})
    public String home() {
        return "home"; // templates/home.html
    }
    @GetMapping("/user/home")
    public String userHome() {
        return "user-home"; // templates/user-home.html
    }
}