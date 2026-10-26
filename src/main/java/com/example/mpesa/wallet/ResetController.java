package com.example.mpesa.wallet;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/mpesa/internal")
public class ResetController {

    private final SeedService seedService;

    public ResetController(SeedService seedService) {
        this.seedService = seedService;
    }

    @PostMapping("/reset")
    public ResponseEntity<Map<String, String>> reset() {
        seedService.reset();
        return ResponseEntity.ok(Map.of("status", "RESET"));
    }
}
