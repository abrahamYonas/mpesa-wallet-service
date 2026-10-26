package com.example.mpesa.wallet.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;

@Getter
@AllArgsConstructor
public class UpdateWalletStatusResponse {

    @JsonProperty("wallet_id")
    private final String walletId;

    private final String status;

    @JsonProperty("changed_at")
    private final Instant changedAt;
}
