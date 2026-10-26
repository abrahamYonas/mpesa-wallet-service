package com.example.mpesa.wallet;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class WalletDataLoader implements ApplicationRunner {

    private final SeedService seedService;

    public WalletDataLoader(SeedService seedService) {
        this.seedService = seedService;
    }

    @Override
    public void run(ApplicationArguments args) {
        seedService.seedIfEmpty();
    }
}
