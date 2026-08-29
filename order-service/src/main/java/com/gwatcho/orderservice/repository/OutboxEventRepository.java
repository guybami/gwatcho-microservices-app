package com.gwatcho.orderservice.repository;

import com.gwatcho.orderservice.entity.OutboxEvent;
import com.gwatcho.orderservice.entity.OutboxStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OutboxEventRepository
        extends JpaRepository<OutboxEvent, Long> {

    List<OutboxEvent>
    findByStatusOrderByCreatedAtAsc(
            OutboxStatus status,
            Pageable pageable
    );
}