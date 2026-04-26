package com.littletrip.api.service;

import com.littletrip.api.model.*;
import com.littletrip.api.repository.JourneyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TripServiceTest {

    @Mock
    private JourneyRepository journeyRepository;

    @Mock
    private TransitFareService transitFareService;

    @InjectMocks
    private TripService tripService;

    @Test
    void processTap_tapOn_withExistingJourney_appliesMaxFareToOldJourney() {
        UUID originStopId = UUID.randomUUID();
        TransitStop originStop = new TransitStop(originStopId, "North Station");

        Journey existingJourney = new Journey();
        existingJourney.setStatus(JourneyStatus.IN_PROGRESS);
        existingJourney.setOriginStop(originStop);

        TapEvent tapEvent = tapEvent(TapType.ON, new TransitStop(UUID.randomUUID(), "South Terminal"));

        when(journeyRepository.findByCardTokenAndStatus(tapEvent.getCardToken(), JourneyStatus.IN_PROGRESS))
                .thenReturn(Optional.of(existingJourney));
        when(transitFareService.getMaxFareFrom(originStopId)).thenReturn(730);
        when(journeyRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        tripService.processTap(tapEvent);

        assertThat(existingJourney.getStatus()).isEqualTo(JourneyStatus.INCOMPLETE);
        assertThat(existingJourney.getFareAmount()).isEqualTo(730);
    }

    @Test
    void processTap_tapOff_withExistingJourney_completesWithFare() {
        UUID originStopId = UUID.randomUUID();
        UUID destStopId = UUID.randomUUID();
        TransitStop originStop = new TransitStop(originStopId, "North Station");
        TransitStop destStop = new TransitStop(destStopId, "South Terminal");

        Journey existingJourney = new Journey();
        existingJourney.setStatus(JourneyStatus.IN_PROGRESS);
        existingJourney.setOriginStop(originStop);

        TapEvent tapEvent = tapEvent(TapType.OFF, destStop);

        when(journeyRepository.findByCardTokenAndStatus(tapEvent.getCardToken(), JourneyStatus.IN_PROGRESS))
                .thenReturn(Optional.of(existingJourney));
        when(transitFareService.getFareCents(originStopId, destStopId)).thenReturn(325);
        when(journeyRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        tripService.processTap(tapEvent);

        assertThat(existingJourney.getStatus()).isEqualTo(JourneyStatus.COMPLETED);
        assertThat(existingJourney.getFareAmount()).isEqualTo(325);
    }

    @Test
    void processTap_tapOff_sameStop_cancelsJourneyWithZeroFare() {
        UUID stopId = UUID.randomUUID();
        TransitStop stop = new TransitStop(stopId, "Central Avenue");

        Journey existingJourney = new Journey();
        existingJourney.setStatus(JourneyStatus.IN_PROGRESS);
        existingJourney.setOriginStop(stop);

        TapEvent tapEvent = tapEvent(TapType.OFF, stop);

        when(journeyRepository.findByCardTokenAndStatus(tapEvent.getCardToken(), JourneyStatus.IN_PROGRESS))
                .thenReturn(Optional.of(existingJourney));
        when(journeyRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        tripService.processTap(tapEvent);

        assertThat(existingJourney.getStatus()).isEqualTo(JourneyStatus.CANCELLED);
        assertThat(existingJourney.getFareAmount()).isEqualTo(0);
    }

    private TapEvent tapEvent(TapType type, TransitStop stop) {
        TapEvent e = new TapEvent();
        e.setTapType(type);
        e.setStop(stop);
        e.setCardToken("card-abc");
        e.setRequestId(UUID.randomUUID());
        e.setCreatedAt(Instant.now());
        return e;
    }
}
