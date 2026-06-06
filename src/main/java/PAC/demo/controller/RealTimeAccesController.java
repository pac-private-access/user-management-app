/* 
package PAC.demo.controller;

import org.springframework.ui.Model;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import PAC.demo.model.User;
import PAC.demo.repository.UserRepository;
import jakarta.servlet.http.HttpSession;


@Controller
public class RealTimeAccesController{

@GetMapping("/accestimpreal")
public String accestimpreal() {
    return "accestimpreal";
}
    
/* @GetMapping("/accestimpreal")
public String accestimpreal(Model model) {

     model.addAttribute(                // De implementat functionalitatea
            "requests",     accessRequestRepository.findAll() );

    return "accestimpreal";
}

@PostMapping("/accestimpreal/accept")
public String acceptAccess(
        @RequestParam Long id) {

    AccessRequest request =  accessRequestRepository.findById(id).orElse(null);

    if(request != null) {
        request.setStatus("APROBAT");
        accessRequestRepository.save(request);
    } 

    return "redirect:/accestimpreal";
}

@PostMapping("/accestimpreal/reject")
public String rejectAccess(
        @RequestParam Long id) {

    AccessRequest request =     accessRequestRepository.findById(id).orElse(null);

    if(request != null) {
        request.setStatus("REFUZAT");
        accessRequestRepository.save(request);
    }
    return "redirect:/accestimpreal";
}
} 
 */