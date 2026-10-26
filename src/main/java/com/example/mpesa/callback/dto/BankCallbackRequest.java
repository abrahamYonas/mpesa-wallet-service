package com.example.mpesa.callback.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BankCallbackRequest {

    @JsonProperty("event_id")
    private String eventId;

    @JsonProperty("payout_id")
    private String payoutId;

    @JsonProperty("bank_ref")
    private String bankRef;

    private String status;
}
