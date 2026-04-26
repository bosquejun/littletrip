package com.littletrip.api.repository;

import com.littletrip.api.model.ApiKeyDevice;
import com.littletrip.api.model.ApiKeyDeviceId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ApiKeyDeviceRepository extends JpaRepository<ApiKeyDevice, ApiKeyDeviceId> {

    boolean existsByApiKeyIdAndDeviceId(UUID apiKeyId, UUID deviceId);
}
