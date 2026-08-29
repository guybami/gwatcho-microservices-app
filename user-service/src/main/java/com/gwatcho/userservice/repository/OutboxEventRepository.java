package com.gwatcho.userservice.repository;

import com.gwatcho.userservice.entity.OutboxEvent;
import com.gwatcho.userservice.entity.OutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OutboxEventRepository
        extends JpaRepository<OutboxEvent, Long> {

    List<OutboxEvent> findTop100ByStatusOrderByCreatedAtAsc(
            OutboxStatus status
    );
}