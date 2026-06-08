package mobi.foo.mpcss.processor;

import mobi.foo.mpcss.dto.CreditTransferMessage;
import mobi.foo.mpcss.dto.PaymentStatusReport;
import mobi.foo.mpcss.dto.RegistrationMessage;
import mobi.foo.mpcss.enums.MpcssMessageType;
import mobi.foo.mpcss.enums.PaymentRejectReasonCode;
import mobi.foo.mpcss.model.MpcssMessageWrapper;
import mobi.foo.mpcss.parser.Iso20022XmlParser;
import mobi.foo.mpcss.producer.MpcssResponseProducer;
import mobi.foo.mpcss.service.EnquiryService;
import mobi.foo.mpcss.service.PaymentService;
import mobi.foo.mpcss.service.RegistrationService;
import mobi.foo.mpcss.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Central processor for all MPCSS inward messages.
 * Routes parsed messages to the appropriate service layer based on message type.
 * As PSP role, we primarily:
 *   - RECEIVE status reports (pacs.002) for our outward payments
 *   - RECEIVE inward credit transfers (pacs.008) to credit our customers
 *   - RECEIVE registration responses (cstmrreg.10) for our registration requests
 *   - RECEIVE enquiry responses (cstmrreg.21, cstmrreg.26)
 *   - RECEIVE binary reports (NCP, Reconciliation, Transaction)
 *   - RECEIVE MPCSSv2 responses (accounts list, set PIN, participants list)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MpcssMessageProcessor {

    private final Iso20022XmlParser xmlParser;
    private final MpcssResponseProducer responseProducer;
    private final PaymentService paymentService;
    private final RegistrationService registrationService;
    private final EnquiryService enquiryService;
    private final ReportService reportService;

    /**
     * Main entry point for processing non-binary messages from PS-mpClear.
     */
    public void processMessage(MpcssMessageWrapper wrapper) {
        MpcssMessageType msgType = wrapper.getMessageType();

        log.info("Processing message type: {} ({})", msgType.getCode(), msgType.getDescription());

        try {
            if (msgType.isPaymentMessage()) {
                processPaymentMessage(wrapper);
            } else if (msgType.isStatusReport()) {
                processStatusReport(wrapper);
            } else if (msgType.isRegistrationMessage()) {
                processRegistrationMessage(wrapper);
            } else if (msgType.isRecurringPayment()) {
                processRecurringPaymentMessage(wrapper);
            } else if (msgType.isPreAuthorizedPayment()) {
                processPreAuthorizedPaymentMessage(wrapper);
            } else {
                log.warn("Unhandled message type: {}", msgType.getCode());
            }
        } catch (Exception e) {
            log.error("Error processing message type {}: {}", msgType.getCode(), e.getMessage(), e);
            handleProcessingError(wrapper, e);
        }
    }

    /**
     * Handle digital signature verification failure.
     */
    public void handleSignatureFailure(MpcssMessageWrapper wrapper) {
        log.error("Signature verification failed for message type: {}", wrapper.getType());
        responseProducer.sendRejectionReply(
                wrapper,
                PaymentRejectReasonCode.DIGITAL_SIGNATURE_ERROR,
                "Digital signature verification failed"
        );
    }

    // ═══════════════════════════════════════════════════════════════════
    // Payment Messages — pacs.008 / pacs.003 / pacs.004
    // ═══════════════════════════════════════════════════════════════════

    private void processPaymentMessage(MpcssMessageWrapper wrapper) {
        switch (wrapper.getMessageType()) {
            case PACS_008 -> {
                // Inward credit: another PSP sent money to our customer
                CreditTransferMessage credit = xmlParser.parseCreditTransfer(wrapper.getContent());
                paymentService.handleInwardCreditTransfer(credit, wrapper.getContent(), wrapper.getCorrelationId());

                // Reply to MPCSS with acceptance (pacs.002)
                responseProducer.sendPaymentStatusReply(
                        wrapper, credit.getMessageId(), "pacs.008.001.05",
                        "ACSP", null, null);
            }
            case PACS_003 -> {
                // Inward debit request — forward to core banking
                log.info("Inward Direct Debit (pacs.003) — forwarding to core banking");
                // As PSP, we forward debit instruction to core banking
            }
            case PACS_004 -> {
                // Payment return/refund — forward to core banking to reverse
                log.info("Inward Payment Return (pacs.004) — forwarding to core banking");
            }
            default -> log.warn("Unexpected payment type: {}", wrapper.getMessageType());
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // Status Report — pacs.002
    // ═══════════════════════════════════════════════════════════════════

    private void processStatusReport(MpcssMessageWrapper wrapper) {
        PaymentStatusReport report = xmlParser.parsePaymentStatusReport(wrapper.getContent());

        paymentService.handlePaymentStatusReport(
                report.getOriginalMessageId(),
                report.getGroupStatus() != null ? report.getGroupStatus().name() : "UNKNOWN",
                report.getReasonCode(),
                report.getAdditionalInfo(),
                wrapper.getContent()
        );
    }

    // ═══════════════════════════════════════════════════════════════════
    // Registration Messages — cstmrreg.*
    // ═══════════════════════════════════════════════════════════════════

    private void processRegistrationMessage(MpcssMessageWrapper wrapper) {
        MpcssMessageType msgType = wrapper.getMessageType();

        switch (msgType) {
            // ── As PSP: we RECEIVE responses to our registration requests ──
            case CSTMRREG_10 -> {
                RegistrationMessage regMsg = xmlParser.parseRegistrationMessage(
                        wrapper.getContent(), "cstmrreg.10.01");
                registrationService.handleRegistrationResponse(regMsg, wrapper.getContent());
            }

            // ── As PSP: we RECEIVE enquiry responses ──
            case CSTMRREG_21 -> {
                // Name verification response — extract customer name
                RegistrationMessage nameResp = xmlParser.parseRegistrationMessage(
                        wrapper.getContent(), "cstmrreg.21.01");
                enquiryService.handleNameVerificationResponse(
                        nameResp.getOriginalMessageId(),
                        nameResp.getFirstName() + " " + nameResp.getLastName(),
                        nameResp.getMobileNumber(),
                        wrapper.getContent());
            }
            case CSTMRREG_26 -> {
                // Default account check response
                enquiryService.handleDefaultAccountResponse(wrapper.getContent());
            }

            // ── MPCSSv2 PSP responses ──
            case CSTMRREG_31 -> {
                // Accounts list response from AHI (for registration flow)
                log.info("Accounts List Response (cstmrreg.31) — forwarding to core banking");
                // Forward to core banking so mobile app can display account list
            }
            case CSTMRREG_34 -> {
                // Set PIN response from AHI
                log.info("Set PIN Response (cstmrreg.34) — forwarding to core banking");
            }
            case CSTMRREG_36 -> {
                // Participants list response
                enquiryService.handleParticipantsListResponse(wrapper.getContent());
            }
            case CSTMRREG_38 -> {
                // Wallets list response
                log.info("Wallets List Response (cstmrreg.38) — forwarding to core banking");
            }

            // ── These are outward-only for PSP (we don't receive them inward) ──
            case CSTMRREG_01, CSTMRREG_02, CSTMRREG_03,
                 CSTMRREG_06, CSTMRREG_07, CSTMRREG_08 ->
                    log.warn("Received outward-only registration type as inward: {}", msgType);

            case CSTMRREG_20, CSTMRREG_25, CSTMRREG_30,
                 CSTMRREG_33, CSTMRREG_35, CSTMRREG_37 ->
                    log.warn("Received request message as inward (PSP role): {}", msgType);

            default -> log.warn("Unhandled registration type: {}", msgType.getCode());
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // Recurring Payments — pain.009 / pain.011 / pain.012
    // ═══════════════════════════════════════════════════════════════════

    private void processRecurringPaymentMessage(MpcssMessageWrapper wrapper) {
        switch (wrapper.getMessageType()) {
            case PAIN_009 -> {
                // As Payer PSP: MPCSS forwards approval request from Beneficiary PSP
                // We display to payer, get consent + PIN, send pain.012 reply
                log.info("Recurring Payment Approval Request (pain.009) — forwarding to core banking");
            }
            case PAIN_011 -> {
                // Cancellation request forwarded from payer or beneficiary
                log.info("Recurring Payment Cancellation (pain.011) — forwarding to core banking");
            }
            case PAIN_012 -> {
                // Status update — approval confirmation or authorization result
                log.info("Recurring Payment Status Update (pain.012) — forwarding to core banking");
            }
            default -> log.warn("Unknown recurring payment type: {}", wrapper.getMessageType());
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // Pre-Authorized Payments — camt.056 / camt.029
    // ═══════════════════════════════════════════════════════════════════

    private void processPreAuthorizedPaymentMessage(MpcssMessageWrapper wrapper) {
        switch (wrapper.getMessageType()) {
            case CAMT_056 -> {
                log.info("Pre-Auth Void/Expiry Request (camt.056) — forwarding to core banking");
            }
            case CAMT_029 -> {
                log.info("Pre-Auth Void/Expiry Status (camt.029) — forwarding to core banking");
            }
            default -> log.warn("Unknown pre-auth type: {}", wrapper.getMessageType());
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // Binary Messages — Reports + Bulk Registration
    // ═══════════════════════════════════════════════════════════════════

    public void processBinaryMessage(String messageId, String messageType, byte[] compressedContent) {
        log.info("Processing binary message - ID: {}, Type: {}, Size: {} bytes",
                messageId, messageType, compressedContent.length);

        try {
            byte[] decompressed = decompressZip(compressedContent);
            String content = new String(decompressed);

            switch (messageType) {
                case "NCP_REPORT" -> reportService.handleNcpReport(messageId, content);
                case "RECON_REPORT" -> reportService.handleReconciliationReport(messageId, content);
                case "TRANS_REPORT" -> reportService.handleTransactionReport(messageId, content);
                case "REGISTRATION_RESPONSE" -> reportService.handleBulkRegistrationResponse(messageId, content);
                default -> log.warn("Unknown binary message type: {}", messageType);
            }
        } catch (Exception e) {
            log.error("Error processing binary message {}: {}", messageId, e.getMessage(), e);
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────────

    private byte[] decompressZip(byte[] compressed) throws Exception {
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(compressed))) {
            ZipEntry entry = zis.getNextEntry();
            if (entry == null) {
                throw new RuntimeException("No entries found in ZIP archive");
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int len;
            while ((len = zis.read(buffer)) > 0) {
                baos.write(buffer, 0, len);
            }
            return baos.toByteArray();
        }
    }

    private void handleProcessingError(MpcssMessageWrapper wrapper, Exception e) {
        log.error("Processing error for message type {}: {}", wrapper.getType(), e.getMessage());
        if (wrapper.getMessageType() != null && wrapper.getMessageType().isPaymentMessage()) {
            responseProducer.sendRejectionReply(
                    wrapper,
                    PaymentRejectReasonCode.TECHNICAL_ERROR,
                    "Internal processing error: " + e.getMessage()
            );
        }
    }
}
