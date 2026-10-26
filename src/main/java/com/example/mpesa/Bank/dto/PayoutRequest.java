package com.example.mpesa.Bank.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PayoutRequest {

    @JsonProperty("payout_id")
    private String payoutId;

    @JsonProperty("bank_account")
    private String bankAccount;

    private Integer amount;
}
