package com.pac.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.pac.repository.SmartphoneRepository;
import com.pac.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * Adds global model attributes available in every Thymeleaf template:
 *  - pendingCount : number of smartphone access requests awaiting approval
 *  - currentUserName : display name of the logged-in user
 */
@ControllerAdvice
@RequiredArgsConstructor
public class GlobalModelAttributeAdvice {

    private final SmartphoneRepository smartphoneRepository;
    private final UserRepository userRepository;

    @ModelAttribute("pendingCount")
    public long pendingCount() {
        return smartphoneRepository.findByIsActiveFalse().size();
    }

    @ModelAttribute("currentUserName")
    public String currentUserName(@AuthenticationPrincipal UserDetails principal) {
        if (principal == null) return null;
        return userRepository.findByEmail(principal.getUsername())
                .map(u -> u.getFullName())
                .orElse(principal.getUsername());
    }
}
