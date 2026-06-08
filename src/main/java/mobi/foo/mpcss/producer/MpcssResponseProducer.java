package mobi.foo.mpcss.producer;

import mobi.foo.mpcss.config.MpcssProperties;
import mobi.foo.mpcss.enums.MpcssMessageType;
import mobi.foo.mpcss.enums.PaymentRejectReasonCode;
import mobi.foo.mpcss.model.MpcssMessageWrapper;
import mobi.foo.mpcss.signature.DigitalSignatureValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Producer for sending outward messages to PS-mpClear via ActiveMQ Artemis.
 * Handles message construction, digital signing, and queue routing.
 *
 * <p>Messages are sent to outward queues as defined in Section 10:
 * <ul>
 *   <li>Payment Outward: pacs.008 credit transfer requests</li>
 *   <li>Reply Outward: pacs.002 payment status reports</li>
 *   <li>Registration Outward: cstmrreg.* registration requests</li>
 *   <li>Heartbeat Outward: heartbeat requests</li>
 *   <li>Payment Enquiry Outward: payment enquiry requests</li>
 *   <li>Name Verification Outward: name verification requests (cstmrreg.20)</li>
 *   <li>Default Account Outward: default account check requests (cstmrreg.25)</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MpcssResponseProducer {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final JmsTemplate jmsTemplate;
    private final MpcssProperties properties;
    private final DigitalSignatureValidator signatureValidator;

    /**
     * Send a pacs.002 Payment Status Report reply to PS-mpClear (Section 5.2).
     * Used by instructed PSP to respond to received payment messages.
     *
     * @param originalWrapper  The original inward message wrapper
     * @param originalMsgId    The original message ID being replied to
     * @param originalMsgNmId  The original message name (e.g. "pacs.008.001.05")
     * @param status           Group status: "ACSP", "ACSC", or "RJCT"
     * @param reasonCode       Rejection reason code (null if accepted)
     * @param additionalInfo   Additional information text
     */
    public void sendPaymentStatusReply(MpcssMessageWrapper originalWrapper,
                                        String originalMsgId,
                                        String originalMsgNmId,
                                        String status,
                                        String reasonCode,
                                        String additionalInfo) {
        String msgId = generateMessageId();
        String now = LocalDateTime.now().format(DATE_FORMAT);

        // Build pacs.002.001.06 XML
        String pacs002Content = buildPaymentStatusReport(
                msgId, now, originalMsgId, originalMsgNmId, status, reasonCode, additionalInfo);

        // Sign the content
        String signature = signatureValidator.signMessage(pacs002Content, now);

        // Wrap in MPCSS envelope
        String fullMessage = buildMessageEnvelope("PACS.002", "MX", now, signature, pacs002Content);

        // Send to reply outward queue
        sendToQueue(properties.getQueues().getReplyOutward(), fullMessage,
                originalWrapper.getCorrelationId());

        log.info("Sent pacs.002 reply - MsgId: {}, OrigMsgId: {}, Status: {}",
                msgId, originalMsgId, status);
    }

    /**
     * Send a rejection reply for signature or processing failures.
     */
    public void sendRejectionReply(MpcssMessageWrapper originalWrapper,
                                    PaymentRejectReasonCode reason,
                                    String additionalInfo) {
        sendPaymentStatusReply(
                originalWrapper,
                "UNKNOWN",  // Original MsgId might not be available if parsing failed
                originalWrapper.getType(),
                "RJCT",
                String.valueOf(reason.getCode()),
                additionalInfo
        );
    }

    /**
     * Send a payment message (pacs.008) to the payment outward queue.
     */
    public void sendPaymentMessage(String pacs008Content) {
        String now = LocalDateTime.now().format(DATE_FORMAT);
        String signature = signatureValidator.signMessage(pacs008Content, now);
        String fullMessage = buildMessageEnvelope("PACS.008", "MX", now, signature, pacs008Content);
        sendToQueue(properties.getQueues().getPaymentOutward(), fullMessage, null);
        log.info("Sent pacs.008 payment to outward queue");
    }

    /**
     * Send a registration message (cstmrreg.*) to the registration outward queue.
     */
    public void sendRegistrationMessage(String messageTypeCode, String registrationContent) {
        String now = LocalDateTime.now().format(DATE_FORMAT);
        String signature = signatureValidator.signMessage(registrationContent, now);
        String fullMessage = buildMessageEnvelope(messageTypeCode, "", now, signature, registrationContent);
        sendToQueue(properties.getQueues().getRegistrationOutward(), fullMessage, null);
        log.info("Sent {} registration to outward queue", messageTypeCode);
    }

    /**
     * Send a heartbeat request message.
     */
    public void sendHeartbeatRequest() {
        String now = LocalDateTime.now().format(DATE_FORMAT);
        String content = "<HeartBeatReq><MsgId>" + generateMessageId() + "</MsgId></HeartBeatReq>";
        String signature = signatureValidator.signMessage(content, now);
        String fullMessage = buildMessageEnvelope("HEARTBEAT", "", now, signature, content);
        sendToQueue(properties.getQueues().getHeartbeatOutward(), fullMessage, null);
        log.debug("Sent heartbeat request");
    }

    /**
     * Send a customer name verification request (cstmrreg.20.01) - Section 5.6.
     */
    public void sendNameVerificationRequest(String nameVerificationContent) {
        String now = LocalDateTime.now().format(DATE_FORMAT);
        String signature = signatureValidator.signMessage(nameVerificationContent, now);
        String fullMessage = buildMessageEnvelope("CSTMRREG.20", "", now, signature, nameVerificationContent);
        sendToQueue(properties.getQueues().getNameVerificationOutward(), fullMessage, null);
        log.info("Sent name verification request");
    }

    /**
     * Send a check default account request (cstmrreg.25.01) - Section 5.7.
     */
    public void sendDefaultAccountCheckRequest(String checkAccountContent) {
        String now = LocalDateTime.now().format(DATE_FORMAT);
        String signature = signatureValidator.signMessage(checkAccountContent, now);
        String fullMessage = buildMessageEnvelope("CSTMRREG.25", "", now, signature, checkAccountContent);
        sendToQueue(properties.getQueues().getDefaultAccountOutward(), fullMessage, null);
        log.info("Sent default account check request");
    }

    /**
     * Send a payment enquiry request - Section 5.3.
     */
    public void sendPaymentEnquiryRequest(String enquiryContent) {
        String now = LocalDateTime.now().format(DATE_FORMAT);
        String signature = signatureValidator.signMessage(enquiryContent, now);
        String fullMessage = buildMessageEnvelope("PAYMENT_ENQUIRY", "", now, signature, enquiryContent);
        sendToQueue(properties.getQueues().getPaymentEnquiryOutward(), fullMessage, null);
        log.info("Sent payment enquiry request");
    }

    /**
     * Send a bulk registration request (binary, compressed) - Section 4.2.
     */
    public void sendBulkRegistrationRequest(String fileName, byte[] csvContent) {
        try {
            byte[] compressed = compressToZip(fileName, csvContent);
            String signature = signatureValidator.signBinaryMessage(compressed);

            jmsTemplate.send(properties.getQueues().getBulkRegistrationOutward(), session -> {
                var bytesMessage = session.createBytesMessage();
                bytesMessage.writeBytes(compressed);
                bytesMessage.setStringProperty("messageID", fileName);
                bytesMessage.setStringProperty("messageType", "REGISTRATION_REQUEST");
                bytesMessage.setStringProperty("digitalSignature", signature);
                return bytesMessage;
            });

            log.info("Sent bulk registration request: {}", fileName);
        } catch (Exception e) {
            log.error("Failed to send bulk registration: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to send bulk registration", e);
        }
    }

    // ─── Helper Methods ─────────────────────────────────────────────────

    /**
     * Build the MPCSS message XML envelope (Section 10.2).
     */
    private String buildMessageEnvelope(String type, String format, String date,
                                         String signature, String content) {
        return """
                <MpcssMessage>
                    <type>%s</type>
                    <format>%s</format>
                    <date>%s</date>
                    <signature>%s</signature>
                    <content>%s</content>
                </MpcssMessage>""".formatted(type, format, date, signature, escapeXml(content));
    }

    /**
     * Build pacs.002.001.06 Payment Status Report XML (Section 5.2).
     */
    private String buildPaymentStatusReport(String msgId, String creDtTm,
                                             String origMsgId, String origMsgNmId,
                                             String status, String reasonCode,
                                             String additionalInfo) {
        StringBuilder sb = new StringBuilder();
        sb.append("<Document xmlns=\"urn:iso:std:iso:20022:tech:xsd:pacs.002.001.06\">");
        sb.append("<FIToFIPmtStsRpt>");

        // Group Header
        sb.append("<GrpHdr>");
        sb.append("<MsgId>").append(msgId).append("</MsgId>");
        sb.append("<CreDtTm>").append(creDtTm).append("</CreDtTm>");
        sb.append("</GrpHdr>");

        // Original Group Information
        sb.append("<OrgnlGrpInfAndSts>");
        sb.append("<OrgnlMsgId>").append(origMsgId).append("</OrgnlMsgId>");
        sb.append("<OrgnlMsgNmId>").append(origMsgNmId).append("</OrgnlMsgNmId>");
        sb.append("<GrpSts>").append(status).append("</GrpSts>");

        // Reason (for rejections)
        if (reasonCode != null) {
            sb.append("<StsRsnInf>");
            sb.append("<Rsn><Prtry>").append(reasonCode).append("</Prtry></Rsn>");
            if (additionalInfo != null) {
                sb.append("<AddtlInf>").append(escapeXml(additionalInfo)).append("</AddtlInf>");
            }
            sb.append("</StsRsnInf>");
        }

        sb.append("</OrgnlGrpInfAndSts>");
        sb.append("</FIToFIPmtStsRpt>");
        sb.append("</Document>");

        return sb.toString();
    }

    /**
     * Generate message ID: {ParticipantNumericCode}{12-char unique reference} (Section 10.7).
     */
    private String generateMessageId() {
        String prefix = properties.getParticipant().getNumericCode();
        String reference = String.format("%012d", System.nanoTime() % 1_000_000_000_000L);
        return prefix + reference;
    }

    private void sendToQueue(String queueName, String messageContent, String correlationId) {
        jmsTemplate.send(queueName, session -> {
            var textMessage = session.createTextMessage(messageContent);
            if (correlationId != null) {
                textMessage.setJMSCorrelationID(correlationId);
            }
            textMessage.setStringProperty("certificateNumber",
                    properties.getParticipant().getShortName());
            return textMessage;
        });
    }

    private byte[] compressToZip(String fileName, byte[] content) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            zos.putNextEntry(new ZipEntry(fileName));
            zos.write(content);
            zos.closeEntry();
        }
        return baos.toByteArray();
    }

    private String escapeXml(String text) {
        if (text == null) return "";
        // Only escape if content contains problematic chars and isn't already XML
        if (text.startsWith("<") && text.endsWith(">")) {
            return "<![CDATA[" + text + "]]>";
        }
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}

