package com.cobiss.backend.controllers;

import com.cobiss.backend.services.AltchaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class GatewayController {

    @Autowired
    private AltchaService altchaService;

    @PostMapping("/verify-gateway")
    public ResponseEntity<Void> verifyGatewayElement(@RequestBody Map<String, String> body) {
        String payload = body.get("payload");

        if (payload == null || !altchaService.validateResponse(payload)) {
            // Return 403 Forbidden or 400 Bad Request to tell React it failed
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok().build();
    }

    @GetMapping("/captcha-challenge")
    public ResponseEntity<Map<String, Object>> getChallenge() {
        return ResponseEntity.ok(altchaService.generateChallenge());
    }
}