package com.pac.controller;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pac.entity.AccessDirection;
import com.pac.entity.AccessLog;
import com.pac.entity.AccessResult;
import com.pac.entity.Employee;
import com.pac.entity.AccessSchedule;
import com.pac.repository.AccessLogRepository;
import com.pac.repository.EmployeeRepository;
import com.pac.repository.ScheduleRepository;
import com.pac.service.EmployeeService;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@RestController
@RequestMapping("/api/gate")
@RequiredArgsConstructor
public class GateApiController {

	private final EmployeeRepository employeeRepository;
	private final EmployeeService employeeService;
	private final AccessLogRepository accessLogRepository;
	private final SimpMessagingTemplate messagingTemplate;
	private final ScheduleRepository scheduleRepository;

	@PostMapping("/authorize")
	public ResponseEntity<?> authorize(@RequestBody GateRequest request) {
		Optional<Employee> empOpt = Optional.empty();
		
		if (request.getBluetoothSecurityCode() != null && !request.getBluetoothSecurityCode().isEmpty()) {
			empOpt = employeeRepository.findByBluetoothSecurityCode(request.getBluetoothSecurityCode());
		} else if (request.getBadgeNumber() != null && !request.getBadgeNumber().isEmpty()) {
			empOpt = employeeRepository.findByBadgeNumber(request.getBadgeNumber());
		}

		if (empOpt.isEmpty()) {
			return ResponseEntity.status(404).body(new GateResponse("DENIED", "Employee not found"));
		}

		Employee employee = empOpt.get();
		boolean allowed = employeeService.canEnter(employee);
		AccessResult result = allowed ? AccessResult.GRANTED : AccessResult.DENIED;
		AccessDirection direction = "EXIT".equalsIgnoreCase(request.getDirection()) ? AccessDirection.EXIT : AccessDirection.ENTRY;

		// Save access log
		AccessLog log = AccessLog.builder()
				.employee(employee)
				.timestamp(LocalDateTime.now())
				.direction(direction)
				.result(result)
				.build();
		accessLogRepository.save(log);

		java.util.List<AccessSchedule> schedules = scheduleRepository.findByEmployee(employee);
		java.util.List<GateEvent.ScheduleInfo> scheduleData = schedules.stream()
				.map(s -> new GateEvent.ScheduleInfo(
						s.getDaysOfWeek().stream().map(Enum::name).collect(java.util.stream.Collectors.toSet()),
						s.getTimeFrom().toString(),
						s.getTimeTo().toString()
				))
				.toList();

		// Prepare WebSocket message for the Guard Monitor
		GateEvent wsMessage = new GateEvent(
				employee.getUser().getId().toString(),
				employee.getId().toString(),
				employee.getUser().getFirstName(),
				employee.getUser().getLastName(),
				employee.getCreatedAt().toString(),
				employee.getGrantedAccessAt() != null ? employee.getGrantedAccessAt().toString() : null,
				employee.isAccessActive(),
				employee.getDivisionId().toString(),
				employee.getBluetoothSecurityCode(),
				result.name(),
				direction.name(),
				LocalDateTime.now().toString(),
				scheduleData
		);

		// Broadcast to all listening guardians
		messagingTemplate.convertAndSend("/topic/monitor", wsMessage);

		return ResponseEntity.ok(new GateResponse(result.name(), allowed ? "Access granted" : "Access denied due to schedule or status"));
	}

	@Getter
	@Setter
	@NoArgsConstructor
	public static class GateRequest {
		private String bluetoothSecurityCode;
		private String badgeNumber;
		private String direction; // "ENTRY" or "EXIT"
	}

	@Getter
	@RequiredArgsConstructor
	public static class GateResponse {
		private final String status; // "GRANTED" or "DENIED"
		private final String message;
	}

	@Getter
	@RequiredArgsConstructor
	public static class GateEvent {
		private final String userId;
		private final String employeeId;
		private final String firstName;
		private final String lastName;
		private final String createdAt;
		private final String accessGrantedAt;
		private final boolean isAccessActive;
		private final String divisionId;
		private final String bluetoothSecurityCode;
		private final String result;
		private final String direction;
		private final String timestamp;
		private final java.util.List<ScheduleInfo> scheduleData;

		@Getter
		@RequiredArgsConstructor
		public static class ScheduleInfo {
			private final java.util.Set<String> daysOfWeek;
			private final String timeFrom;
			private final String timeTo;
		}
	}
}
