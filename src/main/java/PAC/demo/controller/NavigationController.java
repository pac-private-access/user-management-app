package PAC.demo.controller;

import org.springframework.ui.Model;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import PAC.demo.model.User;
import PAC.demo.repository.UserRepository;
import jakarta.servlet.http.HttpSession;


    @Controller
public class NavigationController {

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
    public String accestimpreal() {
        return "accestimpreal";
    }
    

    @GetMapping("/settings")
    public String settings() {
        return "settings";
    }


    @GetMapping("/logout")
    public String logout() {
        return "redirect:/login";
    }
}
