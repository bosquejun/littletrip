package com.littletrip.api.service;

import com.littletrip.api.dto.OperatorUpdateRequest;
import com.littletrip.api.dto.PagedResponse;
import com.littletrip.api.model.Operator;
import com.littletrip.api.repository.OperatorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OperatorServiceTest {

    @Mock
    private OperatorRepository operatorRepository;

    private OperatorService operatorService;

    @BeforeEach
    void setUp() {
        operatorService = new OperatorService(operatorRepository);
    }

    @Test
    void listCachedOperators_returnsPaginatedOperators() {
        int page = 1;
        int size = 10;

        Operator operator = createOperator("Test Operator");
        Page<Operator> operatorPage = new PageImpl<>(List.of(operator), PageRequest.of(0, size), 1);

        when(operatorRepository.findAll(PageRequest.of(0, size))).thenReturn(operatorPage);

        PagedResponse<Operator> response = operatorService.listCachedOperators(page, size);

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getPage()).isEqualTo(1);
        assertThat(response.getTotalElements()).isEqualTo(1);
        assertThat(response.getTotalPages()).isEqualTo(1);
    }

    @Test
    void getCachedOperatorDetails_returnsOperator() {
        UUID id = UUID.randomUUID();
        Operator operator = createOperator("Test Operator");

        when(operatorRepository.findById(id)).thenReturn(Optional.of(operator));

        Optional<Operator> result = operatorService.getCachedOperatorDetails(id);

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Test Operator");
    }

    @Test
    void getCachedOperatorDetails_notFound_returnsEmpty() {
        UUID id = UUID.randomUUID();
        when(operatorRepository.findById(id)).thenReturn(Optional.empty());

        Optional<Operator> result = operatorService.getCachedOperatorDetails(id);

        assertThat(result).isEmpty();
    }

    @Test
    void updateOperator_updatesName() {
        UUID id = UUID.randomUUID();
        Operator existing = createOperator("Old Name");
        OperatorUpdateRequest request = new OperatorUpdateRequest("New Name");

        when(operatorRepository.findById(id)).thenReturn(Optional.of(existing));
        when(operatorRepository.save(any(Operator.class))).thenAnswer(i -> i.getArgument(0));

        Optional<Operator> result = operatorService.updateOperator(id, request);

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("New Name");
    }

    @Test
    void updateOperator_notFound_returnsEmpty() {
        UUID id = UUID.randomUUID();
        OperatorUpdateRequest request = new OperatorUpdateRequest("New Name");

        when(operatorRepository.findById(id)).thenReturn(Optional.empty());

        Optional<Operator> result = operatorService.updateOperator(id, request);

        assertThat(result).isEmpty();
    }

    @Test
    void updateOperator_nullName_doesNotUpdate() {
        UUID id = UUID.randomUUID();
        Operator existing = createOperator("Original");
        OperatorUpdateRequest request = new OperatorUpdateRequest(null);

        when(operatorRepository.findById(id)).thenReturn(Optional.of(existing));
        when(operatorRepository.save(any(Operator.class))).thenAnswer(i -> i.getArgument(0));

        Optional<Operator> result = operatorService.updateOperator(id, request);

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Original");
    }

    @Test
    void deleteOperator_existingId_returnsTrue() {
        UUID id = UUID.randomUUID();
        when(operatorRepository.existsById(id)).thenReturn(true);

        boolean result = operatorService.deleteOperator(id);

        assertThat(result).isTrue();
        verify(operatorRepository).deleteById(id);
    }

    @Test
    void deleteOperator_nonExistingId_returnsFalse() {
        UUID id = UUID.randomUUID();
        when(operatorRepository.existsById(id)).thenReturn(false);

        boolean result = operatorService.deleteOperator(id);

        assertThat(result).isFalse();
    }

    private Operator createOperator(String name) {
        Operator operator = new Operator();
        operator.setId(UUID.randomUUID());
        operator.setName(name);
        return operator;
    }
}