package com.littletrip.api.controller;

import com.littletrip.api.dto.PagedResponse;
import com.littletrip.api.dto.TapEventDto;
import com.littletrip.api.dto.TapRequest;
import com.littletrip.api.dto.TapResponse;
import com.littletrip.api.exception.DuplicateEventException;
import com.littletrip.api.exception.GlobalExceptionHandler;
import com.littletrip.api.exception.InvalidDeviceException;
import com.littletrip.api.model.ApiKey;
import com.littletrip.api.model.JourneyStatus;
import com.littletrip.api.model.Operator;
import com.littletrip.api.service.TapService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TapControllerTest {

    private MockMvc mockMvc;

    @Mock
    private TapService tapService;

    private TapController tapController;
    private ApiKey testApiKey;
    private Operator testOperator;

    @BeforeEach
    void setUp() {
        tapController = new TapController(tapService);
        mockMvc = MockMvcBuilders.standaloneSetup(tapController)
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

        testOperator = new Operator();
        testOperator.setId(UUID.randomUUID());
        testOperator.setName("Test Operator");

        testApiKey = new ApiKey();
        testApiKey.setId(UUID.randomUUID());
        testApiKey.setKeyHash("test-key-hash");
        testApiKey.setOperator(testOperator);
    }

    private UsernamePasswordAuthenticationToken createAuth() {
        return new UsernamePasswordAuthenticationToken(testApiKey, null, List.of());
    }

    @Test
    void ingestTap_validRequest_returns201() throws Exception {
        TapResponse response = new TapResponse(UUID.randomUUID(), JourneyStatus.IN_PROGRESS, 0);
        when(tapService.processTap(any(), any())).thenReturn(response);

        mockMvc.perform(post("/taps")
                .contentType(MediaType.APPLICATION_JSON)
                .principal(createAuth())
                .content("""
                    {
                        "id": "11111111-1111-1111-1111-111111111111",
                        "deviceId": "22222222-2222-2222-2222-222222222222",
                        "stopId": "33333333-3333-3333-3333-333333333333",
                        "tapType": "ON",
                        "cardToken": "card-123",
                        "dateTimeUtc": "2024-01-01T10:00:00Z"
                    }
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.tripId").exists());
    }

    @Test
    void ingestTap_deviceNotFound_returns400() throws Exception {
        when(tapService.processTap(any(), any())).thenThrow(new InvalidDeviceException("Device not found"));

        mockMvc.perform(post("/taps")
                .contentType(MediaType.APPLICATION_JSON)
                .principal(createAuth())
                .content("""
                    {
                        "id": "11111111-1111-1111-1111-111111111111",
                        "deviceId": "22222222-2222-2222-2222-222222222222",
                        "stopId": "33333333-3333-3333-3333-333333333333",
                        "tapType": "ON",
                        "cardToken": "card-123",
                        "dateTimeUtc": "2024-01-01T10:00:00Z"
                    }
                    """))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void ingestTap_duplicateEvent_returns409() throws Exception {
        when(tapService.processTap(any(), any())).thenThrow(new DuplicateEventException("Duplicate"));

        mockMvc.perform(post("/taps")
                .contentType(MediaType.APPLICATION_JSON)
                .principal(createAuth())
                .content("""
                    {
                        "id": "11111111-1111-1111-1111-111111111111",
                        "deviceId": "22222222-2222-2222-2222-222222222222",
                        "stopId": "33333333-3333-3333-3333-333333333333",
                        "tapType": "ON",
                        "cardToken": "card-123",
                        "dateTimeUtc": "2024-01-01T10:00:00Z"
                    }
                    """))
            .andExpect(status().isConflict());
    }

    @Test
    void getTapEvents_validRequest_returns200() throws Exception {
        TapEventDto dto = new TapEventDto();
        dto.setCardToken("card-123");
        PagedResponse<TapEventDto> response = new PagedResponse<>(List.of(dto), 1, 20, 1, 1);
        when(tapService.getTapEvents(any(), any(), any(), any(Integer.class), any(Integer.class))).thenReturn(response);

        mockMvc.perform(get("/taps")
                .param("cardToken", "card-123")
                .principal(createAuth()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray());
    }
}