package com.pac.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.pac.dto.EmployeeDto;
import com.pac.entity.Division;
import com.pac.entity.Employee;
import com.pac.repository.DivisionRepository;
import com.pac.repository.EmployeeRepository;
import com.pac.repository.UserRepository;
import com.pac.service.EmployeeService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class EmployeeWebController {

    private final EmployeeService employeeService;
    private final EmployeeRepository employeeRepository;
    private final DivisionRepository divisionRepository;
    private final UserRepository userRepository;

    @Value("${app.upload-dir:uploads}")
    private String uploadDir;

    // ─── List / form page ────────────────────────────────────────────────────

    @GetMapping("/angajati")
    public String angajati(Model model) {
        List<Employee> employees = employeeService.getAllEmployees();
        List<Division> divisions = divisionRepository.findAll();

        Map<UUID, String> divisionNames = divisions.stream()
                .collect(Collectors.toMap(Division::getId, Division::getName));

        long totalEmployees    = employees.size();
        long activeEmployees   = employeeRepository.countByIsAccessActive(true);
        long inactiveEmployees = totalEmployees - activeEmployees;

        model.addAttribute("employees",         employees);
        model.addAttribute("divisions",         divisions);
        model.addAttribute("divisionNames",     divisionNames);
        model.addAttribute("totalEmployees",    totalEmployees);
        model.addAttribute("activeEmployees",   activeEmployees);
        model.addAttribute("inactiveEmployees", inactiveEmployees);
        model.addAttribute("employeeDto",       new EmployeeDto());

        return "angajati";
    }

    // ─── Add employee ─────────────────────────────────────────────────────────

    @PostMapping("/angajati/add")
    public String addEmployee(
            @ModelAttribute("employeeDto") EmployeeDto dto,
            @RequestParam(value = "photoFile", required = false) MultipartFile photoFile,
            @AuthenticationPrincipal UserDetails principal,
            RedirectAttributes redirectAttributes) {

        try {
            // Handle photo upload
            if (photoFile != null && !photoFile.isEmpty()) {
                dto.setPhotoUrl(savePhoto(photoFile));
            }

            // Record who granted access
            if (dto.isAccessActive() && principal != null) {
                userRepository.findByEmail(principal.getUsername())
                        .ifPresent(u -> dto.setAccessGrantedBy(u.getId()));
            }

            employeeService.addEmployee(dto);
            redirectAttributes.addFlashAttribute("successMessage", "Angajat adăugat cu succes.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Eroare la adăugare: " + e.getMessage());
        }
        return "redirect:/angajati";
    }

    // ─── Toggle access ────────────────────────────────────────────────────────

    @PostMapping("/angajati/{id}/toggle-access")
    public String toggleAccess(
            @PathVariable UUID id,
            RedirectAttributes redirectAttributes) {

        employeeService.getEmployeeById(id).ifPresentOrElse(emp -> {
            EmployeeDto dto = new EmployeeDto();
            dto.setFirstName(emp.getFirstName());
            dto.setLastName(emp.getLastName());
            dto.setCnp(emp.getCnp());
            dto.setPhotoUrl(emp.getPhotoUrl());
            dto.setBadgeNumber(emp.getBadgeNumber());
            dto.setDivisionId(emp.getDivisionId());
            dto.setBluetoothSecurityCode(emp.getBluetoothSecurityCode());
            dto.setCarPlate(emp.getCarPlate());
            dto.setAccessActive(!emp.isAccessActive());
            employeeService.updateEmployee(id, dto);
            String state = !emp.isAccessActive() ? "activat" : "dezactivat";
            redirectAttributes.addFlashAttribute("successMessage",
                    "Acces " + state + " cu succes pentru "
                    + emp.getFirstName() + " " + emp.getLastName() + ".");
        }, () -> redirectAttributes.addFlashAttribute("errorMessage",
                "Angajatul nu a fost găsit."));

        return "redirect:/angajati";
    }

    // ─── File save helper ─────────────────────────────────────────────────────

    private String savePhoto(MultipartFile file) throws IOException {
        Path uploadPath = Paths.get(uploadDir).toAbsolutePath();
        Files.createDirectories(uploadPath);

        String originalName = file.getOriginalFilename();
        String ext = (originalName != null && originalName.contains("."))
                ? originalName.substring(originalName.lastIndexOf('.'))
                : ".jpg";
        String filename = UUID.randomUUID() + ext;
        Files.copy(file.getInputStream(), uploadPath.resolve(filename),
                StandardCopyOption.REPLACE_EXISTING);

        return "/uploads/" + filename;
    }
}
