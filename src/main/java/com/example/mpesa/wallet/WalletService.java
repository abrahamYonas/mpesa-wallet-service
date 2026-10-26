package com.example.mpesa.wallet;

import com.example.mpesa.exception.ApiException;
import com.example.mpesa.wallet.dto.UpdateWalletStatusRequest;
import com.example.mpesa.wallet.dto.UpdateWalletStatusResponse;
import com.example.mpesa.wallet.dto.WalletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class WalletService {

    private final WalletRepository walletRepository;

    public WalletService(WalletRepository walletRepository) {
        this.walletRepository = walletRepository;
    }

    @Transactional(readOnly = true)
    public WalletResponse getWallet(String walletId) {
        Wallet wallet = findOrThrow(walletId);
        return WalletResponse.from(wallet);
    }

    @Transactional
    public UpdateWalletStatusResponse updateStatus(String walletId, UpdateWalletStatusRequest request) {
        if (request.getStatus() == null
                || (!Wallet.STATUS_ACTIVE.equals(request.getStatus())
                && !Wallet.STATUS_FROZEN.equals(request.getStatus()))) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_STATUS", "status must be ACTIVE or FROZEN");
        }

        Wallet wallet = walletRepository.findByIdForUpdate(walletId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Wallet not found: " + walletId));

        Instant changedAt = Instant.now();
        if (!wallet.getStatus().equals(request.getStatus())) {
            wallet.setStatus(request.getStatus());
            walletRepository.save(wallet);
        }

        return new UpdateWalletStatusResponse(wallet.getWalletId(), wallet.getStatus(), changedAt);
    }

    private Wallet findOrThrow(String walletId) {
        return walletRepository.findById(walletId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Wallet not found: " + walletId));
    }
}
