package com.littletrip.api.controller;

import com.littletrip.api.dto.OperatorUpdateRequest;
import com.littletrip.api.dto.PagedResponse;
import com.littletrip.api.exception.GlobalExceptionHandler;
import com.littletrip.api.model.ApiKey;
import com.littletrip.api.model.Operator;
import com.littletrip.api.model.OperatorDevice;
import com.littletrip.api.service.OperatorDeviceService;
import com.littletrip.api.service.OperatorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class OperatorControllerTest {

    private MockMvc mockMvc;

    @Mock
    private OperatorService operatorService;
    @Mock
    private OperatorDeviceService operatorDeviceService;

    private OperatorController controller;
    private ApiKey testApiKey;
    private Operator testOperator;

    @BeforeEach
    void setUp() {
        controller = new OperatorController(operatorService, operatorDeviceService);
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
    void get_returnsOperator() throws Exception {
        when(operatorService.getCachedOperatorDetails(testOperator.getId())).thenReturn(Optional.of(testOperator));

        mockMvc.perform(get("/operator")
                .principal(createAuth()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Test Operator"));
    }

    @Test
    void get_notFound_returns404() throws Exception {
        when(operatorService.getCachedOperatorDetails(testOperator.getId())).thenReturn(Optional.empty());

        mockMvc.perform(get("/operator")
                .principal(createAuth()))
            .andExpect(status().isNotFound());
    }

    @Test
    void getDevices_returnsDevices() throws Exception {
        OperatorDevice device = new OperatorDevice();
        device.setId(UUID.randomUUID());
        device.setName("Device 1");
        
        PagedResponse<OperatorDevice> response = new PagedResponse<>(List.of(device), 1, 20, 1, 1);
        when(operatorDeviceService.listCachedOperatorDevices(testOperator.getId(), 1, 20)).thenReturn(response);

        mockMvc.perform(get("/operator/devices")
                .principal(createAuth()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void update_success() throws Exception {
        Operator updated = new Operator();
        updated.setId(testOperator.getId());
        updated.setName("Updated Name");
        
        when(operatorService.updateOperator(testOperator.getId(), new OperatorUpdateRequest("Updated Name")))
            .thenReturn(Optional.of(updated));

        mockMvc.perform(put("/operator")
                .principal(createAuth())
                .contentType("application/json")
                .content("""
                    {"name": "Updated Name"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Updated Name"));
    }

    @Test
    void update_notFound_returns404() throws Exception {
        when(operatorService.updateOperator(testOperator.getId(), new OperatorUpdateRequest("New Name")))
            .thenReturn(Optional.empty());

        mockMvc.perform(put("/operator")
                .principal(createAuth())
                .contentType("application/json")
                .content("""
                    {"name": "New Name"}
                    """))
            .andExpect(status().isNotFound());
    }
}