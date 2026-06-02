package com.pac.controller;

import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.pac.entity.Employee;
import com.pac.repository.EmployeeRepository;

import jakarta.servlet.http.HttpSession;

@Controller
public class UserWebController {

	private final EmployeeRepository employeeRepository;
	private final PasswordEncoder passwordEncoder;

	public UserWebController(EmployeeRepository employeeRepository, PasswordEncoder passwordEncoder) {
		this.employeeRepository = employeeRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@GetMapping("/users")
	public String users() {
		return "users";
	}

	@GetMapping("/login")
	public String showLoginPage() { return "login"; }

	@PostMapping("/users")
	public String saveUser(@ModelAttribute Employee employee) {
		employeeRepository.save(employee);
		return "redirect:/users";
	}

	@PostMapping("/login")
	public String login(@RequestParam String email, @RequestParam String parola, HttpSession session) {

		Employee employee = employeeRepository.findByUserEmail(email)
				.orElseThrow(() -> new UsernameNotFoundException(email));

		if (employee != null && passwordEncoder.matches(parola, employee.getUser().getPasswordHash())) {
			session.setAttribute("user", employee);
			return "redirect:/users";
		}

		return "redirect:/login";
	}
}