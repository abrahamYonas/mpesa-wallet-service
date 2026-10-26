package com.example.mpesa.transfer;

import com.example.mpesa.exception.ApiException;
import com.example.mpesa.exception.ErrorCode;
import com.example.mpesa.idempotency.IdempotencyRecord;
import com.example.mpesa.idempotency.IdempotencyService;
import com.example.mpesa.transfer.dto.ReversalRequest;
import com.example.mpesa.transfer.dto.ReversalResponse;
import com.example.mpesa.transfer.dto.TransferRequest;
import com.example.mpesa.transfer.dto.TransferResponse;
import com.example.mpesa.wallet.Wallet;
import com.example.mpesa.wallet.WalletRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class TransferService {

    private static final long MIN_AMOUNT = 1L;
    private static final long MAX_AMOUNT = 5_000_000L;

    private final TransferRepository transferRepository;
    private final WalletRepository walletRepository;
    private final IdempotencyService idempotencyService;

    public TransferService(
            TransferRepository transferRepository,
            WalletRepository walletRepository,
            IdempotencyService idempotencyService
    ) {
        this.transferRepository = transferRepository;
        this.walletRepository = walletRepository;
        this.idempotencyService = idempotencyService;
    }

    @Transactional
    public TransferResponse createTransfer(String idempotencyKey, TransferRequest request) {
        idempotencyService.validateKey(idempotencyKey);
        validateTransferRequest(request);

        String requestHash = idempotencyService.hashRequest(request);
        Optional<IdempotencyRecord> existing = idempotencyService.findValid(idempotencyKey);
        if (existing.isPresent()) {
            idempotencyService.assertSameBodyOrConflict(existing.get(), requestHash);
            return idempotencyService.readBody(existing.get(), TransferResponse.class);
        }

        WalletPair wallets = lockPair(request.getFromWallet(), request.getToWallet());
        Wallet from = wallets.leftEquals(request.getFromWallet()) ? wallets.left() : wallets.right();
        Wallet to = wallets.leftEquals(request.getToWallet()) ? wallets.left() : wallets.right();

        requireActive(from);
        requireActive(to);

        if (from.available() < request.getAmount()) {
            throw new ApiException(
                    ErrorCode.INSUFFICIENT_FUNDS,
                    "Sender has insufficient available balance"
            );
        }

        from.setBalance(from.getBalance() - request.getAmount());
        to.setBalance(to.getBalance() + request.getAmount());
        walletRepository.save(from);
        walletRepository.save(to);

        Transfer transfer = new Transfer();
        transfer.setTransferId("T-" + shortId());
        transfer.setFromWallet(from.getWalletId());
        transfer.setToWallet(to.getWalletId());
        transfer.setAmount(request.getAmount());
        transfer.setReference(request.getReference());
        transfer.setStatus(Transfer.STATUS_COMPLETED);
        transfer.setCreatedAt(Instant.now());
        transferRepository.save(transfer);

        TransferResponse response = new TransferResponse(
                transfer.getTransferId(),
                transfer.getStatus(),
                from.available(),
                transfer.getCreatedAt()
        );
        idempotencyService.save(idempotencyKey, requestHash, 201, response);
        return response;
    }

    @Transactional
    public ReversalResponse reverse(String transferId, ReversalRequest request) {
        Transfer transfer = transferRepository.findById(transferId)
                .orElseThrow(() -> new ApiException(
                        ErrorCode.TRANSFER_NOT_FOUND,
                        "Transfer not found: " + transferId
                ));

        if (Transfer.STATUS_REVERSED.equals(transfer.getStatus())) {
            throw new ApiException(ErrorCode.ALREADY_REVERSED, "Transfer was already reversed");
        }
        if (!Transfer.STATUS_COMPLETED.equals(transfer.getStatus())) {
            throw new ApiException(
                    ErrorCode.NOT_REVERSIBLE,
                    "Only COMPLETED transfers can be reversed"
            );
        }

        // Debit original to_wallet (must be ACTIVE); credit original from_wallet (may be FROZEN)
        WalletPair wallets = lockPair(transfer.getFromWallet(), transfer.getToWallet());
        Wallet debitWallet = wallets.leftEquals(transfer.getToWallet()) ? wallets.left() : wallets.right();
        Wallet creditWallet = wallets.leftEquals(transfer.getFromWallet()) ? wallets.left() : wallets.right();

        requireActive(debitWallet);
        if (debitWallet.available() < transfer.getAmount()) {
            throw new ApiException(
                    ErrorCode.INSUFFICIENT_FUNDS,
                    "Debited wallet has insufficient available balance"
            );
        }

        debitWallet.setBalance(debitWallet.getBalance() - transfer.getAmount());
        creditWallet.setBalance(creditWallet.getBalance() + transfer.getAmount());
        walletRepository.save(debitWallet);
        walletRepository.save(creditWallet);

        String reversalId = "R-" + shortId();
        transfer.setStatus(Transfer.STATUS_REVERSED);
        transfer.setReversalId(reversalId);
        transfer.setReversalReason(request != null ? request.getReason() : null);
        transferRepository.save(transfer);

        return new ReversalResponse(
                transfer.getTransferId(),
                reversalId,
                Transfer.STATUS_REVERSED,
                transfer.getAmount()
        );
    }

    private void validateTransferRequest(TransferRequest request) {
        if (request.getFromWallet() == null || request.getFromWallet().isBlank()
                || request.getToWallet() == null || request.getToWallet().isBlank()) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "from_wallet and to_wallet are required");
        }
        if (request.getAmount() == null || request.getAmount() < MIN_AMOUNT || request.getAmount() > MAX_AMOUNT) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "amount must be between 1 and 5000000");
        }
        if (request.getReference() == null
                || request.getReference().isBlank()
                || request.getReference().length() > 64) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "reference must be 1 to 64 characters");
        }
        if (request.getFromWallet().equals(request.getToWallet())) {
            throw new ApiException(ErrorCode.SAME_WALLET, "from_wallet and to_wallet must differ");
        }
    }

    private WalletPair lockPair(String walletA, String walletB) {
        List<String> ordered = List.of(walletA, walletB).stream()
                .sorted(Comparator.naturalOrder())
                .toList();
        Wallet left = lockWallet(ordered.get(0));
        Wallet right = lockWallet(ordered.get(1));
        return new WalletPair(left, right);
    }

    private Wallet lockWallet(String walletId) {
        return walletRepository.findByIdForUpdate(walletId)
                .orElseThrow(() -> new ApiException(ErrorCode.WALLET_NOT_FOUND, "Wallet not found: " + walletId));
    }

    private void requireActive(Wallet wallet) {
        if (!wallet.isActive()) {
            throw new ApiException(
                    ErrorCode.WALLET_NOT_ACTIVE,
                    "Wallet is not ACTIVE: " + wallet.getWalletId()
            );
        }
    }

    private String shortId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    private record WalletPair(Wallet left, Wallet right) {
        boolean leftEquals(String walletId) {
            return left.getWalletId().equals(walletId);
        }
    }
}
