package com.ptit.chess.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ViewController {

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @GetMapping("/register")
    public String registerPage() {
        return "login"; // Assuming login.html has both forms, or we can use separate
    }

    @GetMapping("/profile")
    public String profilePage() {
        return "profile";
    }
    
    @GetMapping("/")
    public String homePage() {
        return "profile";
    }

    @GetMapping("/lobby")
    public String lobbyPage() {
        return "lobby";
    }

    @GetMapping("/match/{roomId}")
    public String matchPage() {
        return "match";
    }

    @GetMapping("/admin")
    public String adminPage() {
        return "admin-dashboard";
    }

    @GetMapping("/admin/dashboard")
    public String adminDashboard() {
        return "admin-dashboard";
    }
}
