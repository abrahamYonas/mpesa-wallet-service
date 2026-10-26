package com.example.mpesa.wallet;

import com.example.mpesa.Bank.BankRepository;
import com.example.mpesa.callback.CallbackEventRepository;
import com.example.mpesa.idempotency.IdempotencyRepository;
import com.example.mpesa.payout.PayoutRepository;
import com.example.mpesa.transfer.TransferRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class SeedService {

    private static final long WALLET_BALANCE = 1_000_000L;
    private static final long MERCHANT_BALANCE = 50_000_000L;

    private final WalletRepository walletRepository;
    private final TransferRepository transferRepository;
    private final IdempotencyRepository idempotencyRepository;
    private final BankRepository bankRepository;
    private final PayoutRepository payoutRepository;
    private final CallbackEventRepository callbackEventRepository;

    public SeedService(
            WalletRepository walletRepository,
            TransferRepository transferRepository,
            IdempotencyRepository idempotencyRepository,
            BankRepository bankRepository,
            PayoutRepository payoutRepository,
            CallbackEventRepository callbackEventRepository
    ) {
        this.walletRepository = walletRepository;
        this.transferRepository = transferRepository;
        this.idempotencyRepository = idempotencyRepository;
        this.bankRepository = bankRepository;
        this.payoutRepository = payoutRepository;
        this.callbackEventRepository = callbackEventRepository;
    }

    @Transactional
    public void reset() {
        callbackEventRepository.deleteAllInBatch();
        payoutRepository.deleteAllInBatch();
        bankRepository.deleteAllInBatch();
        transferRepository.deleteAllInBatch();
        idempotencyRepository.deleteAllInBatch();
        walletRepository.deleteAllInBatch();
        walletRepository.saveAll(buildSeedWallets());
    }

    @Transactional
    public void seedIfEmpty() {
        if (walletRepository.count() > 0) {
            return;
        }
        walletRepository.saveAll(buildSeedWallets());
    }

    private List<Wallet> buildSeedWallets() {
        List<Wallet> wallets = new ArrayList<>(106);
        for (int i = 1; i <= 100; i++) {
            wallets.add(new Wallet(walletId("W", i), Wallet.STATUS_ACTIVE, WALLET_BALANCE, 0L));
        }
        for (int i = 101; i <= 105; i++) {
            wallets.add(new Wallet(walletId("W", i), Wallet.STATUS_FROZEN, WALLET_BALANCE, 0L));
        }
        wallets.add(new Wallet("M001", Wallet.STATUS_ACTIVE, MERCHANT_BALANCE, 0L));
        return wallets;
    }

    private String walletId(String prefix, int number) {
        return prefix + String.format("%03d", number);
    }
}
