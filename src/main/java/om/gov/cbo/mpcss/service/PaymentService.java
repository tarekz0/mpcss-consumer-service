package om.gov.cbo.mpcss.service;

import om.gov.cbo.mpcss.client.CoreBankingClient;
import om.gov.cbo.mpcss.config.MpcssProperties;
import om.gov.cbo.mpcss.dto.CreditTransferMessage;
import om.gov.cbo.mpcss.dto.request.PaymentRequest;
import om.gov.cbo.mpcss.dto.response.PaymentResponse;
import om.gov.cbo.mpcss.entity.MpcssTransaction;
import om.gov.cbo.mpcss.entity.MpcssTransaction.Direction;
import om.gov.cbo.mpcss.producer.MpcssResponseProducer;
import om.gov.cbo.mpcss.repository.TransactionRepository;
import om.gov.cbo.mpcss.util.MessageIdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Service handling all payment-related operations for PSP role.
 * <p>
 * Outward flow (PSP sends payment):
 *   Mobile App → REST API → PaymentService → MpcssResponseProducer → ActiveMQ → MPCSS
 * <p>
 * Inward flow (PSP receives payment/status):
 *   MPCSS → ActiveMQ → MpcssMessageListener → MpcssMessageProcessor → PaymentService → CoreBankingClient
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final TransactionRepository transactionRepository;
    private final MpcssResponseProducer responseProducer;
    private final CoreBankingClient coreBankingClient;
    private final MessageIdGenerator messageIdGenerator;
    private final MpcssProperties properties;

    // ═══════════════════════════════════════════════════════════════════
    // OUTWARD — PSP sends payment to MPCSS (initiated by mobile app)
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Initiate a Direct Credit payment (pacs.008) to MPCSS.
     * Called from REST API when customer wants to send money.
     */
    @Transactional
    public PaymentResponse initiatePayment(PaymentRequest request) {
        String messageId = messageIdGenerator.generate();
        String now = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME);

        log.info("Initiating payment - MsgId: {}, Amount: {} {}, From: {}, To: {}",
                messageId, request.getAmount(), request.getCurrency(),
                request.getDebtorAccount(), request.getCreditorIdentifier());

        // 1. Build pacs.008 XML content
        String pacs008Xml = buildPacs008Xml(messageId, now, request);

        // 2. Save transaction in DB
        MpcssTransaction txn = MpcssTransaction.builder()
                .messageId(messageId)
                .messageType("PACS_008")
                .direction(Direction.OUTWARD)
                .status("SENT")
                .amount(request.getAmount())
                .currency(request.getCurrency() != null ? request.getCurrency() : "OMR")
                .settlementDate(LocalDate.now())
                .categoryPurpose(request.getCategoryPurpose() != null ? request.getCategoryPurpose() : "MP")
                .localInstrumentCode("TEL")
                .debtorName(request.getDebtorName())
                .debtorAccountId(request.getDebtorAccount())
                .debtorAgentBic(properties.getParticipant().getBic())
                .creditorName(request.getCreditorName())
                .creditorAccountId(request.getCreditorIdentifier())
                .endToEndId(request.getEndToEndId() != null ? request.getEndToEndId() : messageId)
                .transactionId(messageId)
                .remittanceInfo(request.getDescription())
                .pepEncryptedData(request.getPepEncryptedData())
                .rawOutwardXml(pacs008Xml)
                .build();

        transactionRepository.save(txn);

        // 3. Send to MPCSS via outward payment queue
        responseProducer.sendPaymentMessage(pacs008Xml);

        log.info("Payment sent to MPCSS - MsgId: {}", messageId);

        return PaymentResponse.builder()
                .messageId(messageId)
                .status("SENT")
                .statusDescription("Payment submitted to MPCSS for processing")
                .amount(request.getAmount())
                .currency(txn.getCurrency())
                .debtorName(request.getDebtorName())
                .debtorAccount(request.getDebtorAccount())
                .creditorName(request.getCreditorName())
                .creditorAccount(request.getCreditorIdentifier())
                .endToEndId(txn.getEndToEndId())
                .createdAt(txn.getCreatedAt())
                .build();
    }

    // ═══════════════════════════════════════════════════════════════════
    // INWARD — PSP receives from MPCSS
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Handle inward credit transfer (pacs.008) from MPCSS.
     * Another PSP sent money to our customer — we need to credit them.
     */
    @Transactional
    public void handleInwardCreditTransfer(CreditTransferMessage credit, String rawXml, String correlationId) {
        log.info("Handling inward credit - MsgId: {}, Creditor: {}, Amount: {} {}",
                credit.getMessageId(), credit.getCreditorAccountId(),
                credit.getTransactionAmount(), credit.getSettlementCurrency());

        // 1. Save inward transaction
        MpcssTransaction txn = MpcssTransaction.builder()
                .messageId(credit.getMessageId())
                .messageType("PACS_008")
                .direction(Direction.INWARD)
                .status("RECEIVED")
                .amount(credit.getTransactionAmount())
                .currency(credit.getSettlementCurrency())
                .settlementDate(credit.getInterbankSettlementDate())
                .localInstrumentCode(credit.getLocalInstrumentCode())
                .categoryPurpose(credit.getCategoryPurpose())
                .debtorName(credit.getDebtorName())
                .debtorAccountId(credit.getDebtorAccountId())
                .debtorAgentBic(credit.getDebtorAgentBic())
                .creditorName(credit.getCreditorName())
                .creditorAccountId(credit.getCreditorAccountId())
                .creditorAgentBic(credit.getCreditorAgentBic())
                .endToEndId(credit.getEndToEndId())
                .transactionId(credit.getTransactionId())
                .instructionId(credit.getInstructionId())
                .remittanceInfo(credit.getRemittanceInfo())
                .onUs(credit.isOnUs())
                .correlationId(correlationId)
                .rawInwardXml(rawXml)
                .build();

        transactionRepository.save(txn);

        // 2. Forward to core banking for crediting
        var cbResponse = coreBankingClient.forwardCreditTransfer(credit);

        if (cbResponse.isSuccess()) {
            txn.setStatus("ACCEPTED");
            txn.setForwardedToCoreBanking(true);
            txn.setCoreBankingRef(cbResponse.getReferenceId());
            transactionRepository.save(txn);
            log.info("Credit forwarded to core banking successfully: {}", credit.getMessageId());
        } else {
            txn.setStatus("REJECTED");
            txn.setReplyReasonCode("1");  // Invalid account or core banking error
            txn.setReplyAdditionalInfo(cbResponse.getMessage());
            transactionRepository.save(txn);
            log.warn("Core banking rejected credit: {} - {}", credit.getMessageId(), cbResponse.getMessage());
        }
    }

    /**
     * Handle inward payment status report (pacs.002) from MPCSS.
     * This is a reply to our outward payment — tells us if it was accepted or rejected.
     */
    @Transactional
    public void handlePaymentStatusReport(String originalMsgId, String status,
                                           String reasonCode, String additionalInfo,
                                           String rawXml) {
        log.info("Handling status report - OrigMsgId: {}, Status: {}, Reason: {}",
                originalMsgId, status, reasonCode);

        transactionRepository.findByMessageId(originalMsgId).ifPresentOrElse(txn -> {
            txn.setReplyStatus(status);
            txn.setReplyReasonCode(reasonCode);
            txn.setReplyAdditionalInfo(additionalInfo);
            txn.setRawInwardXml(rawXml);
            txn.setRepliedAt(LocalDateTime.now());

            switch (status) {
                case "ACSP" -> {
                    txn.setStatus("ACCEPTED");
                    log.info("Payment accepted for settlement: {}", originalMsgId);
                }
                case "ACSC" -> {
                    txn.setStatus("SETTLED");
                    log.info("Payment settlement completed: {}", originalMsgId);
                }
                case "RJCT" -> {
                    txn.setStatus("REJECTED");
                    log.warn("Payment rejected: {} - Reason: {} ({})",
                            originalMsgId, reasonCode, additionalInfo);
                }
                default -> log.warn("Unknown status: {} for message: {}", status, originalMsgId);
            }

            transactionRepository.save(txn);

            // Forward status to core banking
            var statusReport = new om.gov.cbo.mpcss.dto.PaymentStatusReport();
            statusReport.setOriginalMessageId(originalMsgId);
            statusReport.setGroupStatus(om.gov.cbo.mpcss.enums.PaymentStatus.fromCode(status));
            statusReport.setReasonCode(reasonCode);
            statusReport.setAdditionalInfo(additionalInfo);
            coreBankingClient.forwardPaymentStatusReport(statusReport);

        }, () -> log.error("No outward transaction found for status report: {}", originalMsgId));
    }

    // ═══════════════════════════════════════════════════════════════════
    // QUERY — REST API lookups
    // ═══════════════════════════════════════════════════════════════════

    public PaymentResponse getPaymentByMessageId(String messageId) {
        return transactionRepository.findByMessageId(messageId)
                .map(this::toPaymentResponse)
                .orElse(null);
    }

    public List<PaymentResponse> getPaymentsByStatus(String status) {
        return transactionRepository.findByStatusOrderByCreatedAtDesc(status)
                .stream().map(this::toPaymentResponse).toList();
    }

    public List<PaymentResponse> getPaymentsByAccount(String accountId, LocalDateTime from, LocalDateTime to) {
        var debits = transactionRepository.findByDebtorAccountIdAndCreatedAtBetween(accountId, from, to);
        var credits = transactionRepository.findByCreditorAccountIdAndCreatedAtBetween(accountId, from, to);
        debits.addAll(credits);
        return debits.stream().map(this::toPaymentResponse).toList();
    }

    // ═══════════════════════════════════════════════════════════════════
    // XML Builder — pacs.008.001.05
    // ═══════════════════════════════════════════════════════════════════

    private String buildPacs008Xml(String messageId, String creationDateTime, PaymentRequest request) {
        String currency = request.getCurrency() != null ? request.getCurrency() : "OMR";
        String categoryPurpose = request.getCategoryPurpose() != null ? request.getCategoryPurpose() : "MP";
        String endToEndId = request.getEndToEndId() != null ? request.getEndToEndId() : messageId;
        String settlementDate = LocalDate.now().toString();

        return """
                <Document xmlns="urn:iso:std:iso:20022:tech:xsd:pacs.008.001.05">
                  <FIToFICstmrCdtTrf>
                    <GrpHdr>
                      <MsgId>%s</MsgId>
                      <CreDtTm>%s</CreDtTm>
                      <NbOfTxs>1</NbOfTxs>
                      <TtlIntrBkSttlmAmt Ccy="%s">%s</TtlIntrBkSttlmAmt>
                      <IntrBkSttlmDt>%s</IntrBkSttlmDt>
                      <SttlmInf>
                        <SttlmMtd>CLRG</SttlmMtd>
                        <ClrSys><Prtry>CBO</Prtry></ClrSys>
                      </SttlmInf>
                      <PmtTpInf>
                        <LclInstrm><Cd>TEL</Cd></LclInstrm>
                        <CtgyPurp><Prtry>%s</Prtry></CtgyPurp>
                      </PmtTpInf>
                    </GrpHdr>
                    <CdtTrfTxInf>
                      <PmtId>
                        <InstrId>%s</InstrId>
                        <EndToEndId>%s</EndToEndId>
                        <TxId>%s</TxId>
                      </PmtId>
                      <IntrBkSttlmAmt Ccy="%s">%s</IntrBkSttlmAmt>
                      <InstgAgt><FinInstnId><BICFI>%s</BICFI></FinInstnId></InstgAgt>
                      <InstdAgt><FinInstnId><BICFI>ACH</BICFI></FinInstnId></InstdAgt>
                      <Dbtr><Nm>%s</Nm></Dbtr>
                      <DbtrAcct><Id><IBAN>%s</IBAN></Id></DbtrAcct>
                      <DbtrAgt><FinInstnId><BICFI>%s</BICFI></FinInstnId></DbtrAgt>
                      <CdtrAgt><FinInstnId><BICFI>ACH</BICFI></FinInstnId></CdtrAgt>
                      <Cdtr><Nm>%s</Nm></Cdtr>
                      <CdtrAcct><Id><Othr><Id>%s</Id></Othr></Id></CdtrAcct>
                      <RmtInf><Ustrd>%s</Ustrd></RmtInf>
                    </CdtTrfTxInf>
                  </FIToFICstmrCdtTrf>
                </Document>""".formatted(
                messageId, creationDateTime,
                currency, request.getAmount().toPlainString(),
                settlementDate,
                categoryPurpose,
                messageId, endToEndId, messageId,
                currency, request.getAmount().toPlainString(),
                properties.getParticipant().getBic(),
                escapeXml(request.getDebtorName()),
                request.getDebtorAccount(),
                properties.getParticipant().getBic(),
                escapeXml(request.getCreditorName() != null ? request.getCreditorName() : ""),
                request.getCreditorIdentifier(),
                escapeXml(request.getDescription() != null ? request.getDescription() : "")
        );
    }

    private PaymentResponse toPaymentResponse(MpcssTransaction txn) {
        return PaymentResponse.builder()
                .messageId(txn.getMessageId())
                .status(txn.getStatus())
                .statusDescription(txn.getReplyAdditionalInfo())
                .amount(txn.getAmount())
                .currency(txn.getCurrency())
                .debtorName(txn.getDebtorName())
                .debtorAccount(txn.getDebtorAccountId())
                .creditorName(txn.getCreditorName())
                .creditorAccount(txn.getCreditorAccountId())
                .endToEndId(txn.getEndToEndId())
                .reasonCode(txn.getReplyReasonCode())
                .additionalInfo(txn.getReplyAdditionalInfo())
                .createdAt(txn.getCreatedAt())
                .repliedAt(txn.getRepliedAt())
                .build();
    }

    private String escapeXml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;");
    }
}

