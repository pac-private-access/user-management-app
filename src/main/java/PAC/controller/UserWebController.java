package PAC.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import PAC.model.Employee;
import PAC.repository.EmployeeRepository;
import jakarta.servlet.http.HttpSession;
/*
 * Controller for managing data flow to the web frontend,
 * 
 * NOT to be confused with controller that talks to 
 * the database API endpoints
*/
@Controller
public class UserWebController {

	private final EmployeeRepository userRepository;

	public UserWebController(EmployeeRepository userRepository) { this.userRepository = userRepository; }

	@GetMapping("/users")
	public String users() {
		/*
		 * if (session.getAttribute("user") == null) { return "redirect:/login"; }
		 */
		return "users";
	}

	@GetMapping("/login")
	public String showLoginPage() { return "login"; }

	@PostMapping("/users")
	public String saveUser(@ModelAttribute Employee user) {
		userRepository.save(user);
		return "redirect:/users";
	}

	@PostMapping("/login")
	public String login(@RequestParam String email, @RequestParam String parola, HttpSession session) {

		Employee user = userRepository.findByEmail(email);

		if (user != null && user.getPassword().equals(parola)) {
			session.setAttribute("user", user);
			return "redirect:/users";
		}

		return "redirect:/login";
	}
}