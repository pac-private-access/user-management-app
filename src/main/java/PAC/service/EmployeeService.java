package PAC.service;

import org.springframework.stereotype.Service;

import PAC.dto.EmployeeDto;
import PAC.model.Employee;

@Service
public class EmployeeService {
	public boolean addEmployee(EmployeeDto dto) {
		Employee emp = new Employee();
		return false;
	}
}
