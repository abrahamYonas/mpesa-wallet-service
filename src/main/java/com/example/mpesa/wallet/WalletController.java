package com.example.mpesa.wallet;

import com.example.mpesa.wallet.dto.UpdateWalletStatusRequest;
import com.example.mpesa.wallet.dto.UpdateWalletStatusResponse;
import com.example.mpesa.wallet.dto.WalletResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/mpesa/v1/wallets")
public class WalletController {

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @GetMapping("/{id}")
    public WalletResponse getWallet(@PathVariable String id) {
        return walletService.getWallet(id);
    }

    @PutMapping("/{id}/status")
    public UpdateWalletStatusResponse updateStatus(
            @PathVariable String id,
            @RequestBody UpdateWalletStatusRequest request
    ) {
        return walletService.updateStatus(id, request);
    }
}
