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

  /**
   * Receives a gate request from the ESP32 or mobile app.
   * Looks up the employee by Bluetooth security code (preferred) or badge number,
   * evaluates access schedules, logs the event, broadcasts via WebSocket,
   * and if access is granted fires an async HTTP call to the ESP32 to open the gate.
   */
  @PostMapping("/authorize")
  public ResponseEntity<GateResponse> authorize(@RequestBody GateRequest request) {

    Optional<Employee> empOpt = Optional.empty();

    if (request.getBluetoothSecurityCode() != null
        && !request.getBluetoothSecurityCode().isEmpty()) {
      empOpt = employeeRepository.findByBluetoothSecurityCode(
          request.getBluetoothSecurityCode());
    } else if (request.getBadgeNumber() != null && !request.getBadgeNumber().isEmpty()) {
      empOpt = employeeRepository.findByBadgeNumber(request.getBadgeNumber());
    }

    if (empOpt.isEmpty()) {
      return ResponseEntity.status(404)
          .body(new GateResponse("DENIED", "Angajat negăsit", false));
    }

    Employee employee  = empOpt.get();
    boolean  allowed   = employeeService.canEnter(employee);
    boolean  outOfSched = !allowed && employee.isAccessActive();
    String   eventType  = "EXIT".equalsIgnoreCase(request.getDirection()) ? "exit" : "entry";
    String   accessMethod =
        request.getAccessMethod() != null ? request.getAccessMethod() : "bluetooth_pc";

    AccessLog entry = AccessLog.builder()
        .employeeId(employee.getId())
        .eventType(eventType)
        .accessMethod(accessMethod)
        .isAuthorized(allowed)
        .outOfSchedule(outOfSched)
        .carPlateSeen(request.getCarPlate())
        .eventAt(OffsetDateTime.now())
        .syncedToCloud(true)
        .build();
    accessLogRepository.save(entry);

    // Broadcast to live-monitor WebSocket subscribers
    messagingTemplate.convertAndSend("/topic/monitor",
        new GateEvent(
            employee.getId().toString(),
            employee.getFirstName(),
            employee.getLastName(),
            employee.getBadgeNumber(),
            allowed ? "GRANTED" : "DENIED",
            eventType.toUpperCase(),
            accessMethod,
            OffsetDateTime.now().toString()));

    // If the request came from the mobile app (not the ESP32 itself),
    // set a pending flag — the ESP32 will pick it up on its next poll.
    if (allowed && !"bluetooth_esp32".equalsIgnoreCase(accessMethod)) {
      gatePendingService.request();
    }

    return ResponseEntity.ok(
        new GateResponse(
            allowed ? "GRANTED" : "DENIED",
            allowed ? "Acces permis" : "Acces refuzat",
            allowed));
  }

  // ─── ESP32 receive (plain text from ESP32) ────────────────────────────────

  @PostMapping("/code")
  public ResponseEntity<Void> receive(@RequestBody String message) {
    log.info("ESP32 message received: {}", message);
    return ResponseEntity.ok().build();
  }

  // ─── ESP32 polling endpoint ───────────────────────────────────────────────

  /**
   * Called by the ESP32 every 2 seconds. Returns OPEN once when a gate command
   * is pending (triggered by mobile app or the web "Deschide poarta" button),
   * then clears the flag so the gate only opens once per command.
   */
  @GetMapping("/poll")
  public ResponseEntity<String> poll() {
    if (gatePendingService.consume()) {
      log.info("ESP32 poll: OPEN command dispatched");
      return ResponseEntity.ok("{\"command\":\"OPEN\"}");
    }
    return ResponseEntity.ok("{\"command\":\"NONE\"}");
  }

  // ─── DTOs ─────────────────────────────────────────────────────────────────

  @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
  public static class GateRequest {
    private String bluetoothSecurityCode;
    private String badgeNumber;
    private String direction;
    /** "bluetooth_esp32" | "mobile" | "bluetooth_pc" */
    private String accessMethod;
    private String carPlate;
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
    private final String direction;
    private final String accessMethod;
    private final String timestamp;
  }
}
