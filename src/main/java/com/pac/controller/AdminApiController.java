package com.pac.controller;

import java.time.OffsetDateTime;
import java.time.LocalTime;
import org.springframework.format.annotation.DateTimeFormat;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.pac.dto.EmployeeDto;
import com.pac.entity.AccessLog;
import com.pac.entity.AccessSchedule;
import com.pac.entity.Employee;
import com.pac.repository.AccessLogRepository;
import com.pac.repository.EmployeeRepository;
import com.pac.repository.ScheduleRepository;
import com.pac.service.EmployeeService;

import lombok.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminApiController {

    private final EmployeeService employeeService;
    private final EmployeeRepository employeeRepository;
    private final ScheduleRepository scheduleRepository;
    private final AccessLogRepository accessLogRepository;

    // Employee CRUD

    @GetMapping("/employees")
    public ResponseEntity<List<Employee>> getAllEmployees() {
        return ResponseEntity.ok(employeeService.getAllEmployees());
    }

    @GetMapping("/employees/{id}")
    public ResponseEntity<Employee> getEmployeeById(@PathVariable UUID id) {
        return employeeService.getEmployeeById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/employees")
    public ResponseEntity<Employee> createEmployee(@RequestBody EmployeeDto dto) {
        return ResponseEntity.ok(employeeService.addEmployee(dto));
    }

    @PutMapping("/employees/{id}")
    public ResponseEntity<Employee> updateEmployee(@PathVariable UUID id, @RequestBody EmployeeDto dto) {
        try {
            return ResponseEntity.ok(employeeService.updateEmployee(id, dto));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/employees/{id}")
    public ResponseEntity<Void> deleteEmployee(@PathVariable UUID id) {
        employeeService.deleteEmployee(id);
        return ResponseEntity.noContent().build();
    }

    // Schedule management

    @PostMapping("/schedules")
    public ResponseEntity<?> createSchedule(@RequestBody ScheduleRequest request) {
        Employee employee = employeeRepository.findById(request.getEmployeeId())
                .orElseThrow(() -> new IllegalArgumentException("Employee not found: " + request.getEmployeeId()));

        AccessSchedule schedule = AccessSchedule.builder()
                .employee(employee)
                .dayOfWeek(request.getDayOfWeek())
                .timeFrom(LocalTime.parse(request.getTimeFrom()))
                .timeTo(LocalTime.parse(request.getTimeTo()))
                .build();

        scheduleRepository.save(schedule);
        return ResponseEntity.ok(schedule);
    }

    @GetMapping("/schedules/employee/{employeeId}")
    public ResponseEntity<List<AccessSchedule>> getEmployeeSchedules(@PathVariable UUID employeeId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found: " + employeeId));
        return ResponseEntity.ok(scheduleRepository.findByEmployee(employee));
    }

    @DeleteMapping("/schedules/{id}")
    public ResponseEntity<Void> deleteSchedule(@PathVariable UUID id) {
        scheduleRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // Reporting

    @GetMapping("/reports/logs")
    public ResponseEntity<List<AccessLog>> getAccessLogs(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime end,
            @RequestParam(required = false) UUID employeeId) {

        List<AccessLog> logs = employeeId != null
                ? accessLogRepository.findByEmployeeIdAndEventAtBetween(employeeId, start, end)
                : accessLogRepository.findByEventAtBetween(start, end);

        return ResponseEntity.ok(logs);
    }

    @Getter @Setter @NoArgsConstructor
    public static class ScheduleRequest {
        private UUID employeeId;
        private Integer dayOfWeek; // 0=Sun … 6=Sat, null=every day
        private String timeFrom;   // HH:mm
        private String timeTo;     // HH:mm
    }
}
