package om.gov.cbo.mpcss.processor;

import om.gov.cbo.mpcss.dto.CreditTransferMessage;
import om.gov.cbo.mpcss.dto.PaymentStatusReport;
import om.gov.cbo.mpcss.dto.RegistrationMessage;
import om.gov.cbo.mpcss.enums.MpcssMessageType;
import om.gov.cbo.mpcss.enums.PaymentRejectReasonCode;
import om.gov.cbo.mpcss.model.MpcssMessageWrapper;
import om.gov.cbo.mpcss.parser.Iso20022XmlParser;
import om.gov.cbo.mpcss.producer.MpcssResponseProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Central processor for all MPCSS messages.
 * Routes parsed messages to appropriate business logic handlers based on message type.
 *
 * <p>Handles all message categories:
 * <ul>
 *   <li>Payment messages: pacs.008 (Credit), pacs.003 (Debit), pacs.004 (Return)</li>
 *   <li>Status reports: pacs.002</li>
 *   <li>Registration: cstmrreg.01-10 (Customer/Account CRUD)</li>
 *   <li>MPCSSv2 Registration: cstmrreg.30-38 (Account List, Set PIN, Participants, Wallets)</li>
 *   <li>Recurring Payments: pain.009, pain.011, pain.012</li>
 *   <li>Pre-Authorized: camt.056, camt.029</li>
 *   <li>Enquiries: Name verification, Default account check, Payment enquiry</li>
 *   <li>Binary: Reports (NCP, Reconciliation, Transactions), Bulk Registration</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MpcssMessageProcessor {

    private final Iso20022XmlParser xmlParser;
    private final MpcssResponseProducer responseProducer;

    /**
     * Main entry point for processing non-binary messages from PS-mpClear.
     */
    public void processMessage(MpcssMessageWrapper wrapper) {
        MpcssMessageType msgType = wrapper.getMessageType();
        String content = wrapper.getContent();

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
     * Responds with rejection reason code 1003 (Digital signature/Security error).
     */
    public void handleSignatureFailure(MpcssMessageWrapper wrapper) {
        log.error("Signature verification failed for message type: {}",
                wrapper.getType());
        // Build rejection response with reason code 1003
        responseProducer.sendRejectionReply(
                wrapper,
                PaymentRejectReasonCode.DIGITAL_SIGNATURE_ERROR,
                "Digital signature verification failed"
        );
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Payment Message Processing
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Process payment messages: pacs.008 (Credit), pacs.003 (Debit), pacs.004 (Return).
     * As the instructed PSP, we need to:
     * 1. Parse the ISO 20022 message
     * 2. Validate the payment transaction
     * 3. Credit/Debit the beneficiary/debtor account
     * 4. Send pacs.002 status report back to PS-mpClear
     */
    private void processPaymentMessage(MpcssMessageWrapper wrapper) {
        MpcssMessageType msgType = wrapper.getMessageType();

        switch (msgType) {
            case PACS_008 -> processCreditTransfer(wrapper);
            case PACS_003 -> processDirectDebit(wrapper);
            case PACS_004 -> processPaymentReturn(wrapper);
            default -> log.warn("Unexpected payment message type: {}", msgType);
        }
    }

    /**
     * Process pacs.008.001.05 - Direct Credit Transfer (Section 5.1).
     * Instructed PSP receives credit transfer, credits beneficiary, sends pacs.002 reply.
     */
    private void processCreditTransfer(MpcssMessageWrapper wrapper) {
        CreditTransferMessage creditTransfer = xmlParser.parseCreditTransfer(wrapper.getContent());

        log.info("Credit Transfer received - MsgId: {}, Amount: {} {}, Debtor: {}, Creditor: {}",
                creditTransfer.getMessageId(),
                creditTransfer.getTransactionAmount(),
                creditTransfer.getSettlementCurrency(),
                creditTransfer.getDebtorName(),
                creditTransfer.getCreditorName());

        // TODO: Implement business logic
        // 1. Validate creditor account exists and is active
        // 2. Validate creditor customer in national directory
        // 3. Process credit to beneficiary account
        // 4. Check if OnUs transaction
        // 5. Handle PEP encrypted data for MPCSSv2 (PIN/OTP verification)

        // Send ACSP (Accepted Settlement In Process) reply
        responseProducer.sendPaymentStatusReply(
                wrapper,
                creditTransfer.getMessageId(),
                "pacs.008.001.05",
                "ACSP",
                null,
                null
        );

        log.info("Credit Transfer processed successfully: {}", creditTransfer.getMessageId());
    }

    /**
     * Process pacs.003.001.05 - Direct Debit.
     */
    private void processDirectDebit(MpcssMessageWrapper wrapper) {
        log.info("Processing Direct Debit (pacs.003) message");

        // TODO: Implement debit processing
        // 1. Validate debtor account
        // 2. Check sufficient funds
        // 3. Process debit
        // 4. Send pacs.002 reply

        log.info("Direct Debit processing completed");
    }

    /**
     * Process pacs.004.001.05 - Payment Return (Refund) (Section 2.2).
     */
    private void processPaymentReturn(MpcssMessageWrapper wrapper) {
        log.info("Processing Payment Return (pacs.004) message");

        // TODO: Implement return/refund processing
        // 1. Find original transaction
        // 2. Reverse the original payment
        // 3. Send pacs.002 reply

        log.info("Payment Return processing completed");
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Status Report Processing
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Process pacs.002.001.06 - Payment Status Report (Section 5.2).
     * Status codes: RJCT (Rejected), ACSP (Accepted), ACSC (Settlement Completed).
     */
    private void processStatusReport(MpcssMessageWrapper wrapper) {
        PaymentStatusReport statusReport = xmlParser.parsePaymentStatusReport(wrapper.getContent());

        log.info("Status Report received - OrigMsgId: {}, Status: {}, Reason: {}",
                statusReport.getOriginalMessageId(),
                statusReport.getGroupStatus(),
                statusReport.getReasonCode());

        // TODO: Implement status report handling
        // 1. Find original outward payment by originalMessageId
        // 2. Update payment status (ACSP/ACSC/RJCT)
        // 3. Notify customer of payment result
        // 4. If RJCT, handle rejection reason

        switch (statusReport.getGroupStatus()) {
            case ACSP -> log.info("Payment accepted for settlement: {}",
                    statusReport.getOriginalMessageId());
            case ACSC -> log.info("Payment settlement completed: {}",
                    statusReport.getOriginalMessageId());
            case RJCT -> log.warn("Payment REJECTED: {} - Reason: {} ({})",
                    statusReport.getOriginalMessageId(),
                    statusReport.getReasonCode(),
                    statusReport.getAdditionalInfo());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Registration Message Processing
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Process registration messages (cstmrreg.*) - Section 3.1 and Section 6.2.
     */
    private void processRegistrationMessage(MpcssMessageWrapper wrapper) {
        MpcssMessageType msgType = wrapper.getMessageType();

        switch (msgType) {
            // MPCSSv1 Registration
            case CSTMRREG_10 -> processRegistrationResponse(wrapper);
            case CSTMRREG_21 -> processNameVerificationResponse(wrapper);
            case CSTMRREG_26 -> processDefaultAccountResponse(wrapper);

            // MPCSSv2 Registration (incoming as AHI)
            case CSTMRREG_01 -> processCustomerRecordOpening(wrapper);
            case CSTMRREG_02 -> processCustomerRecordMaintenance(wrapper);
            case CSTMRREG_03 -> processCustomerRecordClosing(wrapper);
            case CSTMRREG_06 -> processAccountOpening(wrapper);
            case CSTMRREG_07 -> processAccountMaintenance(wrapper);
            case CSTMRREG_08 -> processAccountClosing(wrapper);

            // MPCSSv2 New Messages
            case CSTMRREG_30 -> processAccountsListRequest(wrapper);
            case CSTMRREG_31 -> processAccountsListResponse(wrapper);
            case CSTMRREG_33 -> processSetPinRequest(wrapper);
            case CSTMRREG_34 -> processSetPinResponse(wrapper);
            case CSTMRREG_35 -> processGetParticipantsListRequest(wrapper);
            case CSTMRREG_36 -> processGetParticipantsListResponse(wrapper);
            case CSTMRREG_37 -> processGetWalletsListRequest(wrapper);
            case CSTMRREG_38 -> processGetWalletsListResponse(wrapper);

            default -> log.warn("Unhandled registration message type: {}", msgType.getCode());
        }
    }

    private void processRegistrationResponse(MpcssMessageWrapper wrapper) {
        RegistrationMessage regMsg = xmlParser.parseRegistrationMessage(
                wrapper.getContent(), "cstmrreg.10.01");
        log.info("Registration Response - OrigMsgId: {}, Status: {}, Reason: {}",
                regMsg.getOriginalMessageId(), regMsg.getStatus(), regMsg.getReasonCode());
        // TODO: Update registration request status in database
    }

    private void processNameVerificationResponse(MpcssMessageWrapper wrapper) {
        log.info("Processing Name Verification Response (cstmrreg.21)");
        // TODO: Extract and return customer name for payment confirmation flow
    }

    private void processDefaultAccountResponse(MpcssMessageWrapper wrapper) {
        log.info("Processing Default Account Response (cstmrreg.26)");
        // TODO: Extract default account info
    }

    private void processCustomerRecordOpening(MpcssMessageWrapper wrapper) {
        RegistrationMessage regMsg = xmlParser.parseRegistrationMessage(
                wrapper.getContent(), "cstmrreg.01.01");
        log.info("Customer Record Opening - ID: {}, Mobile: {}",
                regMsg.getCustomerIdNumber(), regMsg.getMobileNumber());
        // TODO: Process customer opening, send cstmrreg.10 response
    }

    private void processCustomerRecordMaintenance(MpcssMessageWrapper wrapper) {
        log.info("Processing Customer Record Maintenance (cstmrreg.02)");
        // TODO: Update customer record
    }

    private void processCustomerRecordClosing(MpcssMessageWrapper wrapper) {
        log.info("Processing Customer Record Closing (cstmrreg.03)");
        // TODO: Close customer record
    }

    private void processAccountOpening(MpcssMessageWrapper wrapper) {
        RegistrationMessage regMsg = xmlParser.parseRegistrationMessage(
                wrapper.getContent(), "cstmrreg.06.01");
        log.info("Account Opening - Type: {}, Currency: {}, Banked: {}",
                regMsg.getAccountType(), regMsg.getCurrency(), regMsg.getIsBanked());
        // TODO: Process account opening, link to AHI if applicable
    }

    private void processAccountMaintenance(MpcssMessageWrapper wrapper) {
        log.info("Processing Account Maintenance (cstmrreg.07)");
        // TODO: Update account details
    }

    private void processAccountClosing(MpcssMessageWrapper wrapper) {
        log.info("Processing Account Closing (cstmrreg.08)");
        // TODO: Close account, notify AHI if PSP-opened account (Section 6.2.2)
    }

    // MPCSSv2 New Registration Messages
    private void processAccountsListRequest(MpcssMessageWrapper wrapper) {
        log.info("Processing Accounts List Request (cstmrreg.30) - AHI role");
        // TODO: As AHI, return list of accounts for customer (Section 6.2.1 step 6-7)
        // Response includes: account list, auth method, PIN set indicator, OTP flag
    }

    private void processAccountsListResponse(MpcssMessageWrapper wrapper) {
        log.info("Processing Accounts List Response (cstmrreg.31) - PSP role");
        // TODO: As PSP, receive account list from AHI to display to customer
    }

    private void processSetPinRequest(MpcssMessageWrapper wrapper) {
        log.info("Processing Set PIN Request (cstmrreg.33) - AHI role");
        // TODO: As AHI, decrypt MPIN using private key, validate auth, set PIN (Section 6.2.3)
    }

    private void processSetPinResponse(MpcssMessageWrapper wrapper) {
        log.info("Processing Set PIN Response (cstmrreg.34) - PSP role");
        // TODO: As PSP, notify customer of PIN creation result
    }

    private void processGetParticipantsListRequest(MpcssMessageWrapper wrapper) {
        log.info("Processing Get Participants List Request (cstmrreg.35)");
        // TODO: Section 6.2.5 - Pull list of defined participants
    }

    private void processGetParticipantsListResponse(MpcssMessageWrapper wrapper) {
        log.info("Processing Get Participants List Response (cstmrreg.36)");
        // TODO: Store participant list with PKI public keys
    }

    private void processGetWalletsListRequest(MpcssMessageWrapper wrapper) {
        log.info("Processing Get Wallets List Request (cstmrreg.37)");
    }

    private void processGetWalletsListResponse(MpcssMessageWrapper wrapper) {
        log.info("Processing Get Wallets List Response (cstmrreg.38)");
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Recurring Payment Processing (Section 6.3.3)
    // ═══════════════════════════════════════════════════════════════════════

    private void processRecurringPaymentMessage(MpcssMessageWrapper wrapper) {
        switch (wrapper.getMessageType()) {
            case PAIN_009 -> {
                log.info("Processing Recurring Payment Approval Request (pain.009)");
                // TODO: Section 6.3.3.1 - Display recurring payment details to payer
                // Obtain payer consent, process PIN through PEP, send approval to AHI
            }
            case PAIN_011 -> {
                log.info("Processing Recurring Payment Cancellation Request (pain.011)");
                // TODO: Section 6.3.3.3 - Cancel previously approved recurring payment
            }
            case PAIN_012 -> {
                log.info("Processing Recurring Payment Status Update (pain.012)");
                // TODO: Handle approval confirmation/authorization status
            }
            default -> log.warn("Unknown recurring payment type: {}", wrapper.getMessageType());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Pre-Authorized Payment Processing (Section 6.3.4)
    // ═══════════════════════════════════════════════════════════════════════

    private void processPreAuthorizedPaymentMessage(MpcssMessageWrapper wrapper) {
        switch (wrapper.getMessageType()) {
            case CAMT_056 -> {
                log.info("Processing Pre-Auth Payment Void/Expiry Request (camt.056)");
                // TODO: Section 6.3.4.3 - Void an active pre-authorized payment
            }
            case CAMT_029 -> {
                log.info("Processing Pre-Auth Payment Void/Expiry Status Update (camt.029)");
                // TODO: Handle void/expiry status response
            }
            default -> log.warn("Unknown pre-auth payment type: {}", wrapper.getMessageType());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Binary Message Processing
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Process binary messages (Section 10.4):
     * - NCP Report (Section 8.3.1)
     * - Reconciliation Report (Section 8.3.3)
     * - Transaction Report (Section 8.3.2)
     * - Bulk Registration Response
     *
     * Binary content is ZIP-compressed (Section 10.4.4).
     */
    public void processBinaryMessage(String messageId, String messageType, byte[] compressedContent) {
        log.info("Processing binary message - ID: {}, Type: {}, Size: {} bytes",
                messageId, messageType, compressedContent.length);

        try {
            // Decompress ZIP content
            byte[] decompressedContent = decompressZip(compressedContent);
            String contentStr = new String(decompressedContent);

            switch (messageType) {
                case "NCP_REPORT" -> processNcpReport(messageId, contentStr);
                case "RECON_REPORT" -> processReconciliationReport(messageId, contentStr);
                case "TRANS_REPORT" -> processTransactionReport(messageId, contentStr);
                case "REGISTRATION_RESPONSE" -> processBulkRegistrationResponse(messageId, contentStr);
                default -> log.warn("Unknown binary message type: {}", messageType);
            }
        } catch (Exception e) {
            log.error("Error processing binary message {}: {}", messageId, e.getMessage(), e);
        }
    }

    private void processNcpReport(String messageId, String content) {
        log.info("Processing NCP Report: {}", messageId);
        // TODO: Parse NCP report XML (Section 8.3.1)
        // Fields: totalDebit, totalCredit, netPosition, settlementDate,
        //         sessionSequence, currency, participant, settlementRetry
    }

    private void processReconciliationReport(String messageId, String content) {
        log.info("Processing Reconciliation Report: {}", messageId);
        // TODO: Parse reconciliation report (Section 8.3.3)
    }

    private void processTransactionReport(String messageId, String content) {
        log.info("Processing Transaction Report: {}", messageId);
        // TODO: Parse transaction report (Section 8.3.2)
        // Contains per-transaction details: type, amount, payer/payee info, status, fees
    }

    private void processBulkRegistrationResponse(String messageId, String content) {
        log.info("Processing Bulk Registration Response: {}", messageId);
        // TODO: Parse CSV response (Section 4.2.7)
        // Each line: lineNumber, originalMessageId, status (ACPT/RJCT), reasonCode
    }

    // ─── Helpers ────────────────────────────────────────────────────────

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
        log.error("Processing error for message type {}: {}",
                wrapper.getType(), e.getMessage());

        if (wrapper.getMessageType() != null && wrapper.getMessageType().isPaymentMessage()) {
            responseProducer.sendRejectionReply(
                    wrapper,
                    PaymentRejectReasonCode.TECHNICAL_ERROR,
                    "Internal processing error: " + e.getMessage()
            );
        }
    }
}

