package com.example.mpesa.payout.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;

@Getter
@AllArgsConstructor
public class PayoutResponse {

    @JsonProperty("payout_id")
    private final String payoutId;

    @JsonProperty("wallet_id")
    private final String walletId;

    private final long amount;

    private final String status;

    @JsonProperty("bank_ref")
    private final String bankRef;

    private final boolean conflict;

    @JsonProperty("updated_at")
    private final Instant updatedAt;
}
