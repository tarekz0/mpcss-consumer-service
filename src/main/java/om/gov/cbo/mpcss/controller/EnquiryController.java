package om.gov.cbo.mpcss.controller;

import om.gov.cbo.mpcss.dto.request.NameVerificationRequest;
import om.gov.cbo.mpcss.dto.response.ApiResponse;
import om.gov.cbo.mpcss.service.EnquiryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST API for enquiry operations:
 * - Name Verification (before payment)
 * - Default Account Check
 * - Payment Enquiry (status of specific payment)
 * - Heartbeat
 * - Participants List (MPCSSv2)
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/enquiry")
@RequiredArgsConstructor
public class EnquiryController {

    private final EnquiryService enquiryService;

    /**
     * Verify customer name by mobile number or alias (cstmrreg.20.01).
     * Used before initiating a payment to confirm beneficiary identity.
     * POST /api/v1/enquiry/name-verification
     */
    @PostMapping("/name-verification")
    public ResponseEntity<ApiResponse<Map<String, String>>> verifyName(
            @Valid @RequestBody NameVerificationRequest request) {
        log.info("REST: Name verification - {}: {}",
                request.getIdentifierType(), request.getIdentifier());

        String messageId = enquiryService.sendNameVerification(
                request.getIdentifier(), request.getIdentifierType());

        return ResponseEntity.ok(ApiResponse.ok("Name verification request sent",
                Map.of("messageId", messageId, "status", "PENDING")));
    }

    /**
     * Check default account for a mobile number (cstmrreg.25.01).
     * POST /api/v1/enquiry/default-account
     */
    @PostMapping("/default-account")
    public ResponseEntity<ApiResponse<Map<String, String>>> checkDefaultAccount(
            @RequestParam String mobileNumber) {
        log.info("REST: Default account check - Mobile: {}", mobileNumber);

        String messageId = enquiryService.sendDefaultAccountCheck(mobileNumber);

        return ResponseEntity.ok(ApiResponse.ok("Default account check sent",
                Map.of("messageId", messageId, "status", "PENDING")));
    }

    /**
     * Enquire about a payment status via MPCSS (Section 5.3).
     * POST /api/v1/enquiry/payment
     */
    @PostMapping("/payment")
    public ResponseEntity<ApiResponse<Map<String, String>>> enquirePayment(
            @RequestParam String originalMessageId) {
        log.info("REST: Payment enquiry - OrigMsgId: {}", originalMessageId);

        String messageId = enquiryService.sendPaymentEnquiry(originalMessageId);

        return ResponseEntity.ok(ApiResponse.ok("Payment enquiry sent",
                Map.of("messageId", messageId, "status", "PENDING")));
    }

    /**
     * Send heartbeat to MPCSS.
     * POST /api/v1/enquiry/heartbeat
     */
    @PostMapping("/heartbeat")
    public ResponseEntity<ApiResponse<String>> heartbeat() {
        enquiryService.sendHeartbeat();
        return ResponseEntity.ok(ApiResponse.ok("Heartbeat sent", "OK"));
    }

    /**
     * Pull participants list from MPCSS (cstmrreg.35.01) — MPCSSv2.
     * POST /api/v1/enquiry/participants
     */
    @PostMapping("/participants")
    public ResponseEntity<ApiResponse<Map<String, String>>> getParticipantsList() {
        log.info("REST: Pull participants list from MPCSS");

        String messageId = enquiryService.sendGetParticipantsList();

        return ResponseEntity.ok(ApiResponse.ok("Participants list request sent",
                Map.of("messageId", messageId, "status", "PENDING")));
    }
}

