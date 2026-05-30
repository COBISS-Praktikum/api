package com.cobiss.backend.controllers;

import com.cobiss.backend.services.AltchaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class GatewayControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private AltchaService altchaService;

    @InjectMocks
    private GatewayController gatewayController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(gatewayController).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void verifyGateway_returns200WhenPayloadIsValid() throws Exception {
        when(altchaService.validateResponse("valid-token")).thenReturn(true);

        mockMvc.perform(post("/api/auth/verify-gateway")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("payload", "valid-token"))))
                .andExpect(status().isOk());

        verify(altchaService).validateResponse("valid-token");
    }

    @Test
    void verifyGateway_returns403WhenPayloadIsInvalid() throws Exception {
        when(altchaService.validateResponse("bad-token")).thenReturn(false);

        mockMvc.perform(post("/api/auth/verify-gateway")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("payload", "bad-token"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void verifyGateway_returns403WhenPayloadKeyIsMissing() throws Exception {
        mockMvc.perform(post("/api/auth/verify-gateway")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("other", "value"))))
                .andExpect(status().isForbidden());

        verify(altchaService, never()).validateResponse(any());
    }

    @Test
    void verifyGateway_returns403WhenBodyIsEmpty() throws Exception {
        mockMvc.perform(post("/api/auth/verify-gateway")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());

        verify(altchaService, never()).validateResponse(any());
    }

    @Test
    void verifyGateway_doesNotCallServiceWhenPayloadIsNull() throws Exception {
        mockMvc.perform(post("/api/auth/verify-gateway")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"payload\": null}"))
                .andExpect(status().isForbidden());

        verify(altchaService, never()).validateResponse(any());
    }

    @Test
    void getCaptchaChallenge_returns200WithChallengeData() throws Exception {
        Map<String, Object> challenge = Map.of(
                "algorithm", "SHA-256",
                "challenge", "abc123",
                "salt", "randomsalt",
                "signature", "sig"
        );
        when(altchaService.generateChallenge()).thenReturn(challenge);

        mockMvc.perform(get("/api/auth/captcha-challenge"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.algorithm").value("SHA-256"))
                .andExpect(jsonPath("$.challenge").value("abc123"))
                .andExpect(jsonPath("$.salt").value("randomsalt"))
                .andExpect(jsonPath("$.signature").value("sig"));
    }

    @Test
    void getCaptchaChallenge_callsServiceExactlyOnce() throws Exception {
        when(altchaService.generateChallenge()).thenReturn(Map.of("challenge", "xyz"));

        mockMvc.perform(get("/api/auth/captcha-challenge"))
                .andExpect(status().isOk());

        verify(altchaService, times(1)).generateChallenge();
    }

    @Test
    void getCaptchaChallenge_returns200EvenWithMinimalResponse() throws Exception {
        when(altchaService.generateChallenge()).thenReturn(Map.of());

        mockMvc.perform(get("/api/auth/captcha-challenge"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }
}