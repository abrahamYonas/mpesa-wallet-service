package com.example.mpesa.Bank.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PayoutResponse {

    private final String result;

    @JsonProperty("bank_ref")
    private final String bankRef;

    private final String reason;

    public static PayoutResponse accepted(String bankRef) {
        return new PayoutResponse("ACCEPTED", bankRef, null);
    }

    public static PayoutResponse rejected(String reason) {
        return new PayoutResponse("REJECTED", null, reason);
    }
}
