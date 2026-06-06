package PAC.demo.controller;

import org.springframework.ui.Model;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import PAC.demo.model.User;
import PAC.demo.repository.UserRepository;
import jakarta.servlet.http.HttpSession;

@Controller
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/users")
public String users( Model model) {

    model.addAttribute(
        "users",
        userRepository.findAll()
    );

    return "users";
}

    @GetMapping("/login")
public String showLoginPage() {
    return "login";
}


    @PostMapping("/users")
    public String saveUser(@ModelAttribute User user) {
        userRepository.save(user);
        return "redirect:/users";
    }
    @PostMapping("/login")
public String login(@RequestParam String email,
                    @RequestParam String parola,
                    HttpSession session) {

    User user = userRepository.findByEmail(email);

    if (user != null && user.getParola().equals(parola)) {
        session.setAttribute("user", user);
        return "redirect:/users";
    }

    return "redirect:/login";
}
}