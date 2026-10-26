package com.example.mpesa.payout.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CreatePayoutResponse {

    @JsonProperty("payout_id")
    private final String payoutId;

    private final String status;
}
