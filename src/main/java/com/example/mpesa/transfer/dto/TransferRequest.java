package com.example.mpesa.transfer.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TransferRequest {

    @JsonProperty("from_wallet")
    private String fromWallet;

    @JsonProperty("to_wallet")
    private String toWallet;

    private Long amount;
    private String reference;
}
