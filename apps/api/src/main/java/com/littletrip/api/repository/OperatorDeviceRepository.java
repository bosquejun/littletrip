package com.littletrip.api.repository;

import com.littletrip.api.model.OperatorDevice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface OperatorDeviceRepository extends JpaRepository<OperatorDevice, UUID> {

    Page<OperatorDevice> findByOperatorId(UUID operatorId, PageRequest pageRequest);
}