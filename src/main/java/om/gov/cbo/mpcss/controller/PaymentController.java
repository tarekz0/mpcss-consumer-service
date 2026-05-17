package om.gov.cbo.mpcss.controller;

import om.gov.cbo.mpcss.dto.request.PaymentRequest;
import om.gov.cbo.mpcss.dto.response.ApiResponse;
import om.gov.cbo.mpcss.dto.response.PaymentResponse;
import om.gov.cbo.mpcss.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * REST API for payment operations.
 * Used by mobile app and internal services to initiate/query MPCSS payments.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * Initiate a direct credit payment (sends pacs.008 to MPCSS).
     * POST /api/v1/payments/credit
     */
    @PostMapping("/credit")
    public ResponseEntity<ApiResponse<PaymentResponse>> initiatePayment(
            @Valid @RequestBody PaymentRequest request) {
        log.info("REST: Initiating payment - From: {}, To: {}, Amount: {}",
                request.getDebtorAccount(), request.getCreditorIdentifier(), request.getAmount());

        PaymentResponse response = paymentService.initiatePayment(request);
        return ResponseEntity.ok(ApiResponse.ok("Payment submitted to MPCSS", response));
    }

    /**
     * Get payment status by message ID.
     * GET /api/v1/payments/{messageId}
     */
    @GetMapping("/{messageId}")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPayment(@PathVariable String messageId) {
        PaymentResponse response = paymentService.getPaymentByMessageId(messageId);
        if (response == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * Get payments by status.
     * GET /api/v1/payments?status=SENT
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> getPaymentsByStatus(
            @RequestParam(defaultValue = "SENT") String status) {
        List<PaymentResponse> responses = paymentService.getPaymentsByStatus(status);
        return ResponseEntity.ok(ApiResponse.ok(responses));
    }

    /**
     * Get payments for a specific account within a date range.
     * GET /api/v1/payments/account/{accountId}?from=...&to=...
     */
    @GetMapping("/account/{accountId}")
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> getPaymentsByAccount(
            @PathVariable String accountId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        List<PaymentResponse> responses = paymentService.getPaymentsByAccount(accountId, from, to);
        return ResponseEntity.ok(ApiResponse.ok(responses));
    }
}

