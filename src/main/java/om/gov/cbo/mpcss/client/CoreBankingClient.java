package om.gov.cbo.mpcss.client;

import om.gov.cbo.mpcss.dto.CreditTransferMessage;
import om.gov.cbo.mpcss.dto.PaymentStatusReport;
import om.gov.cbo.mpcss.dto.RegistrationMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.Map;

/**
 * Client for forwarding MPCSS messages to downstream core banking system.
 * All inward messages from MPCSS are forwarded here after parsing and validation.
 */
@Slf4j
@Component
public class CoreBankingClient {

    private final RestTemplate restTemplate;
    private final CoreBankingProperties properties;

    public CoreBankingClient(RestTemplateBuilder builder, CoreBankingProperties properties) {
        this.properties = properties;
        this.restTemplate = builder
                .rootUri(properties.getBaseUrl())
                .connectTimeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                .readTimeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                .build();
    }

    /**
     * Forward an inward credit transfer (pacs.008) to core banking for crediting.
     * As PSP, we receive this when another PSP sends money to our customer.
     */
    public CoreBankingResponse forwardCreditTransfer(CreditTransferMessage creditTransfer) {
        log.info("Forwarding credit transfer to core banking - MsgId: {}, Creditor: {}, Amount: {} {}",
                creditTransfer.getMessageId(),
                creditTransfer.getCreditorAccountId(),
                creditTransfer.getTransactionAmount(),
                creditTransfer.getSettlementCurrency());

        return post("/payments/credit", creditTransfer);
    }

    /**
     * Forward a payment status report (pacs.002) to core banking.
     * This updates the status of a previously sent outward payment.
     */
    public CoreBankingResponse forwardPaymentStatusReport(PaymentStatusReport statusReport) {
        log.info("Forwarding status report to core banking - OrigMsgId: {}, Status: {}",
                statusReport.getOriginalMessageId(), statusReport.getGroupStatus());

        return post("/payments/status", statusReport);
    }

    /**
     * Forward a registration response (cstmrreg.10) to core banking.
     */
    public CoreBankingResponse forwardRegistrationResponse(RegistrationMessage registrationMsg) {
        log.info("Forwarding registration response to core banking - MsgId: {}, Status: {}",
                registrationMsg.getMessageId(), registrationMsg.getStatus());

        return post("/registration/response", registrationMsg);
    }

    /**
     * Forward an inward payment (credit to our customer) for processing.
     * Core banking should credit the beneficiary account and return result.
     */
    public CoreBankingResponse requestCreditProcessing(String creditorAccount, String amount,
                                                        String currency, String messageId) {
        var request = Map.of(
                "creditorAccount", creditorAccount,
                "amount", amount,
                "currency", currency,
                "mpcssMessageId", messageId
        );
        return post("/accounts/credit", request);
    }

    /**
     * Forward a name verification result to core banking.
     */
    public CoreBankingResponse forwardNameVerificationResult(String mobileNumber, String customerName,
                                                              String originalRequestId) {
        var request = Map.of(
                "mobileNumber", mobileNumber,
                "customerName", customerName,
                "originalRequestId", originalRequestId
        );
        return post("/customers/name-verified", request);
    }

    /**
     * Notify core banking about a report received from MPCSS.
     */
    public CoreBankingResponse forwardReport(String reportType, String reportContent, String sessionId) {
        var request = Map.of(
                "reportType", reportType,
                "content", reportContent,
                "sessionId", sessionId
        );
        return post("/reports/inward", request);
    }

    // ─── Private ────────────────────────────────────────────────────────

    private CoreBankingResponse post(String path, Object body) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Object> entity = new HttpEntity<>(body, headers);

            ResponseEntity<CoreBankingResponse> response = restTemplate.exchange(
                    path, HttpMethod.POST, entity, CoreBankingResponse.class);

            CoreBankingResponse result = response.getBody();
            if (result == null) {
                result = CoreBankingResponse.builder()
                        .success(response.getStatusCode().is2xxSuccessful())
                        .message("No response body")
                        .build();
            }
            log.debug("Core banking response for {}: success={}", path, result.isSuccess());
            return result;

        } catch (RestClientException e) {
            log.error("Failed to call core banking {}: {}", path, e.getMessage());
            return CoreBankingResponse.builder()
                    .success(false)
                    .message("Core banking unavailable: " + e.getMessage())
                    .build();
        }
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class CoreBankingResponse {
        private boolean success;
        private String message;
        private String referenceId;
        private String errorCode;
    }
}

