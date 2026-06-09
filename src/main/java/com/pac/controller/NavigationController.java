package com.pac.controller;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.pac.repository.AccessLogRepository;
import com.pac.repository.EmployeeRepository;
import com.pac.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class NavigationController {

  private final EmployeeRepository employeeRepository;
  private final AccessLogRepository accessLogRepository;
  private final UserRepository userRepository;

  // ─── Dashboard ───────────────────────────────────────────────────────────
  @GetMapping("/dashboard")
  public String dashboard(Model model, @AuthenticationPrincipal UserDetails principal) {
    OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
    OffsetDateTime startOfToday = now.toLocalDate().atStartOfDay().atOffset(ZoneOffset.UTC);
    OffsetDateTime endOfToday = startOfToday.plusDays(1);
    OffsetDateTime sevenDaysAgo = startOfToday.minusDays(6);

    // Statistici principale
    model.addAttribute("totalAngajati", employeeRepository.count());
    model.addAttribute("activeWebUsers", userRepository.countActive(true));
    model.addAttribute(
        "intrariAzi", accessLogRepository.countEntries("entry", startOfToday, endOfToday));
    model.addAttribute(
        "iesiriAzi", accessLogRepository.countEntries("exit", startOfToday, endOfToday));
    model.addAttribute(
        "refuzateAzi",
        accessLogRepository.countEntriesByAuth("entry", false, startOfToday, endOfToday));
    model.addAttribute("inClădireAcum", accessLogRepository.countEmployeesCurrentlyInside());

    // Grafic 7 zile
    List<Object[]> perDay = accessLogRepository.countEntriesPerDayAfter(sevenDaysAgo);
    List<String> labels = perDay.stream().map(r -> r[1].toString()).collect(Collectors.toList());
    List<Long> values =
        perDay.stream().map(r -> ((Number) r[0]).longValue()).collect(Collectors.toList());
    model.addAttribute("chartLabels", labels);
    model.addAttribute("chartValues", values);

    // Activitate recentă
    List<Object[]> recent = accessLogRepository.findRecentActivity();
    model.addAttribute("recentActivity", recent);

    // User curent
    addCurrentUser(model, principal);

    return "dashboard";
  }

  // ─── Acces în timp real ───────────────────────────────────────────────────
  @GetMapping("/accestimpreal")
  public String accestimpreal(Model model, @AuthenticationPrincipal UserDetails principal) {
    addCurrentUser(model, principal);
    return "accestimpreal";
  }

  // ─── Rapoarte ─────────────────────────────────────────────────────────────
  @GetMapping("/rapoarte")
  public String rapoarte(
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
      Model model,
      @AuthenticationPrincipal UserDetails principal) {

    model.addAttribute("from", from);
    model.addAttribute("to", to);

    if (from != null && to != null) {
      OffsetDateTime start = from.atStartOfDay().atOffset(ZoneOffset.UTC);
      OffsetDateTime end = to.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);

      List<Object[]> raw = accessLogRepository.findReportData(start, end);
      DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

      List<Map<String, String>> rows =
          raw.stream()
              .map(
                  r ->
                      Map.of(
                          "time",
                              r[0] instanceof OffsetDateTime
                                  ? ((OffsetDateTime) r[0])
                                      .atZoneSameInstant(ZoneOffset.UTC)
                                      .format(fmt)
                                  : r[0].toString(),
                          "employee", r[3] != null ? r[3].toString() : "Necunoscut",
                          "division", r[4] != null ? r[4].toString() : "—",
                          "type", "entry".equals(r[1]) ? "Intrare" : "Ieșire",
                          "authorized",
                              Boolean.TRUE.equals(r[2]) || "true".equals(String.valueOf(r[2]))
                                  ? "Da"
                                  : "Nu",
                          "method", r[5] != null ? r[5].toString() : "—"))
              .collect(Collectors.toList());

      model.addAttribute("reportRows", rows);
    }

    addCurrentUser(model, principal);
    return "rapoarte";
  }

  // ─── Setări ───────────────────────────────────────────────────────────────
  @GetMapping("/settings")
  public String settings(Model model, @AuthenticationPrincipal UserDetails principal) {
    if (principal != null) {
      userRepository
          .findByEmail(principal.getUsername())
          .ifPresent(
              u -> {
                model.addAttribute("currentUserName", u.getFullName());
                model.addAttribute("currentUserEmail", u.getEmail());
                model.addAttribute(
                    "currentUserRoles",
                    u.getRoles().stream().map(r -> r.getName()).collect(Collectors.joining(", ")));
              });
    }
    return "settings";
  }

  // ─── Helper ───────────────────────────────────────────────────────────────
  private void addCurrentUser(Model model, UserDetails principal) {
    if (principal != null) {
      userRepository
          .findByEmail(principal.getUsername())
          .ifPresent(
              u -> {
                model.addAttribute("currentUserName", u.getFullName());
              });
    }
  }
}
