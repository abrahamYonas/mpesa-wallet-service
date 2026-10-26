package com.example.mpesa.callback;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "callback_events")
@Getter
@Setter
@NoArgsConstructor
public class CallbackEvent {

    @Id
    @Column(name = "event_id")
    private String eventId;

    @Column(name = "payout_id", nullable = false)
    private String payoutId;

    @Column(nullable = false)
    private String status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
