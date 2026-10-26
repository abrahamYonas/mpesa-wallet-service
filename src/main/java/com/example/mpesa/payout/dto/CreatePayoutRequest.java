package com.example.mpesa.payout.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreatePayoutRequest {

    @JsonProperty("wallet_id")
    private String walletId;

    @JsonProperty("bank_account")
    private String bankAccount;

    private Long amount;
}
