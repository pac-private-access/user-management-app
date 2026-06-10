package com.pac.controller;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import com.pac.entity.AccessLog;
import com.pac.entity.Employee;
import com.pac.repository.AccessLogRepository;
import com.pac.repository.EmployeeRepository;
import com.pac.service.EmployeeService;

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

  /** Base URL of the ESP32 web server (SoftAP default: 192.168.4.1). */
  @Value("${esp32.url:http://192.168.4.1}")
  private String esp32Url;

  // ─── Authorize (called by ESP32 and mobile app) ──────────────────────────

  /**
   * Receives a gate request from the ESP32 or mobile app.
   * Looks up the employee by Bluetooth security code (preferred) or badge number,
   * evaluates access schedules, logs the event, broadcasts via WebSocket,
   * and if access is granted fires an async HTTP call to the ESP32 to open the gate.
   */
  @PostMapping("/authorize")
  public ResponseEntity<GateResponse> authorize(@RequestBody GatePackage packageReceived) {
	
    Optional<Employee> empOpt = Optional.empty();

    if (packageReceived.getBluetoothSecurityCode() != null
        && !packageReceived.getBluetoothSecurityCode().isEmpty()) {
      empOpt = employeeRepository.findByBluetoothSecurityCode(
    		  packageReceived.getBluetoothSecurityCode());
    }
    
    if (empOpt.isEmpty()) {
      return ResponseEntity.status(404)
          .body(new GateResponse("DENIED", "Angajat negăsit", false));
    }

    Employee employee  = empOpt.get();
    boolean  allowed   = employeeService.canEnter(employee);
    boolean  outOfSched = !allowed && employee.isAccessActive();

    AccessLog entry = AccessLog.builder()
        .employeeId(employee.getId())
        .isAuthorized(allowed)
        .outOfSchedule(outOfSched)
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
            OffsetDateTime.now().toString()));

    // If the request came from the mobile app (not the ESP32 itself),
    // command the ESP32 to open the gate asynchronously.
    if (allowed && !"ESP32_PAC".equalsIgnoreCase(packageReceived.getDeviceId())) {
      CompletableFuture.runAsync(this::openEsp32Gate);
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

  // ─── Internal: command ESP32 to open gate ────────────────────────────────

  /**
   * Sends GET {esp32Url}/send_bt?text=OK to the ESP32.
   * Uses Java's built-in HttpClient with a 5-second timeout so it never
   * hangs the calling thread for long.
   */
  private void openEsp32Gate() {
    try {
      HttpClient client = HttpClient.newBuilder()
          .connectTimeout(Duration.ofSeconds(5))
          .build();
      HttpRequest req = HttpRequest.newBuilder()
          .uri(URI.create("OK"))
          .timeout(Duration.ofSeconds(5))
          .GET()
          .build();
      HttpResponse<String> resp =
          client.send(req, HttpResponse.BodyHandlers.ofString());
      log.info("ESP32 gate command sent — HTTP {}", resp.statusCode());
    } catch (Exception e) {
      log.warn("Could not reach ESP32 at {}: {}", esp32Url, e.getMessage());
    }
  }

  // ─── DTOs ─────────────────────────────────────────────────────────────────
  @Getter @Setter @NoArgsConstructor
  public static class GatePackage{
	  private String deviceId;
	  private String bluetoothSecurityCode;
  }
  
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
    private final String timestamp;
  }
}
