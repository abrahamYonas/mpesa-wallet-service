package com.example.mpesa.payout;

import com.example.mpesa.exception.ApiException;
import com.example.mpesa.exception.ErrorCode;
import com.example.mpesa.idempotency.IdempotencyRecord;
import com.example.mpesa.idempotency.IdempotencyService;
import com.example.mpesa.payout.dto.CreatePayoutRequest;
import com.example.mpesa.payout.dto.CreatePayoutResponse;
import com.example.mpesa.payout.dto.PayoutResponse;
import com.example.mpesa.wallet.Wallet;
import com.example.mpesa.wallet.WalletRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.UUID;

@Service
public class PayoutService {

    private static final long MIN_AMOUNT = 1L;
    private static final long MAX_AMOUNT = 5_000_000L;

    private final PayoutRepository payoutRepository;
    private final WalletRepository walletRepository;
    private final IdempotencyService idempotencyService;
    private final PayoutWorker payoutWorker;

    public PayoutService(
            PayoutRepository payoutRepository,
            WalletRepository walletRepository,
            IdempotencyService idempotencyService,
            @Lazy PayoutWorker payoutWorker
    ) {
        this.payoutRepository = payoutRepository;
        this.walletRepository = walletRepository;
        this.idempotencyService = idempotencyService;
        this.payoutWorker = payoutWorker;
    }

    @Transactional
    public CreatePayoutResponse create(String idempotencyKey, CreatePayoutRequest request) {
        idempotencyService.validateKey(idempotencyKey);
        validate(request);

        String requestHash = idempotencyService.hashRequest(request);
        if (!idempotencyService.tryStart(idempotencyKey, requestHash)) {
            return replayOrBusy(idempotencyKey, requestHash);
        }

        try {
            Wallet wallet = walletRepository.findByIdForUpdate(request.getWalletId())
                    .orElseThrow(() -> new ApiException(
                            ErrorCode.WALLET_NOT_FOUND,
                            "Wallet not found: " + request.getWalletId()
                    ));

            if (!wallet.isActive()) {
                throw new ApiException(
                        ErrorCode.WALLET_NOT_ACTIVE,
                        "Wallet is not ACTIVE: " + wallet.getWalletId()
                );
            }
            if (wallet.available() < request.getAmount()) {
                throw new ApiException(
                        ErrorCode.INSUFFICIENT_FUNDS,
                        "Wallet has insufficient available balance"
                );
            }

            wallet.setReserved(wallet.getReserved() + request.getAmount());
            walletRepository.save(wallet);

            Instant now = Instant.now();
            Payout payout = new Payout();
            payout.setPayoutId("P-" + shortId());
            payout.setWalletId(wallet.getWalletId());
            payout.setBankAccount(request.getBankAccount());
            payout.setAmount(request.getAmount());
            payout.setStatus(Payout.STATUS_PENDING);
            payout.setConflict(false);
            payout.setSubmitted(false);
            payout.setCreatedAt(now);
            payout.setUpdatedAt(now);
            payoutRepository.save(payout);

            CreatePayoutResponse response = new CreatePayoutResponse(payout.getPayoutId(), payout.getStatus());
            idempotencyService.complete(idempotencyKey, 202, response);

            String payoutId = payout.getPayoutId();
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    payoutWorker.submitToBank(payoutId);
                }
            });

            return response;
        } catch (RuntimeException e) {
            idempotencyService.release(idempotencyKey);
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public PayoutResponse get(String payoutId) {
        Payout payout = payoutRepository.findById(payoutId)
                .orElseThrow(() -> new ApiException(
                        ErrorCode.PAYOUT_NOT_FOUND,
                        "Payout not found: " + payoutId
                ));
        return toResponse(payout);
    }

    @Transactional
    public void markSubmitted(String payoutId, String bankRef) {
        Payout payout = payoutRepository.findByIdForUpdate(payoutId).orElse(null);
        if (payout == null || !Payout.STATUS_PENDING.equals(payout.getStatus())) {
            return;
        }
        payout.setSubmitted(true);
        if (bankRef != null) {
            payout.setBankRef(bankRef);
        }
        payout.setUpdatedAt(Instant.now());
        payoutRepository.save(payout);
    }

    @Transactional
    public void markSucceeded(String payoutId, String bankRef) {
        Payout payout = payoutRepository.findByIdForUpdate(payoutId).orElse(null);
        if (payout == null || !Payout.STATUS_PENDING.equals(payout.getStatus())) {
            return;
        }

        Wallet wallet = walletRepository.findByIdForUpdate(payout.getWalletId()).orElse(null);
        if (wallet == null) {
            return;
        }

        wallet.setBalance(wallet.getBalance() - payout.getAmount());
        wallet.setReserved(wallet.getReserved() - payout.getAmount());
        walletRepository.save(wallet);

        payout.setStatus(Payout.STATUS_SUCCEEDED);
        if (bankRef != null) {
            payout.setBankRef(bankRef);
        }
        payout.setSubmitted(true);
        payout.setUpdatedAt(Instant.now());
        payoutRepository.save(payout);
    }

    @Transactional
    public void markFailed(String payoutId) {
        Payout payout = payoutRepository.findByIdForUpdate(payoutId).orElse(null);
        if (payout == null || !Payout.STATUS_PENDING.equals(payout.getStatus())) {
            return;
        }

        Wallet wallet = walletRepository.findByIdForUpdate(payout.getWalletId()).orElse(null);
        if (wallet == null) {
            return;
        }

        wallet.setReserved(wallet.getReserved() - payout.getAmount());
        walletRepository.save(wallet);

        payout.setStatus(Payout.STATUS_FAILED);
        payout.setSubmitted(true);
        payout.setUpdatedAt(Instant.now());
        payoutRepository.save(payout);
    }

    private CreatePayoutResponse replayOrBusy(String idempotencyKey, String requestHash) {
        IdempotencyRecord existing = idempotencyService.findValid(idempotencyKey)
                .orElseThrow(() -> new ApiException(
                        ErrorCode.REQUEST_IN_PROGRESS,
                        "Same Idempotency-Key is already being processed"
                ));
        idempotencyService.assertSameBodyOrConflict(existing, requestHash);
        if (!existing.isFinished()) {
            throw new ApiException(
                    ErrorCode.REQUEST_IN_PROGRESS,
                    "Same Idempotency-Key is already being processed"
            );
        }
        return idempotencyService.readBody(existing, CreatePayoutResponse.class);
    }

    private void validate(CreatePayoutRequest request) {
        if (request.getWalletId() == null || request.getWalletId().isBlank()) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "wallet_id is required");
        }
        if (request.getBankAccount() == null || request.getBankAccount().isBlank()) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "bank_account is required");
        }
        if (request.getAmount() == null || request.getAmount() < MIN_AMOUNT || request.getAmount() > MAX_AMOUNT) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "amount must be between 1 and 5000000");
        }
    }

    private PayoutResponse toResponse(Payout payout) {
        return new PayoutResponse(
                payout.getPayoutId(),
                payout.getWalletId(),
                payout.getAmount(),
                payout.getStatus(),
                payout.getBankRef(),
                payout.isConflict(),
                payout.getUpdatedAt()
        );
    }

    private String shortId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }
}
