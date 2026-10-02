package com.gwatcho.orderservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "order_outbox_events",
        indexes = {
                @Index(
                        name = "idx_order_outbox_status",
                        columnList = "status"
                ),
                @Index(
                        name = "idx_order_outbox_event_id",
                        columnList = "event_id"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderOutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            name = "event_id",
            nullable = false,
            unique = true,
            updatable = false,
            length = 36
    )
    private String eventId;

    @Column(
            name = "aggregate_id",
            nullable = false
    )
    private Long aggregateId;

    @Column(
            name = "aggregate_type",
            nullable = false,
            length = 100
    )
    private String aggregateType;

    @Column(
            name = "event_type",
            nullable = false,
            length = 100
    )
    private String eventType;

    @Column(
            name = "topic",
            nullable = false,
            length = 200
    )
    private String topic;

    @Lob
    @Column(
            name = "payload",
            nullable = false,
            columnDefinition = "TEXT"
    )
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            length = 30
    )
    @Builder.Default
    private OutboxStatus status = OutboxStatus.PENDING;

    @Column(
            nullable = false
    )
    @Builder.Default
    private Integer attempts = 0;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "last_error")
    private String lastError;
}