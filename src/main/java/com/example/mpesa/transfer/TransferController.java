package com.example.mpesa.transfer;

import com.example.mpesa.transfer.dto.ReversalRequest;
import com.example.mpesa.transfer.dto.ReversalResponse;
import com.example.mpesa.transfer.dto.TransferRequest;
import com.example.mpesa.transfer.dto.TransferResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/mpesa/v1/p2p/transfers")
public class TransferController {

    private final TransferService transferService;

    public TransferController(TransferService transferService) {
        this.transferService = transferService;
    }

    @PostMapping
    public ResponseEntity<TransferResponse> createTransfer(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody TransferRequest request
    ) {
        TransferResponse response = transferService.createTransfer(idempotencyKey, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{id}/reversal")
    public ReversalResponse reverse(
            @PathVariable String id,
            @RequestBody(required = false) ReversalRequest request
    ) {
        return transferService.reverse(id, request);
    }
}
