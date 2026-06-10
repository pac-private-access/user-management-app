package com.pac.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pac.service.GatePendingService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class GateWebController {

    private final GatePendingService gatePendingService;

    /**
     * Called by the "Deschide poarta" button in accestimpreal.html.
     * Sets a flag that the ESP32 picks up on its next poll (every 2 seconds).
     */
    @PostMapping("/gate/trigger")
    public ResponseEntity<String> trigger() {
        gatePendingService.request();
        return ResponseEntity.ok("{\"status\":\"OK\"}");
    }
}
