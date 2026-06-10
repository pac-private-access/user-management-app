package com.pac.controller;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pac.repository.DivisionRepository;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.pac.entity.AccessLog;
import com.pac.entity.AccessSchedule;
import com.pac.repository.AccessLogRepository;
import com.pac.repository.EmployeeRepository;
import com.pac.repository.ScheduleRepository;

import lombok.*;

@RestController
@RequestMapping("/api/mobile")
@RequiredArgsConstructor
public class MobileApiController {

  private final EmployeeRepository employeeRepository;
  private final AccessLogRepository accessLogRepository;
  private final ScheduleRepository scheduleRepository;
  private final DivisionRepository divisionRepository;

  // ─── Profil angajat ───────────────────────────────────────────────────────
  @GetMapping("/profile/{employeeId}")
  public ResponseEntity<?> getProfile(@PathVariable UUID employeeId) {
    return employeeRepository
            .findById(employeeId)
            .map(emp -> {
              List<AccessSchedule> schedules = scheduleRepository.findByEmployee(emp);

              String orar = buildOrarString(schedules);

              String validTo = schedules.stream()
                      .filter(s -> s.getValidTo() != null)
                      .map(s -> s.getValidTo().toString())
                      .findFirst()
                      .orElse(null);

              String divisionName = emp.getDivisionId() != null
                      ? divisionRepository.findById(emp.getDivisionId())
                      .map(d -> d.getName())
                      .orElse("—")
                      : "—";

              return ResponseEntity.ok(new ProfileResponse(
                      emp.getId().toString(),
                      emp.getFirstName() + " " + emp.getLastName(),
                      emp.getBadgeNumber(),
                      emp.getCnp(),
                      divisionName,
                      orar,
                      emp.getAccessGrantedByBadge(),
                      validTo,
                      emp.getCarPlate(),
                      emp.getBluetoothSecurityCode(),
                      emp.getPhotoUrl(),
                      emp.isAccessActive()
              ));
            })
            .orElse(ResponseEntity.notFound().build());
  }

  // ─── Raport acces lunar ───────────────────────────────────────────────────
  @GetMapping("/report/{employeeId}")
  public ResponseEntity<List<ReportEntry>> getReport(
          @PathVariable UUID employeeId,
          @RequestParam(required = false)
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
          @RequestParam(required = false)
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

    OffsetDateTime start = (from != null ? from : LocalDate.now().withDayOfMonth(1))
            .atStartOfDay().atOffset(ZoneOffset.UTC);
    OffsetDateTime end = (to != null ? to.plusDays(1) : LocalDate.now().plusDays(1))
            .atStartOfDay().atOffset(ZoneOffset.UTC);

    List<AccessLog> logs = accessLogRepository
            .findByEmployeeIdAndEventAtBetween(employeeId, start, end);

    List<ReportEntry> result = logs.stream()
            .map(log -> new ReportEntry(
                    log.getId().toString(),
                    "entry".equals(log.getEventType()) ? "Intrare" : "Iesire",
                    log.getAccessMethod(),
                    log.isAuthorized(),
                    log.isOutOfSchedule(),
                    log.getEventAt().toString(),
                    log.isSyncedToCloud()
            ))
            .collect(Collectors.toList());

    return ResponseEntity.ok(result);
  }

  // ─── Helper orar ──────────────────────────────────────────────────────────
  private String buildOrarString(List<AccessSchedule> schedules) {
    if (schedules == null || schedules.isEmpty()) return "Nespecificat";

    String[] dayNames = {"Dum", "Lun", "Mar", "Mie", "Joi", "Vin", "Sâm"};

    return schedules.stream()
            .collect(Collectors.groupingBy(
                    s -> s.getTimeFrom() + "-" + s.getTimeTo(),
                    Collectors.mapping(s -> {
                      if (s.getDayOfWeek() == null) return "Zilnic";
                      int day = s.getDayOfWeek();
                      return (day >= 0 && day <= 6) ? dayNames[day] : "Z" + day;
                    }, Collectors.joining(", "))
            ))
            .entrySet().stream()
            .map(e -> e.getValue() + " " + e.getKey())
            .collect(Collectors.joining(" | "));
  }

  // ─── DTO-uri ──────────────────────────────────────────────────────────────
  @Getter
  @AllArgsConstructor
  public static class ProfileResponse {
    private final String employeeId;
    private final String numeComplet;
    private final String badgeNumber;
    private final String cnp;
    private final String divisie;
    private final String orarPermis;
    private final String acordatDeBadge;
    private final String valabilPana;
    private final String carPlate;
    private final String bluetoothCode;
    private final String photoUrl;

    @JsonProperty("isAccessActive")
    private final boolean isAccessActive;
  }

  @Getter
  @AllArgsConstructor
  public static class ReportEntry {
    private final String id;
    private final String eventType;
    private final String accessMethod;
    private final boolean isAuthorized;
    private final boolean outOfSchedule;
    private final String eventAt;
    private final boolean synced;
  }
}
