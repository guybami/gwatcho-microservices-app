package com.gwatcho.orderservice.repository;

import com.gwatcho.orderservice.entity.OrderOutboxEvent;
import com.gwatcho.orderservice.entity.OutboxStatus;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderOutboxEventRepository
        extends JpaRepository<OrderOutboxEvent, Long> {

    List<OrderOutboxEvent> findTop100ByStatusOrderByCreatedAtAsc(
            OutboxStatus status
    );
}