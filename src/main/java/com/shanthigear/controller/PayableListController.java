package com.shanthigear.controller;

import com.shanthigear.model.PaymentDetails;
import com.shanthigear.service.PayableListService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/payable-list")
@RequiredArgsConstructor
@Tag(name = "Payable List Management")
public class PayableListController {

    private final PayableListService payableListService;

    @PostMapping("/upload")
    @Operation(summary = "Upload payable list file")
    public ResponseEntity<List<PaymentDetails>> uploadPayableList(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserDetails userDetails) {
        List<PaymentDetails> result = payableListService.processPayableList(file, userDetails.getUsername());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/vendor/{vendorNumber}")
    @Operation(summary = "Get payment details by vendor number")
    public ResponseEntity<List<PaymentDetails>> getPaymentsByVendor(
            @PathVariable String vendorNumber) {
        List<PaymentDetails> payments = payableListService.getPaymentDetailsByVendor(vendorNumber);
        return ResponseEntity.ok(payments);
    }

    @GetMapping("/pending")
    @Operation(summary = "Get all pending payments")
    public ResponseEntity<List<PaymentDetails>> getPendingPayments() {
        List<PaymentDetails> payments = payableListService.getPendingPayments();
        return ResponseEntity.ok(payments);
    }

    @PutMapping("/{paymentReference}/status/{status}")
    @Operation(summary = "Update payment status")
    public ResponseEntity<PaymentDetails> updatePaymentStatus(
            @PathVariable String paymentReference,
            @PathVariable String status,
            @AuthenticationPrincipal UserDetails userDetails) {
        PaymentDetails updated = payableListService.updatePaymentStatus(paymentReference, status, userDetails.getUsername());
        return ResponseEntity.ok(updated);
    }
}
