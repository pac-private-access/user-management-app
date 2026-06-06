package com.pac.controller;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.pac.dto.EmployeeDto;
import com.pac.entity.AccessLog;
import com.pac.entity.AccessSchedule;
import com.pac.entity.Employee;
import com.pac.repository.AccessLogRepository;
import com.pac.repository.EmployeeRepository;
import com.pac.repository.ScheduleRepository;
import com.pac.service.EmployeeService;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

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
		Employee employee = employeeService.addEmployee(dto);
		return ResponseEntity.ok(employee);
	}

	@PutMapping("/employees/{id}")
	public ResponseEntity<Employee> updateEmployee(@PathVariable UUID id, @RequestBody EmployeeDto dto) {
		try {
			Employee employee = employeeService.updateEmployee(id, dto);
			return ResponseEntity.ok(employee);
		} catch (IllegalArgumentException e) {
			return ResponseEntity.notFound().build();
		}
	}

	@DeleteMapping("/employees/{id}")
	public ResponseEntity<Void> deleteEmployee(@PathVariable UUID id) {
		employeeService.deleteEmployee(id);
		return ResponseEntity.noContent().build();
	}

	// Schedule Management

	@PostMapping("/schedules")
	public ResponseEntity<?> createSchedule(@RequestBody ScheduleRequest request) {
		Employee employee = employeeRepository.findById(request.getEmployeeId())
				.orElseThrow(() -> new IllegalArgumentException("Employee not found with ID: " + request.getEmployeeId()));

		AccessSchedule schedule = AccessSchedule.builder()
				.employee(employee)
				.daysOfWeek(request.getDaysOfWeek())
				.timeFrom(LocalTime.parse(request.getTimeFrom()))
				.timeTo(LocalTime.parse(request.getTimeTo()))
				.build();

		scheduleRepository.save(schedule);
		return ResponseEntity.ok(schedule);
	}

	@GetMapping("/schedules/employee/{employeeId}")
	public ResponseEntity<List<AccessSchedule>> getEmployeeSchedules(@PathVariable UUID employeeId) {
		Employee employee = employeeRepository.findById(employeeId)
				.orElseThrow(() -> new IllegalArgumentException("Employee not found with ID: " + employeeId));
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
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
			@RequestParam(required = false) UUID employeeId) {
		
		List<AccessLog> logs;
		if (employeeId != null) {
			logs = accessLogRepository.findByEmployeeIdAndTimestampBetween(employeeId, start, end);
		} else {
			logs = accessLogRepository.findByTimestampBetween(start, end);
		}
		return ResponseEntity.ok(logs);
	}

	@Getter
	@Setter
	@NoArgsConstructor
	public static class ScheduleRequest {
		private UUID employeeId;
		private java.util.Set<java.time.DayOfWeek> daysOfWeek;
		private String timeFrom; // HH:mm
		private String timeTo;   // HH:mm
	}
}
