package com.example.mpesa.payout;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "payouts")
@Getter
@Setter
@NoArgsConstructor
public class Payout {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_SUCCEEDED = "SUCCEEDED";
    public static final String STATUS_FAILED = "FAILED";

    @Id
    @Column(name = "payout_id")
    private String payoutId;

    @Column(name = "wallet_id", nullable = false)
    private String walletId;

    @Column(name = "bank_account", nullable = false)
    private String bankAccount;

    @Column(nullable = false)
    private Long amount;

    @Column(nullable = false)
    private String status;

    @Column(name = "bank_ref")
    private String bankRef;

    @Column(nullable = false)
    private boolean conflict;

    // true after we have called the bank at least once
    @Column(nullable = false)
    private boolean submitted;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
