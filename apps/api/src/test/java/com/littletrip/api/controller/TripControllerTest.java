package com.littletrip.api.controller;

import com.littletrip.api.dto.PagedResponse;
import com.littletrip.api.dto.TripDto;
import com.littletrip.api.exception.GlobalExceptionHandler;
import com.littletrip.api.model.ApiKey;
import com.littletrip.api.model.JourneyStatus;
import com.littletrip.api.model.Operator;
import com.littletrip.api.service.TripService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TripControllerTest {

    private MockMvc mockMvc;

    @Mock
    private TripService tripService;

    private TripController controller;
    private ApiKey testApiKey;
    private Operator testOperator;

    @BeforeEach
    void setUp() {
        controller = new TripController(tripService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
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
    void getAllTrips_returnsTrips() throws Exception {
        TripDto trip = new TripDto();
        trip.setId(UUID.randomUUID());
        trip.setStatus("IN_PROGRESS");
        
        PagedResponse<TripDto> response = new PagedResponse<>(List.of(trip), 1, 20, 1, 1);
        when(tripService.getTrips(any(), any(), any(Integer.class), any(Integer.class))).thenReturn(response);

        mockMvc.perform(get("/trips")
                .principal(createAuth()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void getTripById_found() throws Exception {
        UUID tripId = UUID.randomUUID();
        TripDto trip = new TripDto();
        trip.setId(tripId);
        trip.setStatus("COMPLETED");
        
        when(tripService.getTripById(tripId)).thenReturn(trip);

        mockMvc.perform(get("/trips/" + tripId)
                .principal(createAuth()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(tripId.toString()));
    }

    @Test
    void getTripById_notFound() throws Exception {
        UUID tripId = UUID.randomUUID();
        when(tripService.getTripById(tripId)).thenReturn(null);

        mockMvc.perform(get("/trips/" + tripId)
                .principal(createAuth()))
            .andExpect(status().isNotFound());
    }
}