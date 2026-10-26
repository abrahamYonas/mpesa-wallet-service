package com.example.mpesa.wallet;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class WalletDataLoader implements ApplicationRunner {

    private final WalletRepository walletRepository;

    public WalletDataLoader(WalletRepository walletRepository) {
        this.walletRepository = walletRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        createIfMissing("W001", 1_000_000L);
        createIfMissing("W002", 500_000L);
    }

    private void createIfMissing(String walletId, long balance) {
        if (walletRepository.existsById(walletId)) {
            return;
        }
        walletRepository.save(new Wallet(walletId, Wallet.STATUS_ACTIVE, balance, 0L));
    }
}
