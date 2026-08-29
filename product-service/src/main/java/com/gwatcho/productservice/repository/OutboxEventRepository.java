package com.gwatcho.productservice.repository;

import com.gwatcho.productservice.entity.OutboxEvent;
import com.gwatcho.productservice.entity.OutboxStatus;

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