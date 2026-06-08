package com.pac.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pac.dto.EmployeeDto;
import com.pac.entity.AccessSchedule;
import com.pac.entity.Employee;
import com.pac.entity.User;
import com.pac.repository.EmployeeRepository;
import com.pac.repository.ScheduleRepository;

@Service
public class EmployeeService {

	private final EmployeeRepository employeeRepo;
	private final ScheduleRepository scheduleRepo;
	private final PasswordEncoder passwordEncoder;

	public EmployeeService(EmployeeRepository employeeRepo, ScheduleRepository scheduleRepo, PasswordEncoder passwordEncoder) {
		this.employeeRepo = employeeRepo;
		this.scheduleRepo = scheduleRepo;
		this.passwordEncoder = passwordEncoder;
	}

	public boolean canEnter(Employee employee) {
		if (employee == null || !employee.isAccessActive()) {
			return false;
		}

		LocalDateTime now = LocalDateTime.now();
		java.time.LocalTime currentTime = now.toLocalTime();
		List<AccessSchedule> schedules = scheduleRepo.findByEmployee(employee);

		for (AccessSchedule schedule : schedules) {
			if (schedule.getDaysOfWeek().contains(now.getDayOfWeek())
					&& !currentTime.isBefore(schedule.getTimeFrom())
					&& !currentTime.isAfter(schedule.getTimeTo())) {
				return true;
			}
		}
		return false;
	}

	@Transactional
	public Employee addEmployee(EmployeeDto dto) {
		User user = User.builder()
				.email(dto.getEmail())
				.passwordHash(passwordEncoder.encode(dto.getPassword()))
				.firstName(dto.getFirstName())
				.lastName(dto.getLastName())
				.build();

		Employee emp = Employee.builder()
				.user(user)
				.cnp(dto.getCnp())
				.badgeNumber(dto.getBadgeNumber())
				.bluetoothSecurityCode(dto.getBluetoothSecurityCode())
				.carPlate(dto.getCarPlate())
				.divisionId(dto.getDivisionId())
				.isAccessActive(dto.isAccessActive())
				.createdAt(LocalDateTime.now())
				.roles(dto.getRoles())
				.build();

		if (dto.isAccessActive()) {
			emp.setGrantedAccessAt(LocalDateTime.now());
		}

		return employeeRepo.save(emp);
	}

	@Transactional
	public Employee updateEmployee(UUID id, EmployeeDto dto) {
		Employee emp = employeeRepo.findById(id)
				.orElseThrow(() -> new IllegalArgumentException("Employee not found with id: " + id));

		emp.getUser().setEmail(dto.getEmail());
		if (dto.getPassword() != null && !dto.getPassword().isEmpty()) {
			emp.getUser().setPasswordHash(passwordEncoder.encode(dto.getPassword()));
		}
		emp.getUser().setFirstName(dto.getFirstName());
		emp.getUser().setLastName(dto.getLastName());

		emp.setCnp(dto.getCnp());
		emp.setBadgeNumber(dto.getBadgeNumber());
		emp.setBluetoothSecurityCode(dto.getBluetoothSecurityCode());
		emp.setCarPlate(dto.getCarPlate());
		emp.setDivisionId(dto.getDivisionId());
		emp.setAccessActive(dto.isAccessActive());
		emp.setRoles(dto.getRoles());

		if (dto.isAccessActive() && emp.getGrantedAccessAt() == null) {
			emp.setGrantedAccessAt(LocalDateTime.now());
		} else if (!dto.isAccessActive()) {
			emp.setGrantedAccessAt(null);
		}

		return employeeRepo.save(emp);
	}

	public List<Employee> getAllEmployees() {
		return employeeRepo.findAll();
	}

	public Optional<Employee> getEmployeeById(UUID id) {
		return employeeRepo.findById(id);
	}

	@Transactional
	public void deleteEmployee(UUID id) {
		employeeRepo.deleteById(id);
	}
}
