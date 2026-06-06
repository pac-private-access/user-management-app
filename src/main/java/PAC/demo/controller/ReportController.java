package PAC.demo.controller;

import org.springframework.ui.Model;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import PAC.demo.model.User;
import PAC.demo.repository.UserRepository;
import jakarta.servlet.http.HttpSession;


@Controller
public class ReportController {

    @GetMapping("/rapoarte")
    public String rapoarte() {
        return "rapoarte";
    }

    @PostMapping("/rapoarte")
    public String generateReport( // de implementat metoda 
            @RequestParam String perioada,
            Model model) {

        model.addAttribute("perioadaSelectata", perioada);
        model.addAttribute("mesaj",
                "Raport generat pentru perioada: " + perioada);

        return "rapoarte";
    }
}