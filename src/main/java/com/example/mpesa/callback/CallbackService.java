package com.example.mpesa.callback;

import com.example.mpesa.callback.dto.BankCallbackRequest;
import com.example.mpesa.exception.ApiException;
import com.example.mpesa.exception.ErrorCode;
import com.example.mpesa.payout.Payout;
import com.example.mpesa.payout.PayoutRepository;
import com.example.mpesa.payout.PayoutService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class CallbackService {

    private final CallbackEventRepository callbackEventRepository;
    private final PayoutRepository payoutRepository;
    private final PayoutService payoutService;

    public CallbackService(
            CallbackEventRepository callbackEventRepository,
            PayoutRepository payoutRepository,
            PayoutService payoutService
    ) {
        this.callbackEventRepository = callbackEventRepository;
        this.payoutRepository = payoutRepository;
        this.payoutService = payoutService;
    }

    @Transactional
    public void handle(BankCallbackRequest request) {
        if (request.getEventId() == null || request.getEventId().isBlank()
                || request.getPayoutId() == null || request.getPayoutId().isBlank()
                || request.getStatus() == null || request.getStatus().isBlank()) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "event_id, payout_id and status are required");
        }

        if (!"SUCCEEDED".equals(request.getStatus()) && !"FAILED".equals(request.getStatus())) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "status must be SUCCEEDED or FAILED");
        }

        // same event again → ignore
        if (callbackEventRepository.existsById(request.getEventId())) {
            return;
        }

        Payout payout = payoutRepository.findById(request.getPayoutId())
                .orElseThrow(() -> new ApiException(
                        ErrorCode.PAYOUT_NOT_FOUND,
                        "Payout not found: " + request.getPayoutId()
                ));

        if (!Payout.STATUS_PENDING.equals(payout.getStatus())) {
            if (!payout.getStatus().equals(request.getStatus())) {
                payout.setConflict(true);
                payout.setUpdatedAt(Instant.now());
                payoutRepository.save(payout);
            }
            saveEvent(request);
            return;
        }

        if ("SUCCEEDED".equals(request.getStatus())) {
            payoutService.markSucceeded(request.getPayoutId(), request.getBankRef());
        } else {
            payoutService.markFailed(request.getPayoutId());
        }

        saveEvent(request);
    }

    private void saveEvent(BankCallbackRequest request) {
        try {
            CallbackEvent event = new CallbackEvent();
            event.setEventId(request.getEventId());
            event.setPayoutId(request.getPayoutId());
            event.setStatus(request.getStatus());
            event.setCreatedAt(Instant.now());
            callbackEventRepository.saveAndFlush(event);
        } catch (DataIntegrityViolationException e) {
            // another request saved the same event_id — fine
        }
    }
}
