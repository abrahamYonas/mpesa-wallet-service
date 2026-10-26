package com.example.mpesa.payout;

import com.example.mpesa.Bank.BankService;
import com.example.mpesa.Bank.dto.PayoutRequest;
import com.example.mpesa.Bank.dto.PayoutResponse;
import com.example.mpesa.Bank.dto.PayoutStatusResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
public class PayoutWorker {

    private static final Logger log = LoggerFactory.getLogger(PayoutWorker.class);

    private final PayoutRepository payoutRepository;
    private final PayoutService payoutService;
    private final BankService bankService;

    public PayoutWorker(
            PayoutRepository payoutRepository,
            PayoutService payoutService,
            BankService bankService
    ) {
        this.payoutRepository = payoutRepository;
        this.payoutService = payoutService;
        this.bankService = bankService;
    }

    @Async
    public void submitToBank(String payoutId) {
        try {
            doSubmit(payoutId);
        } catch (Exception e) {
            log.warn("bank submit failed for {}: {}", payoutId, e.getMessage());
        }
    }

    // every 10s, finish anything still PENDING
    @Scheduled(fixedDelay = 10_000)
    public void recover() {
        Instant cutoff = Instant.now().minus(5, ChronoUnit.SECONDS);
        List<Payout> pending = payoutRepository.findByStatusAndUpdatedAtBefore(Payout.STATUS_PENDING, cutoff);
        for (Payout payout : pending) {
            try {
                if (!payout.isSubmitted()) {
                    doSubmit(payout.getPayoutId());
                } else {
                    pollBank(payout.getPayoutId());
                }
            } catch (Exception e) {
                log.warn("recovery failed for {}: {}", payout.getPayoutId(), e.getMessage());
            }
        }
    }

    private void doSubmit(String payoutId) {
        Payout payout = payoutRepository.findById(payoutId).orElse(null);
        if (payout == null || !Payout.STATUS_PENDING.equals(payout.getStatus())) {
            return;
        }

        PayoutRequest request = new PayoutRequest();
        request.setPayoutId(payout.getPayoutId());
        request.setBankAccount(payout.getBankAccount());
        request.setAmount(payout.getAmount().intValue());

        ResponseEntity<PayoutResponse> response = bankService.createPayout(request);

        if (response.getStatusCode().value() == 504 || response.getBody() == null) {
            // unknown — leave PENDING, recovery will ask the bank later
            payoutService.markSubmitted(payoutId, null);
            return;
        }

        PayoutResponse body = response.getBody();
        if ("REJECTED".equals(body.getResult())) {
            payoutService.markFailed(payoutId);
            return;
        }

        // ACCEPTED — wait for final status via recovery poll
        payoutService.markSubmitted(payoutId, body.getBankRef());
    }

    private void pollBank(String payoutId) {
        ResponseEntity<PayoutStatusResponse> response = bankService.getPayoutStatus(payoutId);
        if (response.getBody() == null) {
            return;
        }

        String status = response.getBody().getStatus();
        if ("SUCCEEDED".equals(status)) {
            Payout payout = payoutRepository.findById(payoutId).orElse(null);
            String bankRef = payout != null ? payout.getBankRef() : null;
            payoutService.markSucceeded(payoutId, bankRef);
        } else if ("FAILED".equals(status) || "REJECTED".equals(status)) {
            payoutService.markFailed(payoutId);
        } else if ("NOT_FOUND".equals(status)) {
            // bank never got it — try submit again
            doSubmit(payoutId);
        }
        // IN_PROGRESS → wait for next tick
    }
}
