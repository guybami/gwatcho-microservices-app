package com.gwatcho.userservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "outbox_event",
        indexes = {
                @Index(
                        name = "idx_outbox_status_created",
                        columnList = "status, created_at"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            name = "event_id",
            nullable = false,
            unique = true,
            updatable = false
    )
    private UUID eventId;

    @Column(
            name = "aggregate_type",
            nullable = false,
            length = 100
    )
    private String aggregateType;

    @Column(
            name = "aggregate_id",
            nullable = false,
            length = 100
    )
    private String aggregateId;

    @Column(
            name = "event_type",
            nullable = false,
            length = 100
    )
    private String eventType;

    @Lob
    @Column(
            name = "payload",
            nullable = false,
            columnDefinition = "LONGTEXT"
    )
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            length = 20
    )
    private OutboxStatus status;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @PrePersist
    protected void onCreate() {

        if (eventId == null) {
            eventId = UUID.randomUUID();
        }

        if (status == null) {
            status = OutboxStatus.NEW;
        }

        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public void markPublished() {
        this.status = OutboxStatus.PUBLISHED;
        this.publishedAt = LocalDateTime.now();
    }
}