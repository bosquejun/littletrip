package com.littletrip.api.repository;

import com.littletrip.api.model.TapEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TapEventRepository extends JpaRepository<TapEvent, UUID> {

    List<TapEvent> findByCardTokenOrderByCreatedAtDesc(String cardToken);

    List<TapEvent> findByOperatorIdOrderByCreatedAtDesc(UUID operatorId);

    List<TapEvent> findByCardTokenAndOperatorIdOrderByCreatedAtDesc(String cardToken, UUID operatorId);

    List<TapEvent> findByCardTokenAndOperatorIdAndDeviceIdOrderByCreatedAtDesc(
            String cardToken, UUID operatorId, UUID deviceId);

    long count();

    long countByOperatorId(UUID operatorId);
}