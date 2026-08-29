package com.gwatcho.productservice.entity;

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
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            nullable = false,
            unique = true,
            updatable = false
    )
    private UUID eventId;

    @Column(
            nullable = false,
            length = 50
    )
    private String aggregateType;

    @Column(
            nullable = false,
            length = 100
    )
    private String aggregateId;

    @Column(
            nullable = false,
            length = 100
    )
    private String eventType;

    @Lob
    @Column(nullable = false)
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            length = 20
    )
    @Builder.Default
    private OutboxStatus status = OutboxStatus.NEW;

    @Column(nullable = false)
    @Builder.Default
    private int retryCount = 0;

    @Column(length = 2000)
    private String lastError;

    @Column(
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

    private LocalDateTime publishedAt;


    @PrePersist
    protected void onCreate() {

        if (eventId == null) {
            eventId = UUID.randomUUID();
        }

        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }

        if (status == null) {
            status = OutboxStatus.NEW;
        }
    }
}