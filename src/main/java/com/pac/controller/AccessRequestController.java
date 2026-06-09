package com.pac.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.pac.entity.Employee;
import com.pac.entity.Smartphone;
import com.pac.repository.EmployeeRepository;
import com.pac.repository.SmartphoneRepository;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class AccessRequestController {

  private final SmartphoneRepository smartphoneRepository;
  private final EmployeeRepository employeeRepository;

  // ─── Pagina cereri de acces ───────────────────────────────────────────────
  @GetMapping("/cereri")
  public String cereri(Model model) {
    List<Smartphone> pending = smartphoneRepository.findByIsActiveFalse();
    List<Smartphone> approved = smartphoneRepository.findByIsActiveTrue();

    model.addAttribute("pending", pending);
    model.addAttribute("approved", approved);

    // Atașează și datele angajatului pentru fiecare smartphone
    model.addAttribute("employeeRepo", employeeRepository);

    return "cereri";
  }

  // ─── Aprobă acces ─────────────────────────────────────────────────────────
  @PostMapping("/cereri/{id}/approve")
  public String approve(@PathVariable UUID id) {
    smartphoneRepository
        .findById(id)
        .ifPresent(
            s -> {
              s.setActive(true);
              smartphoneRepository.save(s);
            });
    return "redirect:/cereri";
  }

  // ─── Respinge/revocă acces ────────────────────────────────────────────────
  @PostMapping("/cereri/{id}/revoke")
  public String revoke(@PathVariable UUID id) {
    smartphoneRepository
        .findById(id)
        .ifPresent(
            s -> {
              s.setActive(false);
              smartphoneRepository.save(s);
            });
    return "redirect:/cereri";
  }

  // ─── Șterge cerere ────────────────────────────────────────────────────────
  @PostMapping("/cereri/{id}/delete")
  public String delete(@PathVariable UUID id) {
    smartphoneRepository.deleteById(id);
    return "redirect:/cereri";
  }

  // ─── API pentru aplicația mobilă — verifică status ───────────────────────
  @GetMapping("/api/mobile/status/{employeeId}")
  @ResponseBody
  public ResponseEntity<?> mobileStatus(@PathVariable UUID employeeId) {
    return smartphoneRepository
        .findByEmployeeId(employeeId)
        .map(
            s ->
                ResponseEntity.ok(
                    new StatusResponse(
                        s.isActive() ? "APPROVED" : "PENDING",
                        s.isActive() ? "Acces aprobat" : "Cerere în așteptare")))
        .orElse(
            ResponseEntity.status(404)
                .body(new StatusResponse("NOT_FOUND", "Dispozitiv neînregistrat")));
  }

  record StatusResponse(String status, String message) {}
}
