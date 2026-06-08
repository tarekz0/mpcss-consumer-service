package mobi.foo.mpcss.controller;

import mobi.foo.mpcss.dto.request.RegistrationRequest;
import mobi.foo.mpcss.dto.response.ApiResponse;
import mobi.foo.mpcss.dto.response.RegistrationResponse;
import mobi.foo.mpcss.service.RegistrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API for customer/account registration in MPCSS national directory.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/registrations")
@RequiredArgsConstructor
public class RegistrationController {

    private final RegistrationService registrationService;

    /**
     * Submit a registration request to MPCSS.
     * POST /api/v1/registrations
     *
     * Types: CUSTOMER_OPEN, CUSTOMER_MAINTAIN, CUSTOMER_CLOSE,
     *        ACCOUNT_OPEN, ACCOUNT_MAINTAIN, ACCOUNT_CLOSE
     */
    @PostMapping
    public ResponseEntity<ApiResponse<RegistrationResponse>> submitRegistration(
            @Valid @RequestBody RegistrationRequest request) {
        log.info("REST: Registration - Type: {}, Mobile: {}, ID: {}",
                request.getRegistrationType(), request.getMobileNumber(), request.getCustomerIdNumber());

        RegistrationResponse response = registrationService.submitRegistration(request);
        return ResponseEntity.ok(ApiResponse.ok("Registration submitted to MPCSS", response));
    }

    /**
     * Get registration status by message ID.
     * GET /api/v1/registrations/{messageId}
     */
    @GetMapping("/{messageId}")
    public ResponseEntity<ApiResponse<RegistrationResponse>> getRegistration(
            @PathVariable String messageId) {
        RegistrationResponse response = registrationService.getRegistrationByMessageId(messageId);
        if (response == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * Get registrations by mobile number.
     * GET /api/v1/registrations/mobile/{mobileNumber}
     */
    @GetMapping("/mobile/{mobileNumber}")
    public ResponseEntity<ApiResponse<List<RegistrationResponse>>> getByMobile(
            @PathVariable String mobileNumber) {
        return ResponseEntity.ok(
                ApiResponse.ok(registrationService.getRegistrationsByMobile(mobileNumber)));
    }

    /**
     * Get registrations by status.
     * GET /api/v1/registrations?status=PENDING
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<RegistrationResponse>>> getByStatus(
            @RequestParam(defaultValue = "PENDING") String status) {
        return ResponseEntity.ok(
                ApiResponse.ok(registrationService.getRegistrationsByStatus(status)));
    }
}

