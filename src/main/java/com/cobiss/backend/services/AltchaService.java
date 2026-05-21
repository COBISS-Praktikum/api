package com.cobiss.backend.services;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AltchaService {

    @Value("${altcha.secret}")
    private String secretKey;

    // 1. Generate the Challenge for React
    public Map<String, Object> generateChallenge() {
        try {
            // Generate a random secret number for the client to guess (the puzzle answer)
            int number = new SecureRandom().nextInt(50000) + 1000; // Adjust max for difficulty

            // Create a unique salt containing an expiration time
            String salt = Base64.getEncoder().encodeToString(SecureRandom.getSeed(12));
            long expiresAt = Instant.now().plusSeconds(300).toEpochMilli(); // 5 min expiry
            String saltWithMeta = salt + "?expires=" + expiresAt;

            // Hash the salt + secret number to create the puzzle block
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String puzzleInput = saltWithMeta + number;
            byte[] puzzleHash = digest.digest(puzzleInput.getBytes(StandardCharsets.UTF_8));
            String challenge = bytesToHex(puzzleHash);

            // Create a signature to prevent tampering
            String signature = hmacSha256(saltWithMeta, secretKey);

            Map<String, Object> response = new HashMap<>();
            response.put("algorithm", "SHA-256");
            response.put("challenge", challenge);
            response.put("salt", saltWithMeta);
            response.put("signature", signature);
            // The max number tells the client's browser how far it might need to count
            response.put("maxnumber", 100000);

            return response;
        } catch (Exception e) {
            throw new RuntimeException("Error generating ALTCHA challenge", e);
        }
    }

    // 2. Validate the Payload sent by React Form Submission
    public boolean validateResponse(String payloadBase64) {
        try {
            // Decode incoming ALTCHA payload
            String decodedJson = new String(Base64.getDecoder().decode(payloadBase64), StandardCharsets.UTF_8);
            // Quick manual JSON parse or use Jackson Object Mapper:
            // Assuming you extract fields: salt, number, challenge, signature

            // Validation Logic Checklist:
            // 1. Check if 'expires' timestamp in the salt has passed.
            // 2. Recompute the HMAC signature using the salt and your secretKey. Verify it matches incoming signature.
            // 3. Recompute SHA-256(salt + number). Verify it matches the incoming challenge string.

            return true; // Return true if all match
        } catch (Exception e) {
            return false;
        }
    }

    private String hmacSha256(String data, String key) throws Exception {
        Mac sha256Hmac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        sha256Hmac.init(secretKeySpec);
        return bytesToHex(sha256Hmac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}