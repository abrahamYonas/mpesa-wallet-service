package com.example.mpesa.wallet.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateWalletStatusRequest {

    private String status;
    private String reason;
}
