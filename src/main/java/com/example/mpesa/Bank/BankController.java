package com.example.mpesa.Bank;

import com.example.mpesa.Bank.dto.PayoutRequest;
import com.example.mpesa.Bank.dto.PayoutResponse;
import com.example.mpesa.Bank.dto.PayoutStatusResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/bank/payouts")
public class BankController {

    private final BankService bankService;

    public BankController(BankService bankService) {
        this.bankService = bankService;
    }

    @PostMapping
    public ResponseEntity<PayoutResponse> createPayout(@RequestBody PayoutRequest request) {
        return bankService.createPayout(request);
    }

    @GetMapping("/{payoutId}")
    public ResponseEntity<PayoutStatusResponse> getPayoutStatus(@PathVariable String payoutId) {
        return bankService.getPayoutStatus(payoutId);
    }
}
