package mobi.foo.mpcss.listener;

import jakarta.jms.BytesMessage;
import jakarta.jms.Message;
import jakarta.jms.TextMessage;
import mobi.foo.mpcss.enums.MpcssMessageType;
import mobi.foo.mpcss.model.MpcssMessageWrapper;
import mobi.foo.mpcss.parser.Iso20022XmlParser;
import mobi.foo.mpcss.processor.MpcssMessageProcessor;
import mobi.foo.mpcss.signature.DigitalSignatureValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

/**
 * JMS Message Listener for consuming messages from PS-mpClear ActiveMQ Artemis queues.
 *
 * <p>Listens on all inward queues as defined in Section 10 of the MPCSS document:
 * <ul>
 *   <li>Payment Inward - pacs.008 credit transfers forwarded by PS-mpClear</li>
 *   <li>Reply Inward - pacs.002 payment status reports</li>
 *   <li>Registration Inward - cstmrreg.10 registration responses</li>
 *   <li>Heartbeat Inward - heartbeat responses</li>
 *   <li>Payment Enquiry Inward - payment enquiry responses</li>
 *   <li>Name Verification Inward - customer name verification responses</li>
 *   <li>Default Account Inward - check default account responses</li>
 *   <li>Reports Inward - NCP, reconciliation, transaction reports (binary)</li>
 *   <li>Bulk Registration Inward - bulk registration responses (binary)</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MpcssMessageListener {

    private final Iso20022XmlParser xmlParser;
    private final DigitalSignatureValidator signatureValidator;
    private final MpcssMessageProcessor messageProcessor;

    // ═══════════════════════════════════════════════════════════════════════
    // Payment Inward Queue - Receives payment messages (pacs.008) from PS-mpClear
    // ═══════════════════════════════════════════════════════════════════════
    @JmsListener(
            destination = "${mpcss.queues.payment-inward}",
            containerFactory = "jmsListenerContainerFactory"
    )
    public void onPaymentInward(Message message) {
        processNonBinaryMessage(message, "PAYMENT_INWARD");
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Reply Inward Queue - Receives payment status reports (pacs.002)
    // ═══════════════════════════════════════════════════════════════════════
    @JmsListener(
            destination = "${mpcss.queues.reply-inward}",
            containerFactory = "jmsListenerContainerFactory"
    )
    public void onReplyInward(Message message) {
        processNonBinaryMessage(message, "REPLY_INWARD");
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Registration Inward Queue - Receives registration responses (cstmrreg.10)
    // ═══════════════════════════════════════════════════════════════════════
    @JmsListener(
            destination = "${mpcss.queues.registration-inward}",
            containerFactory = "jmsListenerContainerFactory"
    )
    public void onRegistrationInward(Message message) {
        processNonBinaryMessage(message, "REGISTRATION_INWARD");
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Heartbeat Inward Queue - Receives heartbeat responses
    // ═══════════════════════════════════════════════════════════════════════
    @JmsListener(
            destination = "${mpcss.queues.heartbeat-inward}",
            containerFactory = "jmsListenerContainerFactory"
    )
    public void onHeartbeatInward(Message message) {
        processNonBinaryMessage(message, "HEARTBEAT_INWARD");
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Payment Enquiry Inward Queue
    // ═══════════════════════════════════════════════════════════════════════
    @JmsListener(
            destination = "${mpcss.queues.payment-enquiry-inward}",
            containerFactory = "jmsListenerContainerFactory"
    )
    public void onPaymentEnquiryInward(Message message) {
        processNonBinaryMessage(message, "PAYMENT_ENQUIRY_INWARD");
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Customer Name Inward Queue (Name Verification)
    // ═══════════════════════════════════════════════════════════════════════
    @JmsListener(
            destination = "${mpcss.queues.customer-name-inward}",
            containerFactory = "jmsListenerContainerFactory"
    )
    public void onCustomerNameInward(Message message) {
        processNonBinaryMessage(message, "CUSTOMER_NAME_INWARD");
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Check Default Inward Queue
    // ═══════════════════════════════════════════════════════════════════════
    @JmsListener(
            destination = "${mpcss.queues.check-default-inward}",
            containerFactory = "jmsListenerContainerFactory"
    )
    public void onCheckDefaultInward(Message message) {
        processNonBinaryMessage(message, "CHECK_DEFAULT_INWARD");
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Reports Inward Queue - Binary (NCP, Reconciliation, Transaction reports)
    // ═══════════════════════════════════════════════════════════════════════
    @JmsListener(
            destination = "${mpcss.queues.reports-inward}",
            containerFactory = "binaryJmsListenerContainerFactory"
    )
    public void onReportsInward(Message message) {
        processBinaryMessage(message, "REPORTS_INWARD");
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Bulk Registration Inward Queue - Binary (compressed ZIP)
    // ═══════════════════════════════════════════════════════════════════════
    @JmsListener(
            destination = "${mpcss.queues.bulk-registration-inward}",
            containerFactory = "binaryJmsListenerContainerFactory"
    )
    public void onBulkRegistrationInward(Message message) {
        processBinaryMessage(message, "BULK_REGISTRATION_INWARD");
    }

    // ─── Private Processing Methods ─────────────────────────────────────

    /**
     * Process non-binary (XML) messages.
     * Follows the message structure defined in Section 10.2.
     */
    private void processNonBinaryMessage(Message message, String queueLabel) {
        try {
            if (!(message instanceof TextMessage textMessage)) {
                log.warn("[{}] Received non-text message, skipping. Type: {}",
                        queueLabel, message.getClass().getSimpleName());
                message.acknowledge();
                return;
            }

            String rawXml = textMessage.getText();
            String jmsMessageId = message.getJMSMessageID();
            String correlationId = message.getJMSCorrelationID();
            String certificateNumber = message.getStringProperty("certificateNumber");

            log.info("[{}] Received message. JMS-ID: {}, CorrelationID: {}",
                    queueLabel, jmsMessageId, correlationId);
            log.debug("[{}] Raw XML (first 500 chars): {}", queueLabel,
                    rawXml != null && rawXml.length() > 500 ? rawXml.substring(0, 500) + "..." : rawXml);

            // 1. Parse the MPCSS wrapper envelope
            MpcssMessageWrapper wrapper = xmlParser.parseMessageWrapper(rawXml);
            wrapper.setCorrelationId(correlationId);
            wrapper.setCertificateNumber(certificateNumber);

            // 2. Resolve message type
            MpcssMessageType msgType = MpcssMessageType.fromCode(wrapper.getType());
            wrapper.setMessageType(msgType);

            log.info("[{}] Message type: {} ({})", queueLabel, msgType.getCode(), msgType.getDescription());

            // 3. Verify digital signature (Section 10.6)
            boolean signatureValid = signatureValidator.verifySignature(
                    wrapper.getContent(),
                    wrapper.getDate(),
                    wrapper.getSignature(),
                    certificateNumber
            );

            if (!signatureValid) {
                log.error("[{}] Digital signature verification FAILED for message: {}",
                        queueLabel, jmsMessageId);
                // Still acknowledge to prevent redelivery loop, but process as rejected
                messageProcessor.handleSignatureFailure(wrapper);
                message.acknowledge();
                return;
            }

            // 4. Delegate to processor
            messageProcessor.processMessage(wrapper);

            // 5. Acknowledge
            message.acknowledge();
            log.info("[{}] Message processed and acknowledged: {}", queueLabel, jmsMessageId);

        } catch (Exception e) {
            log.error("[{}] Error processing message: {}", queueLabel, e.getMessage(), e);
            // Don't acknowledge - let JMS redeliver
        }
    }

    /**
     * Process binary messages (reports, bulk registration).
     * Binary messages have headers: messageID, messageType, digitalSignature (Section 10.4).
     */
    private void processBinaryMessage(Message message, String queueLabel) {
        try {
            String messageId = message.getStringProperty("messageID");
            String messageTypeStr = message.getStringProperty("messageType");
            String digitalSignature = message.getStringProperty("digitalSignature");

            log.info("[{}] Received binary message. ID: {}, Type: {}",
                    queueLabel, messageId, messageTypeStr);

            byte[] content;
            if (message instanceof BytesMessage bytesMessage) {
                content = new byte[(int) bytesMessage.getBodyLength()];
                bytesMessage.readBytes(content);
            } else {
                log.warn("[{}] Expected BytesMessage but got: {}", queueLabel,
                        message.getClass().getSimpleName());
                message.acknowledge();
                return;
            }

            // Verify binary digital signature
            boolean signatureValid = signatureValidator.verifyBinarySignature(
                    content, digitalSignature, null);

            if (!signatureValid) {
                log.error("[{}] Binary message signature verification FAILED: {}", queueLabel, messageId);
                message.acknowledge();
                return;
            }

            // Process binary content (decompression + processing)
            messageProcessor.processBinaryMessage(messageId, messageTypeStr, content);

            message.acknowledge();
            log.info("[{}] Binary message processed and acknowledged: {}", queueLabel, messageId);

        } catch (Exception e) {
            log.error("[{}] Error processing binary message: {}", queueLabel, e.getMessage(), e);
        }
    }
}

