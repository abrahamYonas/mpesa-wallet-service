package com.example.mpesa.Bank;

import com.example.mpesa.Bank.dto.PayoutRequest;
import com.example.mpesa.Bank.dto.PayoutResponse;
import com.example.mpesa.Bank.dto.PayoutStatusResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Service
public class BankService {

    private static final Logger log = LoggerFactory.getLogger(BankService.class);

    private static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    private static final String STATUS_SUCCEEDED = "SUCCEEDED";
    private static final String STATUS_FAILED = "FAILED";
    private static final String STATUS_REJECTED = "REJECTED";
    private static final String STATUS_NOT_FOUND = "NOT_FOUND";

    private final BankRepository bankRepository;
    private final RestClient restClient;
    private final List<String> callbackUrls;

    public BankService(
            BankRepository bankRepository,
            RestClient.Builder restClientBuilder,
            @Value("${mpesa.callback.urls:http://localhost:8081/mpesa/v1/callbacks/bank,http://localhost:8082/mpesa/v1/callbacks/bank}")
            String callbackUrls
    ) {
        this.bankRepository = bankRepository;
        this.restClient = restClientBuilder.build();
        this.callbackUrls = List.of(callbackUrls.split(","));
    }

    public ResponseEntity<PayoutResponse> createPayout(PayoutRequest request) {
        sleepSeconds(2, 6);

        return bankRepository.findById(request.getPayoutId())
                .map(existing -> handleIdempotentReplay(existing, request))
                .orElseGet(() -> processNewPayout(request));
    }

    public ResponseEntity<PayoutStatusResponse> getPayoutStatus(String payoutId) {
        sleepSeconds(1, 3);

        return bankRepository.findById(payoutId)
                .map(bank -> ResponseEntity.ok(new PayoutStatusResponse(bank.getStatus())))
                .orElseGet(() -> ResponseEntity.ok(new PayoutStatusResponse(STATUS_NOT_FOUND)));
    }

    private ResponseEntity<PayoutResponse> handleIdempotentReplay(Bank existing, PayoutRequest request) {
        if (!existing.getAmount().equals(request.getAmount())
                || !existing.getBankAccount().equals(request.getBankAccount())) {
            return ResponseEntity.ok(PayoutResponse.rejected("IDEMPOTENCY_AMOUNT_MISMATCH"));
        }

        if ("REJECTED".equals(existing.getResult())) {
            return ResponseEntity.ok(PayoutResponse.rejected(existing.getReason()));
        }

        return ResponseEntity.ok(PayoutResponse.accepted(existing.getBankRef()));
    }

    private ResponseEntity<PayoutResponse> processNewPayout(PayoutRequest request) {
        int roll = ThreadLocalRandom.current().nextInt(100);

        if (roll < 80) {
            return acceptPayout(request);
        }
        if (roll < 88) {
            return rejectPayout(request, "BENEFICIARY_ACCOUNT_INVALID");
        }
        return timeoutPayout(request);
    }

    private ResponseEntity<PayoutResponse> acceptPayout(PayoutRequest request) {
        String bankRef = generateBankRef();
        savePayout(request, "ACCEPTED", null, bankRef, STATUS_IN_PROGRESS);
        scheduleFinalStatus(request.getPayoutId(), bankRef);
        return ResponseEntity.ok(PayoutResponse.accepted(bankRef));
    }

    private ResponseEntity<PayoutResponse> rejectPayout(PayoutRequest request, String reason) {
        savePayout(request, "REJECTED", reason, null, STATUS_REJECTED);
        return ResponseEntity.ok(PayoutResponse.rejected(reason));
    }

    private ResponseEntity<PayoutResponse> timeoutPayout(PayoutRequest request) {
        boolean processed = ThreadLocalRandom.current().nextBoolean();

        if (processed) {
            String bankRef = generateBankRef();
            savePayout(request, "ACCEPTED", null, bankRef, STATUS_IN_PROGRESS);
            scheduleFinalStatus(request.getPayoutId(), bankRef);
        }

        sleepSeconds(60, 60);
        return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT).build();
    }

    private Bank savePayout(
            PayoutRequest request,
            String result,
            String reason,
            String bankRef,
            String status
    ) {
        Bank bank = new Bank();
        bank.setPayoutId(request.getPayoutId());
        bank.setBankAccount(request.getBankAccount());
        bank.setAmount(request.getAmount());
        bank.setResult(result);
        bank.setReason(reason);
        bank.setBankRef(bankRef);
        bank.setStatus(status);
        return bankRepository.save(bank);
    }

    private void scheduleFinalStatus(String payoutId, String bankRef) {
        long delaySeconds = ThreadLocalRandom.current().nextLong(1, 11);
        CompletableFuture.delayedExecutor(delaySeconds, TimeUnit.SECONDS).execute(() -> {
            bankRepository.findById(payoutId).ifPresent(bank -> {
                if (!STATUS_IN_PROGRESS.equals(bank.getStatus())) {
                    return;
                }
                boolean succeeded = ThreadLocalRandom.current().nextInt(100) < 95;
                String status = succeeded ? STATUS_SUCCEEDED : STATUS_FAILED;
                bank.setStatus(status);
                if (!succeeded) {
                    bank.setReason("BANK_TRANSFER_FAILED");
                }
                bankRepository.save(bank);
                sendCallbacks(payoutId, bankRef, status);
            });
        });
    }

    private void sendCallbacks(String payoutId, String bankRef, String status) {
        String eventId = UUID.randomUUID().toString().substring(0, 13);
        int copies = ThreadLocalRandom.current().nextInt(100) < 30
                ? ThreadLocalRandom.current().nextInt(2, 4)
                : 1;

        String url = callbackUrls.get(ThreadLocalRandom.current().nextInt(callbackUrls.size())).trim();

        Map<String, String> body = new HashMap<>();
        body.put("event_id", eventId);
        body.put("payout_id", payoutId);
        body.put("bank_ref", bankRef);
        body.put("status", status);

        for (int i = 0; i < copies; i++) {
            try {
                restClient.post()
                        .uri(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(body)
                        .retrieve()
                        .toBodilessEntity();
            } catch (Exception e) {
                log.warn("callback failed to {}: {}", url, e.getMessage());
            }
        }
    }

    private String generateBankRef() {
        return "BR" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }

    private void sleepSeconds(int minInclusive, int maxInclusive) {
        long seconds = ThreadLocalRandom.current().nextLong(minInclusive, maxInclusive + 1L);
        try {
            TimeUnit.SECONDS.sleep(seconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while simulating bank latency", e);
        }
    }
}
