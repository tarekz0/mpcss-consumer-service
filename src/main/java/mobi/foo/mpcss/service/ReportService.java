package mobi.foo.mpcss.service;

import mobi.foo.mpcss.client.CoreBankingClient;
import mobi.foo.mpcss.entity.InwardMessage;
import mobi.foo.mpcss.repository.InwardMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Service for handling binary report messages from MPCSS:
 * - NCP Report (end-of-session net clearing positions)
 * - Reconciliation Report
 * - Transaction Report
 * - Bulk Registration Response
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {

    private final InwardMessageRepository inwardMessageRepository;
    private final CoreBankingClient coreBankingClient;

    @Transactional
    public void handleNcpReport(String messageId, String content) {
        log.info("Processing NCP Report - SessionId: {}", messageId);

        InwardMessage msg = saveInwardMessage(messageId, "NCP_REPORT", content);

        // Forward to core banking
        coreBankingClient.forwardReport("NCP_REPORT", content, messageId);

        msg.setProcessed(true);
        msg.setProcessedAt(LocalDateTime.now());
        inwardMessageRepository.save(msg);

        log.info("NCP Report forwarded to core banking: {}", messageId);
    }

    @Transactional
    public void handleReconciliationReport(String messageId, String content) {
        log.info("Processing Reconciliation Report - SessionId: {}", messageId);

        InwardMessage msg = saveInwardMessage(messageId, "RECON_REPORT", content);

        coreBankingClient.forwardReport("RECON_REPORT", content, messageId);

        msg.setProcessed(true);
        msg.setProcessedAt(LocalDateTime.now());
        inwardMessageRepository.save(msg);

        log.info("Reconciliation Report forwarded: {}", messageId);
    }

    @Transactional
    public void handleTransactionReport(String messageId, String content) {
        log.info("Processing Transaction Report - SessionId: {}", messageId);

        InwardMessage msg = saveInwardMessage(messageId, "TRANS_REPORT", content);

        coreBankingClient.forwardReport("TRANS_REPORT", content, messageId);

        msg.setProcessed(true);
        msg.setProcessedAt(LocalDateTime.now());
        inwardMessageRepository.save(msg);

        log.info("Transaction Report forwarded: {}", messageId);
    }

    @Transactional
    public void handleBulkRegistrationResponse(String messageId, String content) {
        log.info("Processing Bulk Registration Response - FileId: {}", messageId);

        InwardMessage msg = saveInwardMessage(messageId, "REGISTRATION_RESPONSE", content);

        coreBankingClient.forwardReport("REGISTRATION_RESPONSE", content, messageId);

        msg.setProcessed(true);
        msg.setProcessedAt(LocalDateTime.now());
        inwardMessageRepository.save(msg);

        log.info("Bulk Registration Response forwarded: {}", messageId);
    }

    /**
     * Log any inward message for audit trail.
     */
    @Transactional
    public InwardMessage logInwardMessage(String jmsMessageId, String correlationId,
                                           String messageType, String queueName,
                                           boolean signatureValid, String rawContent) {
        InwardMessage msg = InwardMessage.builder()
                .jmsMessageId(jmsMessageId)
                .correlationId(correlationId)
                .messageType(messageType)
                .queueName(queueName)
                .signatureValid(signatureValid)
                .rawContent(rawContent)
                .processed(false)
                .build();
        return inwardMessageRepository.save(msg);
    }

    private InwardMessage saveInwardMessage(String messageId, String type, String content) {
        return InwardMessage.builder()
                .jmsMessageId(messageId)
                .messageType(type)
                .queueName("reports-inward")
                .signatureValid(true)
                .rawContent(content)
                .processed(false)
                .build();
    }
}

