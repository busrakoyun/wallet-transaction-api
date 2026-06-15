package com.hesap.wallet.controller;

import com.hesap.wallet.dto.request.TransferRequest;
import com.hesap.wallet.dto.response.TransferResponse;
import com.hesap.wallet.service.transfer.AbstractTransferService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST endpoint for internal transfers. Depends on the transfer template abstraction
 * (Controller -> Service -> Repository; no facade).
 */
@RestController
@RequestMapping("/api/v1/transfers")
@RequiredArgsConstructor
public class TransferController {

    private final AbstractTransferService transferService;

    /** Executes an atomic account-to-account transfer. Returns 201 Created. */
    @PostMapping
    public ResponseEntity<TransferResponse> transfer(@Valid @RequestBody TransferRequest request) {
        TransferResponse response = transferService.transfer(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
