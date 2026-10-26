package com.example.mpesa.transfer.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;

@Getter
@AllArgsConstructor
public class TransferResponse {

    @JsonProperty("transfer_id")
    private final String transferId;

    private final String status;

    @JsonProperty("from_available_after")
    private final long fromAvailableAfter;

    @JsonProperty("created_at")
    private final Instant createdAt;
}
