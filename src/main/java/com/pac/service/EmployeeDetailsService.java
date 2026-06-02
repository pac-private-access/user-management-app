package com.pac.service;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.pac.entity.Employee;
import com.pac.repository.EmployeeRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmployeeDetailsService implements UserDetailsService {
	private final EmployeeRepository employeeRepository;

	@Override
	public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
		Employee employee = employeeRepository.findByUserEmail(email)
				.orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
	
		return new User(
				employee.getUser().getEmail(),
				employee.getUser().getPasswordHash(),
				employee.getRoles().stream()
						.map(role -> new SimpleGrantedAuthority("ROLE_" + role.name()))
						.toList()
			);
	}
}
