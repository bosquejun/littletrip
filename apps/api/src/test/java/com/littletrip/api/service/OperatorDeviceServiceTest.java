package com.littletrip.api.service;

import com.littletrip.api.dto.OperatorDeviceUpdateRequest;
import com.littletrip.api.dto.OperatorDeviceUpdateRequest.DeviceStatus;
import com.littletrip.api.dto.PagedResponse;
import com.littletrip.api.model.Operator;
import com.littletrip.api.model.OperatorDevice;
import com.littletrip.api.repository.OperatorDeviceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OperatorDeviceServiceTest {

    @Mock
    private OperatorDeviceRepository operatorDeviceRepository;

    private OperatorDeviceService operatorDeviceService;

    @BeforeEach
    void setUp() {
        operatorDeviceService = new OperatorDeviceService(operatorDeviceRepository);
    }

    @Test
    void listCachedOperatorDevices_noFilter_returnsAllDevices() {
        int page = 1;
        int size = 10;

        OperatorDevice device = createOperatorDevice("Device 1");
        Page<OperatorDevice> devicePage = new PageImpl<>(List.of(device), PageRequest.of(0, size), 1);

        when(operatorDeviceRepository.findAll(PageRequest.of(0, size))).thenReturn(devicePage);

        PagedResponse<OperatorDevice> response = operatorDeviceService.listCachedOperatorDevices(null, page, size);

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getPage()).isEqualTo(1);
    }

    @Test
    void listCachedOperatorDevices_withOperatorFilter_returnsFilteredDevices() {
        UUID operatorId = UUID.randomUUID();
        int page = 1;
        int size = 10;

        OperatorDevice device = createOperatorDevice("Device 1");
        Page<OperatorDevice> devicePage = new PageImpl<>(List.of(device), PageRequest.of(0, size), 1);

        when(operatorDeviceRepository.findByOperatorId(operatorId, PageRequest.of(0, size))).thenReturn(devicePage);

        PagedResponse<OperatorDevice> response = operatorDeviceService.listCachedOperatorDevices(operatorId, page, size);

        assertThat(response.getContent()).hasSize(1);
        verify(operatorDeviceRepository).findByOperatorId(operatorId, PageRequest.of(0, size));
    }

    @Test
    void getCachedOperatorDeviceDetails_returnsDevice() {
        UUID id = UUID.randomUUID();
        OperatorDevice device = createOperatorDevice("Test Device");

        when(operatorDeviceRepository.findById(id)).thenReturn(Optional.of(device));

        Optional<OperatorDevice> result = operatorDeviceService.getCachedOperatorDeviceDetails(id);

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Test Device");
    }

    @Test
    void getCachedOperatorDeviceDetails_notFound_returnsEmpty() {
        UUID id = UUID.randomUUID();
        when(operatorDeviceRepository.findById(id)).thenReturn(Optional.empty());

        Optional<OperatorDevice> result = operatorDeviceService.getCachedOperatorDeviceDetails(id);

        assertThat(result).isEmpty();
    }

    @Test
    void updateOperatorDevice_updatesName() {
        UUID id = UUID.randomUUID();
        OperatorDevice existing = createOperatorDevice("Old Name");
        OperatorDeviceUpdateRequest request = new OperatorDeviceUpdateRequest(null, "New Name", null, null);

        when(operatorDeviceRepository.findById(id)).thenReturn(Optional.of(existing));
        when(operatorDeviceRepository.save(any(OperatorDevice.class))).thenAnswer(i -> i.getArgument(0));

        Optional<OperatorDevice> result = operatorDeviceService.updateOperatorDevice(id, request);

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("New Name");
    }

    @Test
    void updateOperatorDevice_updatesStatus() {
        UUID id = UUID.randomUUID();
        OperatorDevice existing = createOperatorDevice("Device");
        existing.setStatus("ACTIVE");

        OperatorDeviceUpdateRequest request = new OperatorDeviceUpdateRequest(null, null, DeviceStatus.inactive, null);

        when(operatorDeviceRepository.findById(id)).thenReturn(Optional.of(existing));
        when(operatorDeviceRepository.save(any(OperatorDevice.class))).thenAnswer(i -> i.getArgument(0));

        Optional<OperatorDevice> result = operatorDeviceService.updateOperatorDevice(id, request);

        assertThat(result).isPresent();
        assertThat(result.get().getStatus()).isEqualTo("inactive");
    }

    @Test
    void updateOperatorDevice_updatesLastSeenAt() {
        UUID id = UUID.randomUUID();
        OperatorDevice existing = createOperatorDevice("Device");
        OffsetDateTime newLastSeen = OffsetDateTime.now().minusHours(1);

        OperatorDeviceUpdateRequest request = new OperatorDeviceUpdateRequest(null, null, null, newLastSeen);

        when(operatorDeviceRepository.findById(id)).thenReturn(Optional.of(existing));
        when(operatorDeviceRepository.save(any(OperatorDevice.class))).thenAnswer(i -> i.getArgument(0));

        Optional<OperatorDevice> result = operatorDeviceService.updateOperatorDevice(id, request);

        assertThat(result).isPresent();
    }

    @Test
    void updateOperatorDevice_notFound_returnsEmpty() {
        UUID id = UUID.randomUUID();
        OperatorDeviceUpdateRequest request = new OperatorDeviceUpdateRequest(null, "New Name", null, null);

        when(operatorDeviceRepository.findById(id)).thenReturn(Optional.empty());

        Optional<OperatorDevice> result = operatorDeviceService.updateOperatorDevice(id, request);

        assertThat(result).isEmpty();
    }

    private OperatorDevice createOperatorDevice(String name) {
        Operator operator = new Operator();
        operator.setId(UUID.randomUUID());
        operator.setName("Test Operator");

        OperatorDevice device = new OperatorDevice();
        device.setId(UUID.randomUUID());
        device.setName(name);
        device.setOperator(operator);
        device.setStatus("ACTIVE");
        device.setLastSeenAt(Instant.now());
        return device;
    }
}