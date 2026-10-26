package com.example.mpesa.transfer;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "transfers")
@Getter
@Setter
@NoArgsConstructor
public class Transfer {

    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_REVERSED = "REVERSED";

    @Id
    @Column(name = "transfer_id")
    private String transferId;

    @Column(name = "from_wallet", nullable = false)
    private String fromWallet;

    @Column(name = "to_wallet", nullable = false)
    private String toWallet;

    @Column(nullable = false)
    private Long amount;

    @Column(nullable = false, length = 64)
    private String reference;

    @Column(nullable = false)
    private String status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "reversal_id")
    private String reversalId;

    @Column(name = "reversal_reason")
    private String reversalReason;
}
