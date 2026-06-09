package com.pac;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.OffsetDateTime;
import java.util.Random;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.pac.controller.GateApiController;
import com.pac.controller.GateApiController.GateRequest;
import com.pac.entity.Employee;
import com.pac.repository.AccessLogRepository;
import com.pac.repository.EmployeeRepository;
import com.pac.repository.ScheduleRepository;
import com.pac.repository.UserRepository;
import com.pac.service.EmployeeService;

import lombok.experimental.var;

@ExtendWith(MockitoExtension.class)
class DemoApplicationTests {
	
	@Mock
	private AccessLogRepository accessLogRepo;
	
	@Mock
	private EmployeeRepository employeeRepo;
	
	@Mock
	private ScheduleRepository scheduleRepo;
	
	@Mock
	private UserRepository userRepo;
	
	@InjectMocks
	private GateApiController controller;
	
	@InjectMocks
	private EmployeeService employeeService;
	
	@Test
	void EmployeeEntry() {
		Employee emp = Employee.builder()
				.firstName("Ion")
				.lastName("Palasca")
				.cnp("12345678910111")
				.badgeNumber("123")
				.divisionId(new UUID(new Random().nextLong(), new Random().nextLong()))
				.bluetoothSecurityCode("456")
				.isAccessActive(true)
				.createdAt(OffsetDateTime.now())
				.updatedAt(OffsetDateTime.now())
				.build();
		employeeRepo.save(emp);
		
		var resp = controller.authorize(GateRequest.builder()
				.bluetoothSecurityCode(emp.getBluetoothSecurityCode())
				.accessMethod("Car")
				.badgeNumber(emp.getBadgeNumber())
				.carPlate("GJ 06 LAY")
				.direction("OUT")
				.build()
				);
		
		assertNotNull(resp);
		assertEquals(HttpStatus.OK, resp.getStatusCode());
	}
}
