package com.cobiss.backend.controllers;

import com.cobiss.backend.services.AltchaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(
        name = "Preverjanje pristnosti",
        description = "Generiranje in preverjanje ALTCHA CAPTCHA izzivov za zaščito pred roboti"
)
@RestController
@RequestMapping("/api/auth")
public class GatewayController {

    @Autowired
    private AltchaService altchaService;

    @Operation(
            summary = "Preveri ALTCHA odgovor",
            description = "Sprejme Base64-kodiran ALTCHA odgovor iz odjemalca in preveri njegovo veljavnost. " +
                    "Preverja veljavnost podpisa HMAC, rok veljavnosti izziva ter pravilnost rešene uganke. " +
                    "Vrne 200 OK ob uspešnem preverjanju ali 403 Forbidden ob neuspešnem."
    )
    @ApiResponse(responseCode = "200", description = "Preverjanje uspešno — odgovor je veljaven")
    @ApiResponse(responseCode = "403", description = "Preverjanje neuspešno — neveljaven, potekel ali napačen odgovor")
    @PostMapping("/verify-gateway")
    public ResponseEntity<Void> verifyGatewayElement(
            @RequestBody(
                    description = "Telo zahteve z Base64-kodiranim ALTCHA odgovorom",
                    required = true,
                    content = @Content(schema = @Schema(example = "{\"payload\": \"<base64-kodirani-altcha-odgovor>\"}"))
            )
            @org.springframework.web.bind.annotation.RequestBody Map<String, String> body
    ) {
        String payload = body.get("payload");
        if (payload == null || !altchaService.validateResponse(payload)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "Pridobi nov ALTCHA izziv",
            description = "Generira svež ALTCHA izziv za prikaz CAPTCHA komponente na odjemalcu. " +
                    "Izziv vsebuje SHA-256 uganko, sol z vgrajenim rokom veljavnosti (5 minut) " +
                    "ter HMAC podpis za preprečevanje nedovoljenih sprememb."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Izziv uspešno generiran",
            content = @Content(schema = @Schema(example = """
            {
              "algorithm": "SHA-256",
              "challenge": "a3f2...",
              "salt": "base64sol?expires=1234567890000",
              "signature": "hmac...",
              "maxnumber": 100000
            }
        """))
    )
    @GetMapping("/captcha-challenge")
    public ResponseEntity<Map<String, Object>> getChallenge() {
        return ResponseEntity.ok(altchaService.generateChallenge());
    }
}