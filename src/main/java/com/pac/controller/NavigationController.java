package com.pac.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import com.pac.repository.AccessLogRepository;

import org.springframework.ui.Model;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import lombok.RequiredArgsConstructor;


@RequiredArgsConstructor
@Controller
public class NavigationController {
    private final AccessLogRepository accessLogRepository;

    @GetMapping("/dashboard")
    public String dashboard() {

        /*
        model.addAttribute(
            "totalUsers",
            userRepository.count()
        );

        model.addAttribute(
            "activeUsers",
            userRepository.findActiveUsers()
        );

        model.addAttribute(
            "recentEvents",
            eventRepository.findRecentEvents()
        );
        */

        return "dashboard";
    }

    @GetMapping("/accestimpreal")
    public String accestimpreal(Model model) {
        model.addAttribute("recentLogs", accessLogRepository.findAll(PageRequest.of(0, 15, Sort.by(Sort.Direction.DESC, "timestamp"))).getContent());
        return "accestimpreal";
    }
    

    @GetMapping("/settings")
    public String settings() {
        return "settings";
    }
}
