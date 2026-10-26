package com.example.mpesa.transfer.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ReversalResponse {

    @JsonProperty("transfer_id")
    private final String transferId;

    @JsonProperty("reversal_id")
    private final String reversalId;

    private final String status;
    private final long amount;
}
