package com.pac.controller;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.pac.entity.Role;
import com.pac.entity.User;
import com.pac.repository.AccessLogRepository;
import com.pac.repository.DivisionRepository;
import com.pac.repository.RoleRepository;
import com.pac.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class UserWebController {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final DivisionRepository divisionRepository;
    private final AccessLogRepository accessLogRepository;
    private final PasswordEncoder passwordEncoder;

    @GetMapping("/login")
    public String showLoginPage() { return "login"; }

    // ─── Utilizatori page ─────────────────────────────────────────────────────

    @GetMapping("/users")
    public String users(Model model) {
        OffsetDateTime now           = OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime startOfToday  = now.toLocalDate().atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime endOfToday    = startOfToday.plusDays(1);

        List<User> users = userRepository.findAll();

        Map<UUID, String> rolesByUserId = users.stream().collect(Collectors.toMap(
            User::getId,
            u -> u.getRoles().isEmpty() ? "—"
                : u.getRoles().stream().map(Role::getName).collect(Collectors.joining(", "))
        ));

        model.addAttribute("users",          users);
        model.addAttribute("rolesByUserId",  rolesByUserId);
        model.addAttribute("activeWebUsers", userRepository.countActive(true));
        model.addAttribute("entriesToday",
                accessLogRepository.countEntries("entry", startOfToday, endOfToday));
        model.addAttribute("refusedToday",
                accessLogRepository.countEntriesByAuth("entry", false, startOfToday, endOfToday));

        String lastAccess = accessLogRepository.findLatest()
            .map(log -> {
                DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm");
                return log.getEventAt().atZoneSameInstant(ZoneOffset.UTC).format(fmt);
            })
            .orElse("—");
        model.addAttribute("lastAccess", lastAccess);

        // Form support
        model.addAttribute("roles",     roleRepository.findAll());
        model.addAttribute("divisions", divisionRepository.findAll());

        return "users";
    }

    // ─── Add web user ─────────────────────────────────────────────────────────

    @PostMapping("/users/add")
    public String addUser(
            @RequestParam String fullName,
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam UUID roleId,
            @RequestParam(required = false) UUID divisionId,
            RedirectAttributes redirectAttributes) {

        if (userRepository.findByEmail(email).isPresent()) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Există deja un utilizator cu email-ul: " + email);
            return "redirect:/users";
        }

        Role role = roleRepository.findById(roleId).orElse(null);
        if (role == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Rol invalid selectat.");
            return "redirect:/users";
        }

        OffsetDateTime now = OffsetDateTime.now();
        User user = User.builder()
                .fullName(fullName)
                .email(email)
                .passwordHash(passwordEncoder.encode(password))
                .divisionId(divisionId)
                .isActive(true)
                .roles(Set.of(role))
                .createdAt(now)
                .updatedAt(now)
                .build();

        userRepository.save(user);
        redirectAttributes.addFlashAttribute("successMessage",
                "Utilizator " + fullName + " adăugat cu succes.");
        return "redirect:/users";
    }
}
