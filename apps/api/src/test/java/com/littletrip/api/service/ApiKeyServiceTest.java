package com.littletrip.api.service;

import com.littletrip.api.dto.ApiKeyUpdateRequest;
import com.littletrip.api.dto.PagedResponse;
import com.littletrip.api.model.ApiKey;
import com.littletrip.api.model.Operator;
import com.littletrip.api.repository.ApiKeyRepository;
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
class ApiKeyServiceTest {

    @Mock
    private ApiKeyRepository apiKeyRepository;

    private ApiKeyService apiKeyService;

    @BeforeEach
    void setUp() {
        apiKeyService = new ApiKeyService(apiKeyRepository);
    }

    @Test
    void listCachedApiKeys_returnsPaginatedApiKeys() {
        int page = 1;
        int size = 10;

        ApiKey apiKey = createApiKey("Test Key");
        Page<ApiKey> apiKeyPage = new PageImpl<>(List.of(apiKey), PageRequest.of(0, size), 1);

        when(apiKeyRepository.findAll(PageRequest.of(0, size))).thenReturn(apiKeyPage);

        PagedResponse<ApiKey> response = apiKeyService.listCachedApiKeys(page, size);

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getPage()).isEqualTo(1);
        assertThat(response.getTotalElements()).isEqualTo(1);
    }

    @Test
    void getCachedApiKeyDetails_returnsApiKey() {
        UUID id = UUID.randomUUID();
        ApiKey apiKey = createApiKey("Test Key");

        when(apiKeyRepository.findById(id)).thenReturn(Optional.of(apiKey));

        Optional<ApiKey> result = apiKeyService.getCachedApiKeyDetails(id);

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Test Key");
    }

    @Test
    void getCachedApiKeyDetails_notFound_returnsEmpty() {
        UUID id = UUID.randomUUID();
        when(apiKeyRepository.findById(id)).thenReturn(Optional.empty());

        Optional<ApiKey> result = apiKeyService.getCachedApiKeyDetails(id);

        assertThat(result).isEmpty();
    }

    @Test
    void updateApiKey_updatesName() {
        UUID id = UUID.randomUUID();
        ApiKey existing = createApiKey("Old Name");
        ApiKeyUpdateRequest request = new ApiKeyUpdateRequest("New Name", null);

        when(apiKeyRepository.findById(id)).thenReturn(Optional.of(existing));
        when(apiKeyRepository.save(any(ApiKey.class))).thenAnswer(i -> i.getArgument(0));

        Optional<ApiKey> result = apiKeyService.updateApiKey(id, request);

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("New Name");
    }

    @Test
    void updateApiKey_updatesActiveStatus() {
        UUID id = UUID.randomUUID();
        ApiKey existing = createApiKey("Test Key");
        existing.setActive(true);

        ApiKeyUpdateRequest request = new ApiKeyUpdateRequest(null, false);

        when(apiKeyRepository.findById(id)).thenReturn(Optional.of(existing));
        when(apiKeyRepository.save(any(ApiKey.class))).thenAnswer(i -> i.getArgument(0));

        Optional<ApiKey> result = apiKeyService.updateApiKey(id, request);

        assertThat(result).isPresent();
        assertThat(result.get().getActive()).isFalse();
    }

    @Test
    void updateApiKey_notFound_returnsEmpty() {
        UUID id = UUID.randomUUID();
        ApiKeyUpdateRequest request = new ApiKeyUpdateRequest("New Name", null);

        when(apiKeyRepository.findById(id)).thenReturn(Optional.empty());

        Optional<ApiKey> result = apiKeyService.updateApiKey(id, request);

        assertThat(result).isEmpty();
    }

    @Test
    void deleteApiKey_existingId_returnsTrue() {
        UUID id = UUID.randomUUID();
        when(apiKeyRepository.existsById(id)).thenReturn(true);

        boolean result = apiKeyService.deleteApiKey(id);

        assertThat(result).isTrue();
        verify(apiKeyRepository).deleteById(id);
    }

    @Test
    void deleteApiKey_nonExistingId_returnsFalse() {
        UUID id = UUID.randomUUID();
        when(apiKeyRepository.existsById(id)).thenReturn(false);

        boolean result = apiKeyService.deleteApiKey(id);

        assertThat(result).isFalse();
    }

    private ApiKey createApiKey(String name) {
        Operator operator = new Operator();
        operator.setId(UUID.randomUUID());
        operator.setName("Test Operator");

        ApiKey apiKey = new ApiKey();
        apiKey.setId(UUID.randomUUID());
        apiKey.setName(name);
        apiKey.setKeyHash("key-value");
        apiKey.setActive(true);
        apiKey.setOperator(operator);
        return apiKey;
    }
}