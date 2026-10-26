package com.example.mpesa.callback;

import com.example.mpesa.callback.dto.BankCallbackRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/mpesa/v1/callbacks")
public class CallbackController {

    private final CallbackService callbackService;

    public CallbackController(CallbackService callbackService) {
        this.callbackService = callbackService;
    }

    @PostMapping("/bank")
    public ResponseEntity<Void> bankCallback(@RequestBody BankCallbackRequest request) {
        callbackService.handle(request);
        return ResponseEntity.ok().build();
    }
}
