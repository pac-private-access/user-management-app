package com.pac.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;


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