package com.pac.controller;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.pac.entity.User;
import com.pac.repository.AccessLogRepository;
import com.pac.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class UserWebController {

    private final UserRepository userRepository;
    private final AccessLogRepository accessLogRepository;

    @GetMapping("/login")
    public String showLoginPage() { return "login"; }

    @GetMapping("/users")
    public String users(Model model) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime startOfToday = now.toLocalDate().atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime endOfToday = startOfToday.plusDays(1);

        List<User> users = userRepository.findAll();

        // Pre-compute role strings so template doesn't have to iterate collections
        Map<UUID, String> rolesByUserId = users.stream().collect(Collectors.toMap(
            User::getId,
            u -> u.getRoles().isEmpty() ? "—"
                : u.getRoles().stream().map(r -> r.getName()).collect(Collectors.joining(", "))
        ));

        model.addAttribute("users", users);
        model.addAttribute("rolesByUserId", rolesByUserId);
        model.addAttribute("activeWebUsers", userRepository.countActive(true));
        model.addAttribute("entriesToday", accessLogRepository.countEntries("entry", startOfToday, endOfToday));
        model.addAttribute("refusedToday", accessLogRepository.countEntriesByAuth("entry", false, startOfToday, endOfToday));

        String lastAccess = accessLogRepository.findLatest()
            .map(log -> {
                DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm");
                return log.getEventAt().atZoneSameInstant(ZoneOffset.UTC).format(fmt);
            })
            .orElse("—");
        model.addAttribute("lastAccess", lastAccess);

        return "users";
    }
}
