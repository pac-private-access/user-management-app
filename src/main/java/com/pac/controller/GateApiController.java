package com.pac.controller;

import java.time.OffsetDateTime;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import com.pac.entity.AccessLog;
import com.pac.entity.Employee;
import com.pac.repository.AccessLogRepository;
import com.pac.repository.EmployeeRepository;
import com.pac.service.EmployeeService;
import com.pac.service.GatePendingService;

import lombok.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/gate")
public class GateApiController {

  private static final Logger log = LoggerFactory.getLogger(GateApiController.class);

  private final EmployeeRepository employeeRepository;
  private final EmployeeService employeeService;
  private final AccessLogRepository accessLogRepository;
  private final SimpMessagingTemplate messagingTemplate;
  private final GatePendingService gatePendingService;

  // ─── Authorize (called by ESP32 and mobile app) ──────────────────────────

  @PostMapping("/authorize")
  public ResponseEntity<GateResponse> authorize(@RequestBody GatePackage pkg) {

    // Accept both "bluetoothSecurityCode" (new) and "data" (legacy ESP32 field)
    String code = pkg.resolveCode();

    Optional<Employee> empOpt = Optional.empty();
    if (code != null && !code.isEmpty()) {
      empOpt = employeeRepository.findByBluetoothSecurityCode(code);
    }

    if (empOpt.isEmpty()) {
      return ResponseEntity.status(404)
          .body(new GateResponse("DENIED", "Angajat negăsit", false));
    }

    Employee employee   = empOpt.get();
    boolean  allowed    = employeeService.canEnter(employee);
    boolean  outOfSched = !allowed && employee.isAccessActive();

    AccessLog entry = AccessLog.builder()
        .employeeId(employee.getId())
        .isAuthorized(allowed)
        .outOfSchedule(outOfSched)
        .eventAt(OffsetDateTime.now())
        .syncedToCloud(true)
        .build();
    accessLogRepository.save(entry);

    messagingTemplate.convertAndSend("/topic/monitor",
        new GateEvent(
            employee.getId().toString(),
            employee.getFirstName(),
            employee.getLastName(),
            employee.getBadgeNumber(),
            allowed ? "GRANTED" : "DENIED",
            OffsetDateTime.now().toString()));

    // When access comes from mobile/web (not the ESP32 itself), queue a gate-open
    // command. The ESP32 picks it up on its next poll (/api/gate/poll).
    if (allowed && !"ESP32_PAC".equalsIgnoreCase(pkg.getDeviceId())) {
      gatePendingService.request();
      log.info("Gate open queued for mobile/web trigger");
    }

    // Return "OK" for granted — ESP32 firmware checks indexOf("OK") != -1
    return ResponseEntity.ok(
        new GateResponse(
            allowed ? "OK" : "DENIED",
            allowed ? "Acces permis" : "Acces refuzat",
            allowed));
  }

  // ─── ESP32 polling endpoint ───────────────────────────────────────────────

  @GetMapping("/poll")
  public ResponseEntity<String> poll() {
    if (gatePendingService.consume()) {
      log.info("Gate open command dispatched via poll");
      return ResponseEntity.ok("{\"command\":\"OPEN\"}");
    }
    return ResponseEntity.ok("{\"command\":\"NONE\"}");
  }

  // ─── ESP32 plain-text receive ─────────────────────────────────────────────

  @PostMapping("/code")
  public ResponseEntity<Void> receive(@RequestBody String message) {
    log.info("ESP32 message received: {}", message);
    return ResponseEntity.ok().build();
  }

  // ─── DTOs ─────────────────────────────────────────────────────────────────

  @Getter @Setter @NoArgsConstructor
  public static class GatePackage {
    private String deviceId;
    private String bluetoothSecurityCode;
    private String data; // legacy field name used by current ESP32 firmware

    /** Returns whichever code field is present. */
    public String resolveCode() {
      return (bluetoothSecurityCode != null && !bluetoothSecurityCode.isEmpty())
          ? bluetoothSecurityCode : data;
    }
  }

  @Getter @RequiredArgsConstructor
  public static class GateResponse {
    private final String  status;
    private final String  message;
    private final boolean openGate;
  }

  @Getter @RequiredArgsConstructor
  public static class GateEvent {
    private final String employeeId;
    private final String firstName;
    private final String lastName;
    private final String badgeNumber;
    private final String result;
    private final String timestamp;
  }
}
