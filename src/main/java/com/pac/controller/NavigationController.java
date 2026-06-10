package com.pac.controller;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.pac.repository.AccessLogRepository;
import com.pac.repository.EmployeeRepository;
import com.pac.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class NavigationController {

  private static final Logger log = LoggerFactory.getLogger(NavigationController.class);

  private final EmployeeRepository employeeRepository;
  private final AccessLogRepository accessLogRepository;
  private final UserRepository userRepository;

  @Value("${esp32.url:http://192.168.4.1}")
  private String esp32Url;

  // ─── Dashboard ───────────────────────────────────────────────────────────
  @GetMapping("/dashboard")
  public String dashboard(Model model) {
    OffsetDateTime now          = OffsetDateTime.now(ZoneOffset.UTC);
    OffsetDateTime startOfToday = now.toLocalDate().atStartOfDay().atOffset(ZoneOffset.UTC);
    OffsetDateTime endOfToday   = startOfToday.plusDays(1);
    OffsetDateTime sevenDaysAgo = startOfToday.minusDays(6);

    model.addAttribute("totalAngajati",  employeeRepository.count());
    model.addAttribute("activeWebUsers", userRepository.countActive(true));
    model.addAttribute("intrariAzi",
        accessLogRepository.countEntries("entry", startOfToday, endOfToday));
    model.addAttribute("iesiriAzi",
        accessLogRepository.countEntries("exit", startOfToday, endOfToday));
    model.addAttribute("refuzateAzi",
        accessLogRepository.countEntriesByAuth("entry", false, startOfToday, endOfToday));
    model.addAttribute("inCladireAcum",
        accessLogRepository.countEmployeesCurrentlyInside());

    List<Object[]> perDay = accessLogRepository.countEntriesPerDayAfter(sevenDaysAgo);
    model.addAttribute("chartLabels",
        perDay.stream().map(r -> r[1].toString()).collect(Collectors.toList()));
    model.addAttribute("chartValues",
        perDay.stream().map(r -> ((Number) r[0]).longValue()).collect(Collectors.toList()));

    model.addAttribute("recentActivity", accessLogRepository.findRecentActivity());
    return "dashboard";
  }

  // ─── Acces în timp real ───────────────────────────────────────────────────
  @GetMapping("/accestimpreal")
  public String accestimpreal() {
    return "accestimpreal";
  }

  // ─── Rapoarte ─────────────────────────────────────────────────────────────
  @GetMapping("/rapoarte")
  public String rapoarte(
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
      Model model) {

    model.addAttribute("from", from);
    model.addAttribute("to", to);

    if (from != null && to != null) {
      OffsetDateTime start = from.atStartOfDay().atOffset(ZoneOffset.UTC);
      OffsetDateTime end   = to.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);

      DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
      List<Object[]> raw = accessLogRepository.findReportData(start, end);
      List<Map<String, String>> rows = raw.stream()
          .map(r -> Map.of(
              "time",       r[0] instanceof OffsetDateTime
                              ? ((OffsetDateTime) r[0]).atZoneSameInstant(ZoneOffset.UTC).format(fmt)
                              : r[0].toString(),
              "employee",   r[3] != null ? r[3].toString() : "Necunoscut",
              "division",   r[4] != null ? r[4].toString() : "—",
              "type",       "entry".equals(r[1]) ? "Intrare" : "Ieșire",
              "authorized", Boolean.TRUE.equals(r[2]) || "true".equals(String.valueOf(r[2])) ? "Da" : "Nu",
              "method",     r[5] != null ? r[5].toString() : "—"))
          .collect(Collectors.toList());
      model.addAttribute("reportRows", rows);
    }
    return "rapoarte";
  }

  // ─── Setări ───────────────────────────────────────────────────────────────
  @GetMapping("/settings")
  public String settings(Model model, @AuthenticationPrincipal UserDetails principal) {
    if (principal != null) {
      userRepository.findByEmail(principal.getUsername()).ifPresent(u -> {
        model.addAttribute("currentUserEmail", u.getEmail());
        model.addAttribute("currentUserRoles",
            u.getRoles().stream().map(r -> r.getName()).collect(Collectors.joining(", ")));
      });
    }
    return "settings";
  }

  // ─── Manual gate trigger (web-session authenticated) ─────────────────────
  /**
   * Sends GET {esp32Url}/send_bt?text=OK to open the gate.
   * Called by the "Deschide poarta" button in accestimpreal.html via fetch().
   */
//  @PostMapping("/gate/trigger")
//  @ResponseBody
//  public ResponseEntity<String> triggerGate() {
//    try {
//      HttpClient client = HttpClient.newBuilder()
//          .connectTimeout(Duration.ofSeconds(5))
//          .build();
//      HttpRequest req = HttpRequest.newBuilder()
//          .uri(URI.create(esp32Url + "/send_bt?text=OK"))
//          .timeout(Duration.ofSeconds(5))
//          .GET()
//          .build();
//      HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
//      log.info("Manual gate trigger — ESP32 HTTP {}", resp.statusCode());
//      return ResponseEntity.ok("Poarta a fost deschisă.");
//    } catch (Exception e) {
//      log.warn("Manual gate trigger — ESP32 unreachable: {}", e.getMessage());
//      return ResponseEntity.status(502).body("ESP32 inaccesibil: " + e.getMessage());
//    }
//  }
}
