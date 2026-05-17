package om.gov.cbo.mpcss.service;

import om.gov.cbo.mpcss.client.CoreBankingClient;
import om.gov.cbo.mpcss.config.MpcssProperties;
import om.gov.cbo.mpcss.dto.RegistrationMessage;
import om.gov.cbo.mpcss.dto.request.RegistrationRequest;
import om.gov.cbo.mpcss.dto.response.RegistrationResponse;
import om.gov.cbo.mpcss.entity.CustomerRegistration;
import om.gov.cbo.mpcss.producer.MpcssResponseProducer;
import om.gov.cbo.mpcss.repository.CustomerRegistrationRepository;
import om.gov.cbo.mpcss.util.MessageIdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Service handling all registration operations for PSP role.
 * Manages customer/account CRUD in the MPCSS national directory.
 *
 * Registration types map to message types:
 *   CUSTOMER_OPEN    → cstmrreg.01.01
 *   CUSTOMER_MAINTAIN → cstmrreg.02.01
 *   CUSTOMER_CLOSE   → cstmrreg.03.01
 *   ACCOUNT_OPEN     → cstmrreg.06.01
 *   ACCOUNT_MAINTAIN → cstmrreg.07.01
 *   ACCOUNT_CLOSE    → cstmrreg.08.01
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RegistrationService {

    private final CustomerRegistrationRepository registrationRepository;
    private final MpcssResponseProducer responseProducer;
    private final CoreBankingClient coreBankingClient;
    private final MessageIdGenerator messageIdGenerator;
    private final MpcssProperties properties;

    private static final DateTimeFormatter DT_FORMAT = DateTimeFormatter.ISO_DATE_TIME;

    // ═══════════════════════════════════════════════════════════════════
    // OUTWARD — PSP sends registration to MPCSS
    // ═══════════════════════════════════════════════════════════════════

    @Transactional
    public RegistrationResponse submitRegistration(RegistrationRequest request) {
        String messageId = messageIdGenerator.generate();
        String messageType = resolveMessageType(request.getRegistrationType());

        log.info("Submitting registration - MsgId: {}, Type: {}, Mobile: {}",
                messageId, messageType, request.getMobileNumber());

        // Build registration XML
        String xml = buildRegistrationXml(messageId, messageType, request);

        // Determine the MPCSS message type code for the envelope
        String mpcssTypeCode = resolveMessageTypeCode(request.getRegistrationType());

        // Save to DB
        CustomerRegistration reg = CustomerRegistration.builder()
                .messageId(messageId)
                .messageType(messageType)
                .status("PENDING")
                .customerIdType(request.getCustomerIdType())
                .customerIdNumber(request.getCustomerIdNumber())
                .idIssuingCountry(request.getIdIssuingCountry())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .nickname(request.getNickname())
                .mobileNumber(request.getMobileNumber())
                .accountType(request.getAccountType())
                .registrationCode(request.getRegistrationCode())
                .isBanked(request.getIsBanked())
                .currency(request.getCurrency())
                .accountAlias(request.getAccountAlias())
                .isDefaultAccount(request.getIsDefaultAccount())
                .merchantId(request.getMerchantId())
                .additionalInfo(request.getAdditionalInfo())
                .rawOutwardXml(xml)
                .build();

        registrationRepository.save(reg);

        // Send via registration outward queue
        responseProducer.sendRegistrationMessage(mpcssTypeCode, xml);

        log.info("Registration sent to MPCSS - MsgId: {}", messageId);

        return RegistrationResponse.builder()
                .messageId(messageId)
                .registrationType(request.getRegistrationType())
                .status("PENDING")
                .mobileNumber(request.getMobileNumber())
                .customerIdNumber(request.getCustomerIdNumber())
                .createdAt(reg.getCreatedAt())
                .build();
    }

    // ═══════════════════════════════════════════════════════════════════
    // INWARD — PSP receives registration response from MPCSS
    // ═══════════════════════════════════════════════════════════════════

    @Transactional
    public void handleRegistrationResponse(RegistrationMessage response, String rawXml) {
        String originalMsgId = response.getOriginalMessageId();
        String status = response.getStatus();
        String reasonCode = response.getReasonCode();

        log.info("Handling registration response - OrigMsgId: {}, Status: {}, Reason: {}",
                originalMsgId, status, reasonCode);

        registrationRepository.findByMessageId(originalMsgId).ifPresentOrElse(reg -> {
            reg.setStatus("ACPT".equals(status) ? "ACCEPTED" : "REJECTED");
            reg.setResponseReasonCode(reasonCode);
            reg.setResponseAdditionalInfo(response.getAdditionalInfo());
            reg.setRawInwardXml(rawXml);
            registrationRepository.save(reg);

            // Forward to core banking
            coreBankingClient.forwardRegistrationResponse(response);

            log.info("Registration {} - MsgId: {}", reg.getStatus(), originalMsgId);
        }, () -> log.error("No registration found for response: {}", originalMsgId));
    }

    // ═══════════════════════════════════════════════════════════════════
    // QUERY
    // ═══════════════════════════════════════════════════════════════════

    public RegistrationResponse getRegistrationByMessageId(String messageId) {
        return registrationRepository.findByMessageId(messageId)
                .map(this::toResponse)
                .orElse(null);
    }

    public List<RegistrationResponse> getRegistrationsByMobile(String mobileNumber) {
        return registrationRepository.findByMobileNumber(mobileNumber)
                .stream().map(this::toResponse).toList();
    }

    public List<RegistrationResponse> getRegistrationsByStatus(String status) {
        return registrationRepository.findByStatusOrderByCreatedAtDesc(status)
                .stream().map(this::toResponse).toList();
    }

    // ═══════════════════════════════════════════════════════════════════
    // XML Builders
    // ═════════════════════════════════════��═════════════════════════════

    private String buildRegistrationXml(String messageId, String messageType, RegistrationRequest req) {
        StringBuilder sb = new StringBuilder();
        sb.append("<Document>");

        switch (messageType) {
            case "cstmrreg.01.01" -> buildCustomerOpenXml(sb, messageId, req);
            case "cstmrreg.02.01" -> buildCustomerMaintainXml(sb, messageId, req);
            case "cstmrreg.03.01" -> buildCustomerCloseXml(sb, messageId, req);
            case "cstmrreg.06.01" -> buildAccountOpenXml(sb, messageId, req);
            case "cstmrreg.07.01" -> buildAccountMaintainXml(sb, messageId, req);
            case "cstmrreg.08.01" -> buildAccountCloseXml(sb, messageId, req);
        }

        sb.append("</Document>");
        return sb.toString();
    }

    private void buildCustomerOpenXml(StringBuilder sb, String msgId, RegistrationRequest req) {
        sb.append("<CstmrRecOpngReq>");
        appendCommonFields(sb, msgId, req);
        sb.append("<FrstNm>").append(esc(req.getFirstName())).append("</FrstNm>");
        sb.append("<LstNm>").append(esc(req.getLastName())).append("</LstNm>");
        if (req.getNickname() != null) sb.append("<NckNm>").append(esc(req.getNickname())).append("</NckNm>");
        sb.append("<MobNb>").append(req.getMobileNumber()).append("</MobNb>");
        if (req.getAdditionalInfo() != null) sb.append("<AddtlInf>").append(esc(req.getAdditionalInfo())).append("</AddtlInf>");
        sb.append("</CstmrRecOpngReq>");
    }

    private void buildCustomerMaintainXml(StringBuilder sb, String msgId, RegistrationRequest req) {
        sb.append("<CstmrRecMntncReq>");
        appendCommonFields(sb, msgId, req);
        if (req.getFirstName() != null) sb.append("<FrstNm>").append(esc(req.getFirstName())).append("</FrstNm>");
        if (req.getLastName() != null) sb.append("<LstNm>").append(esc(req.getLastName())).append("</LstNm>");
        if (req.getNickname() != null) sb.append("<NckNm>").append(esc(req.getNickname())).append("</NckNm>");
        sb.append("<MobNb>").append(req.getMobileNumber()).append("</MobNb>");
        sb.append("</CstmrRecMntncReq>");
    }

    private void buildCustomerCloseXml(StringBuilder sb, String msgId, RegistrationRequest req) {
        sb.append("<CstmrRecClsgReq>");
        appendCommonFields(sb, msgId, req);
        sb.append("<MobNb>").append(req.getMobileNumber()).append("</MobNb>");
        if (req.getAdditionalInfo() != null) sb.append("<AddtlInf>").append(esc(req.getAdditionalInfo())).append("</AddtlInf>");
        sb.append("</CstmrRecClsgReq>");
    }

    private void buildAccountOpenXml(StringBuilder sb, String msgId, RegistrationRequest req) {
        sb.append("<AcctRecOpngReq>");
        appendCommonFields(sb, msgId, req);
        sb.append("<AcctDtls>");
        sb.append("<AcctTp>").append(req.getAccountType()).append("</AcctTp>");
        sb.append("<MobNb>").append(req.getMobileNumber()).append("</MobNb>");
        if (req.getRegistrationCode() != null) sb.append("<RegnCd>").append(req.getRegistrationCode()).append("</RegnCd>");
        sb.append("<IsBnkd>").append(req.getIsBanked() != null ? req.getIsBanked() : true).append("</IsBnkd>");
        sb.append("<Ccy>").append(req.getCurrency() != null ? req.getCurrency() : "OMR").append("</Ccy>");
        if (req.getAccountAlias() != null) sb.append("<AcctAlias>").append(esc(req.getAccountAlias())).append("</AcctAlias>");
        if (req.getIsDefaultAccount() != null) sb.append("<IsDfltAcct>").append(req.getIsDefaultAccount()).append("</IsDfltAcct>");
        if (req.getMerchantId() != null) sb.append("<MrchntId>").append(req.getMerchantId()).append("</MrchntId>");
        sb.append("</AcctDtls>");
        if (req.getAdditionalInfo() != null) sb.append("<AddtlInf>").append(esc(req.getAdditionalInfo())).append("</AddtlInf>");
        sb.append("</AcctRecOpngReq>");
    }

    private void buildAccountMaintainXml(StringBuilder sb, String msgId, RegistrationRequest req) {
        sb.append("<AcctRecMntncReq>");
        appendCommonFields(sb, msgId, req);
        sb.append("<AcctDtls>");
        sb.append("<AcctTp>").append(req.getAccountType()).append("</AcctTp>");
        sb.append("<MobNb>").append(req.getMobileNumber()).append("</MobNb>");
        if (req.getRegistrationCode() != null) sb.append("<RegnCd>").append(req.getRegistrationCode()).append("</RegnCd>");
        if (req.getAccountAlias() != null) sb.append("<AcctAlias>").append(esc(req.getAccountAlias())).append("</AcctAlias>");
        if (req.getIsDefaultAccount() != null) sb.append("<IsDfltAcct>").append(req.getIsDefaultAccount()).append("</IsDfltAcct>");
        sb.append("</AcctDtls>");
        sb.append("</AcctRecMntncReq>");
    }

    private void buildAccountCloseXml(StringBuilder sb, String msgId, RegistrationRequest req) {
        sb.append("<AcctRecClsgReq>");
        appendCommonFields(sb, msgId, req);
        sb.append("<AcctDtls>");
        sb.append("<AcctTp>").append(req.getAccountType()).append("</AcctTp>");
        sb.append("<MobNb>").append(req.getMobileNumber()).append("</MobNb>");
        if (req.getRegistrationCode() != null) sb.append("<RegnCd>").append(req.getRegistrationCode()).append("</RegnCd>");
        sb.append("</AcctDtls>");
        sb.append("</AcctRecClsgReq>");
    }

    private void appendCommonFields(StringBuilder sb, String msgId, RegistrationRequest req) {
        sb.append("<MsgId>").append(msgId).append("</MsgId>");
        sb.append("<CstmrId>");
        if (req.getCustomerIdType() != null) sb.append("<IdTp>").append(req.getCustomerIdType()).append("</IdTp>");
        sb.append("<Id>").append(req.getCustomerIdNumber()).append("</Id>");
        if (req.getIdIssuingCountry() != null) sb.append("<IdIssCtry>").append(req.getIdIssuingCountry()).append("</IdIssCtry>");
        sb.append("</CstmrId>");
    }

    private String resolveMessageType(String registrationType) {
        return switch (registrationType.toUpperCase()) {
            case "CUSTOMER_OPEN" -> "cstmrreg.01.01";
            case "CUSTOMER_MAINTAIN" -> "cstmrreg.02.01";
            case "CUSTOMER_CLOSE" -> "cstmrreg.03.01";
            case "ACCOUNT_OPEN" -> "cstmrreg.06.01";
            case "ACCOUNT_MAINTAIN" -> "cstmrreg.07.01";
            case "ACCOUNT_CLOSE" -> "cstmrreg.08.01";
            default -> throw new IllegalArgumentException("Invalid registration type: " + registrationType);
        };
    }

    private String resolveMessageTypeCode(String registrationType) {
        return switch (registrationType.toUpperCase()) {
            case "CUSTOMER_OPEN" -> "CSTMRREG.01";
            case "CUSTOMER_MAINTAIN" -> "CSTMRREG.02";
            case "CUSTOMER_CLOSE" -> "CSTMRREG.03";
            case "ACCOUNT_OPEN" -> "CSTMRREG.06";
            case "ACCOUNT_MAINTAIN" -> "CSTMRREG.07";
            case "ACCOUNT_CLOSE" -> "CSTMRREG.08";
            default -> throw new IllegalArgumentException("Invalid registration type: " + registrationType);
        };
    }

    private RegistrationResponse toResponse(CustomerRegistration reg) {
        return RegistrationResponse.builder()
                .messageId(reg.getMessageId())
                .registrationType(reg.getMessageType())
                .status(reg.getStatus())
                .reasonCode(reg.getResponseReasonCode())
                .additionalInfo(reg.getResponseAdditionalInfo())
                .mobileNumber(reg.getMobileNumber())
                .customerIdNumber(reg.getCustomerIdNumber())
                .createdAt(reg.getCreatedAt())
                .build();
    }

    private String esc(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}

