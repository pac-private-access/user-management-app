package com.pac.controller;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    private final EmployeeRepository employeeRepo;
    private final AccessLogRepository logRepo;
    private final UserRepository userRepo;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime startOfToday = now.toLocalDate().atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime endOfToday = startOfToday.plusDays(1);

        model.addAttribute("activeEmployees", employeeRepo.countByIsAccessActive(true));
        model.addAttribute("enteredToday", logRepo.countEntries("entry", startOfToday, endOfToday));
        model.addAttribute("inBuildingNow", logRepo.countEmployeesCurrentlyInside());

        long approvedToday = logRepo.countEntriesByAuth("entry", true, startOfToday, endOfToday);
        long refusedToday  = logRepo.countEntriesByAuth("entry", false, startOfToday, endOfToday);
        model.addAttribute("approvedToday", approvedToday);
        model.addAttribute("refusedToday", refusedToday);

        OffsetDateTime sevenDaysAgo = startOfToday.minusDays(6);
        List<Object[]> dailyCounts = logRepo.countEntriesPerDayAfter(sevenDaysAgo);
        long[] weeklyData = buildWeeklyData(dailyCounts, sevenDaysAgo, 7);
        model.addAttribute("weeklyData", weeklyData);

        DateTimeFormatter labelFmt = DateTimeFormatter.ofPattern("dd.MM");
        String[] weeklyLabels = new String[7];
        for (int i = 0; i < 7; i++) {
            weeklyLabels[i] = sevenDaysAgo.plusDays(i).toLocalDate().format(labelFmt);
        }
        model.addAttribute("weeklyLabels", weeklyLabels);

        List<Object[]> rawActivity = logRepo.findRecentActivity();
        List<Map<String, String>> recentActivity = new ArrayList<>();
        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm");
        for (Object[] row : rawActivity) {
            OffsetDateTime eventAt = ((Instant) row[0]).atOffset(ZoneOffset.UTC);
            String eventType  = (String) row[1];
            boolean authorized = (Boolean) row[2];
            String name = (String) row[3];
            Map<String, String> entry = new LinkedHashMap<>();
            entry.put("name", name);
            entry.put("action", "entry".equals(eventType)
                    ? (authorized ? "Intrare" : "Intrare refuzată")
                    : (authorized ? "Ieșire" : "Ieșire refuzată"));
            entry.put("time", eventAt.format(timeFmt));
            recentActivity.add(entry);
        }
        model.addAttribute("recentActivity", recentActivity);

        return "dashboard";
    }

    @GetMapping("/accestimpreal")
    public String accestimpreal() { return "accestimpreal"; }

    @GetMapping("/rapoarte")
    public String rapoarte(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            Model model) {

        model.addAttribute("from", from != null ? from : "");
        model.addAttribute("to",   to   != null ? to   : "");

        if (from != null && !from.isBlank() && to != null && !to.isBlank()) {
            OffsetDateTime start = LocalDate.parse(from).atStartOfDay().atOffset(ZoneOffset.UTC);
            OffsetDateTime end   = LocalDate.parse(to).plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);

            List<Object[]> raw = logRepo.findReportData(start, end);
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
            List<Map<String, String>> rows = new ArrayList<>();
            for (Object[] r : raw) {
                OffsetDateTime eventAt = ((Instant) r[0]).atOffset(ZoneOffset.UTC);
                Map<String, String> row = new LinkedHashMap<>();
                row.put("time",       eventAt.format(fmt));
                row.put("type",       "entry".equals(r[1]) ? "Intrare" : "Ieșire");
                row.put("authorized", (Boolean) r[2] ? "Da" : "Nu");
                row.put("employee",   (String) r[3]);
                row.put("division",   (String) r[4]);
                row.put("method",     (String) r[5]);
                rows.add(row);
            }
            model.addAttribute("reportRows", rows);
        }

        return "rapoarte";
    }

    @GetMapping("/settings")
    public String settings(@AuthenticationPrincipal UserDetails principal, Model model) {
        userRepo.findByEmail(principal.getUsername()).ifPresent(user -> {
            model.addAttribute("currentUserName",  user.getFullName());
            model.addAttribute("currentUserEmail", user.getEmail());
            String roles = user.getRoles().isEmpty() ? "—"
                : user.getRoles().stream().map(r -> r.getName()).collect(java.util.stream.Collectors.joining(", "));
            model.addAttribute("currentUserRoles", roles);
        });
        return "settings";
    }

    private long[] buildWeeklyData(List<Object[]> dbRows, OffsetDateTime startDay, int days) {
        Map<String, Long> countByDay = new LinkedHashMap<>();
        for (Object[] row : dbRows) {
            countByDay.put(row[1].toString(), ((Number) row[0]).longValue());
        }
        long[] result = new long[days];
        for (int i = 0; i < days; i++) {
            String key = startDay.plusDays(i).toLocalDate().toString();
            result[i] = countByDay.getOrDefault(key, 0L);
        }
        return result;
    }
}
