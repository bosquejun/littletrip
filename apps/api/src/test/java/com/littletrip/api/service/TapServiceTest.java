package com.littletrip.api.service;

import com.littletrip.api.dto.PagedResponse;
import com.littletrip.api.dto.TapEventDto;
import com.littletrip.api.dto.TapRequest;
import com.littletrip.api.dto.TapResponse;
import com.littletrip.api.exception.DuplicateEventException;
import com.littletrip.api.exception.InvalidDeviceException;
import com.littletrip.api.model.ApiKey;
import com.littletrip.api.model.Journey;
import com.littletrip.api.model.JourneyStatus;
import com.littletrip.api.model.Operator;
import com.littletrip.api.model.OperatorDevice;
import com.littletrip.api.model.TapEvent;
import com.littletrip.api.model.TapType;
import com.littletrip.api.model.TransitStop;
import com.littletrip.api.repository.ApiKeyDeviceRepository;
import com.littletrip.api.repository.OperatorDeviceRepository;
import com.littletrip.api.repository.TapEventRepository;
import com.littletrip.api.repository.TransitStopRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TapServiceTest {

    @Mock
    private TapEventRepository tapEventRepository;
    @Mock
    private ApiKeyDeviceRepository apiKeyDeviceRepository;
    @Mock
    private OperatorDeviceRepository operatorDeviceRepository;
    @Mock
    private TransitStopRepository transitStopRepository;
    @Mock
    private TripService tripService;

    private TapService tapService;

    private ApiKey testApiKey;
    private Operator testOperator;
    private OperatorDevice testDevice;
    private TransitStop testStop;

    @BeforeEach
    void setUp() {
        tapService = new TapService(
            tapEventRepository,
            apiKeyDeviceRepository,
            operatorDeviceRepository,
            transitStopRepository,
            tripService
        );

        testOperator = new Operator();
        testOperator.setId(UUID.randomUUID());
        testOperator.setName("Test Operator");

        testApiKey = new ApiKey();
        testApiKey.setId(UUID.randomUUID());
        testApiKey.setKeyHash("test-api-key");
        testApiKey.setOperator(testOperator);

        testDevice = new OperatorDevice();
        testDevice.setId(UUID.randomUUID());
        testDevice.setName("Test Device");

        testStop = new TransitStop();
        testStop.setId(UUID.randomUUID());
        testStop.setName("Test Stop");
    }

    @Test
    void processTap_success_createsJourneyAndReturnsResponse() {
        TapRequest request = createTapRequest(TapType.ON);

        when(operatorDeviceRepository.findById(testDevice.getId())).thenReturn(Optional.of(testDevice));
        when(apiKeyDeviceRepository.existsByApiKeyIdAndDeviceId(testApiKey.getId(), testDevice.getId())).thenReturn(true);
        when(transitStopRepository.findById(testStop.getId())).thenReturn(Optional.of(testStop));

        Journey journey = new Journey();
        journey.setId(UUID.randomUUID());
        journey.setStatus(JourneyStatus.IN_PROGRESS);
        journey.setFareAmount(0);
        when(tripService.processTap(any())).thenReturn(journey);

        TapResponse response = tapService.processTap(request, testApiKey);

        assertThat(response.getTripId()).isEqualTo(journey.getId());
        assertThat(response.getStatus()).isEqualTo(JourneyStatus.IN_PROGRESS);
        verify(tapEventRepository).save(any(TapEvent.class));
    }

    @Test
    void processTap_deviceNotFound_throwsInvalidDeviceException() {
        TapRequest request = createTapRequest(TapType.ON);
        when(operatorDeviceRepository.findById(testDevice.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tapService.processTap(request, testApiKey))
            .isInstanceOf(InvalidDeviceException.class)
            .hasMessageContaining("Device not found");
    }

    @Test
    void processTap_deviceNotAuthorized_throwsInvalidDeviceException() {
        TapRequest request = createTapRequest(TapType.ON);
        when(operatorDeviceRepository.findById(testDevice.getId())).thenReturn(Optional.of(testDevice));
        when(apiKeyDeviceRepository.existsByApiKeyIdAndDeviceId(testApiKey.getId(), testDevice.getId())).thenReturn(false);

        assertThatThrownBy(() -> tapService.processTap(request, testApiKey))
            .isInstanceOf(InvalidDeviceException.class)
            .hasMessageContaining("not authorized");
    }

    @Test
    void processTap_duplicateEvent_throwsDuplicateEventException() {
        TapRequest request = createTapRequest(TapType.ON);
        when(operatorDeviceRepository.findById(testDevice.getId())).thenReturn(Optional.of(testDevice));
        when(apiKeyDeviceRepository.existsByApiKeyIdAndDeviceId(testApiKey.getId(), testDevice.getId())).thenReturn(true);
        when(transitStopRepository.findById(testStop.getId())).thenReturn(Optional.of(testStop));
        doThrow(new DataIntegrityViolationException("Duplicate")).when(tapEventRepository).save(any());

        assertThatThrownBy(() -> tapService.processTap(request, testApiKey))
            .isInstanceOf(DuplicateEventException.class)
            .hasMessageContaining("already processed");
    }

    @Test
    void getTapEvents_byCardToken_returnsPaginatedResults() {
        String cardToken = "card-123";
        UUID operatorId = UUID.randomUUID();

        TapEvent event = new TapEvent();
        event.setCardToken(cardToken);
        event.setTapType(TapType.ON);
        event.setCreatedAt(Instant.now());

        when(tapEventRepository.findByCardTokenAndOperatorIdOrderByCreatedAtDesc(cardToken, operatorId))
            .thenReturn(List.of(event));

        PagedResponse<TapEventDto> response = tapService.getTapEvents(cardToken, operatorId, null, 0, 10);

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getPage()).isEqualTo(0);
        assertThat(response.getSize()).isEqualTo(10);
    }

    @Test
    void getTapEvents_allFilters_returnsFilteredResults() {
        String cardToken = "card-123";
        UUID operatorId = UUID.randomUUID();
        UUID deviceId = UUID.randomUUID();

        TapEvent event = new TapEvent();
        event.setCardToken(cardToken);
        event.setTapType(TapType.OFF);
        event.setCreatedAt(Instant.now());

        when(tapEventRepository.findByCardTokenAndOperatorIdAndDeviceIdOrderByCreatedAtDesc(cardToken, operatorId, deviceId))
            .thenReturn(List.of(event));

        PagedResponse<TapEventDto> response = tapService.getTapEvents(cardToken, operatorId, deviceId, 0, 10);

        assertThat(response.getContent()).hasSize(1);
        verify(tapEventRepository).findByCardTokenAndOperatorIdAndDeviceIdOrderByCreatedAtDesc(cardToken, operatorId, deviceId);
    }

    @Test
    void getTapEvents_paginationAppliesCorrectly() {
        String cardToken = "card-123";
        UUID operatorId = UUID.randomUUID();

        List<TapEvent> events = List.of(
            createTapEvent(Instant.now().minusSeconds(10)),
            createTapEvent(Instant.now().minusSeconds(5)),
            createTapEvent(Instant.now())
        );

        when(tapEventRepository.findByCardTokenAndOperatorIdOrderByCreatedAtDesc(cardToken, operatorId))
            .thenReturn(events);

        PagedResponse<TapEventDto> response = tapService.getTapEvents(cardToken, operatorId, null, 0, 2);

        assertThat(response.getContent()).hasSize(2);
        assertThat(response.getTotalElements()).isEqualTo(3);
        assertThat(response.getTotalPages()).isEqualTo(2);
    }

    @Test
    void getTotalTapEvents_callsRepository() {
        when(tapEventRepository.count()).thenReturn(100L);

        long count = tapService.getTotalTapEvents();

        assertThat(count).isEqualTo(100L);
        verify(tapEventRepository).count();
    }

    @Test
    void getTotalTapEventsByOperator_callsRepository() {
        UUID operatorId = UUID.randomUUID();
        when(tapEventRepository.countByOperatorId(operatorId)).thenReturn(50L);

        long count = tapService.getTotalTapEventsByOperator(operatorId);

        assertThat(count).isEqualTo(50L);
    }

    private TapRequest createTapRequest(TapType tapType) {
        TapRequest request = new TapRequest();
        request.setId(UUID.randomUUID().toString());
        request.setDeviceId(testDevice.getId().toString());
        request.setStopId(testStop.getId().toString());
        request.setTapType(tapType);
        request.setCardToken("card-123");
        request.setDateTimeUtc(Instant.now());
        return request;
    }

    private TapEvent createTapEvent(Instant createdAt) {
        TapEvent event = new TapEvent();
        event.setCardToken("card-123");
        event.setTapType(TapType.ON);
        event.setCreatedAt(createdAt);
        return event;
    }
}