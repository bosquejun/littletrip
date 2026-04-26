package com.littletrip.api.service;

import com.littletrip.api.model.TransitFare;
import com.littletrip.api.model.TransitFareId;
import com.littletrip.api.repository.TransitFareRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransitFareServiceTest {

    @Mock
    private TransitFareRepository transitFareRepository;

    @InjectMocks
    private TransitFareService transitFareService;

    @Test
    void getFareCents_returnsFareCents_whenPairExists() {
        UUID stopA = UUID.randomUUID();
        UUID stopB = UUID.randomUUID();
        when(transitFareRepository.findByStopPair(stopA, stopB))
                .thenReturn(Optional.of(fareWithCents(stopA, stopB, 325)));

        assertThat(transitFareService.getFareCents(stopA, stopB)).isEqualTo(325);
    }

    @Test
    void getFareCents_returnsZero_whenPairNotFound() {
        UUID stopA = UUID.randomUUID();
        UUID stopB = UUID.randomUUID();
        when(transitFareRepository.findByStopPair(stopA, stopB)).thenReturn(Optional.empty());

        assertThat(transitFareService.getFareCents(stopA, stopB)).isEqualTo(0);
    }

    @Test
    void getMaxFareFrom_returnsMaxCents_whenMultipleFaresExist() {
        UUID stopId = UUID.randomUUID();
        when(transitFareRepository.findAllByStopId(stopId))
                .thenReturn(List.of(fareWithCents(stopId, UUID.randomUUID(), 325),
                                    fareWithCents(stopId, UUID.randomUUID(), 730)));

        assertThat(transitFareService.getMaxFareFrom(stopId)).isEqualTo(730);
    }

    @Test
    void getMaxFareFrom_returnsZero_whenNoFaresExist() {
        UUID stopId = UUID.randomUUID();
        when(transitFareRepository.findAllByStopId(stopId)).thenReturn(List.of());

        assertThat(transitFareService.getMaxFareFrom(stopId)).isEqualTo(0);
    }

    private TransitFare fareWithCents(UUID stopAId, UUID stopBId, int cents) {
        TransitFare fare = new TransitFare();
        fare.setId(new TransitFareId(stopAId, stopBId));
        fare.setBaseFareCents(cents);
        return fare;
    }
}
