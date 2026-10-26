package com.example.mpesa.wallet.dto;

import com.example.mpesa.wallet.Wallet;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class WalletResponse {

    @JsonProperty("wallet_id")
    private final String walletId;

    private final String status;
    private final long balance;
    private final long reserved;
    private final long available;

    public static WalletResponse from(Wallet wallet) {
        return new WalletResponse(
                wallet.getWalletId(),
                wallet.getStatus(),
                wallet.getBalance(),
                wallet.getReserved(),
                wallet.available()
        );
    }
}
