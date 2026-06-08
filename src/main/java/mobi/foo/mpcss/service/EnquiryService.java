package mobi.foo.mpcss.service;

import mobi.foo.mpcss.client.CoreBankingClient;
import mobi.foo.mpcss.config.MpcssProperties;
import mobi.foo.mpcss.producer.MpcssResponseProducer;
import mobi.foo.mpcss.util.MessageIdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service for enquiry operations:
 * - Name Verification (cstmrreg.20/21)
 * - Check Default Account (cstmrreg.25/26)
 * - Payment Enquiry
 * - Heartbeat
 * - Get Participants List (cstmrreg.35/36) — MPCSSv2
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EnquiryService {

    private final MpcssResponseProducer responseProducer;
    private final CoreBankingClient coreBankingClient;
    private final MessageIdGenerator messageIdGenerator;
    private final MpcssProperties properties;

    /**
     * Send customer name verification request (cstmrreg.20.01).
     * Used before payment (Section 6.3.1 step 4) to verify beneficiary.
     *
     * @param identifier  Mobile number or alias
     * @param identifierType  MOBILE or ALIAS
     * @return messageId for tracking
     */
    public String sendNameVerification(String identifier, String identifierType) {
        String messageId = messageIdGenerator.generate();

        String xml = """
                <Document>
                  <CstmrNmVrfctnReq>
                    <MsgId>%s</MsgId>
                    <CstmrId>
                      <%s>%s</%s>
                    </CstmrId>
                  </CstmrNmVrfctnReq>
                </Document>""".formatted(
                messageId,
                "MOBILE".equals(identifierType) ? "MobNb" : "AcctAlias",
                identifier,
                "MOBILE".equals(identifierType) ? "MobNb" : "AcctAlias"
        );

        responseProducer.sendNameVerificationRequest(xml);
        log.info("Sent name verification - MsgId: {}, Identifier: {}", messageId, identifier);
        return messageId;
    }

    /**
     * Handle name verification response (cstmrreg.21.01).
     * Forward the resolved name to core banking.
     */
    public void handleNameVerificationResponse(String originalMsgId, String customerName,
                                                String mobileNumber, String rawXml) {
        log.info("Name verification response - OrigMsgId: {}, Name: {}", originalMsgId, customerName);
        coreBankingClient.forwardNameVerificationResult(mobileNumber, customerName, originalMsgId);
    }

    /**
     * Send check default account request (cstmrreg.25.01).
     */
    public String sendDefaultAccountCheck(String mobileNumber) {
        String messageId = messageIdGenerator.generate();

        String xml = """
                <Document>
                  <IsDefAcctReq>
                    <MsgId>%s</MsgId>
                    <MobNb>%s</MobNb>
                  </IsDefAcctReq>
                </Document>""".formatted(messageId, mobileNumber);

        responseProducer.sendDefaultAccountCheckRequest(xml);
        log.info("Sent default account check - MsgId: {}, Mobile: {}", messageId, mobileNumber);
        return messageId;
    }

    /**
     * Handle default account check response (cstmrreg.26.01).
     */
    public void handleDefaultAccountResponse(String rawXml) {
        log.info("Default account response received");
        // Parse response and forward to core banking
    }

    /**
     * Send payment enquiry request (Section 5.3).
     */
    public String sendPaymentEnquiry(String originalMessageId) {
        String messageId = messageIdGenerator.generate();

        String xml = """
                <Document>
                  <PmtEnqryReq>
                    <MsgId>%s</MsgId>
                    <OrgnlMsgId>%s</OrgnlMsgId>
                  </PmtEnqryReq>
                </Document>""".formatted(messageId, originalMessageId);

        responseProducer.sendPaymentEnquiryRequest(xml);
        log.info("Sent payment enquiry - MsgId: {}, OrigMsgId: {}", messageId, originalMessageId);
        return messageId;
    }

    /**
     * Send heartbeat request to MPCSS.
     */
    public void sendHeartbeat() {
        responseProducer.sendHeartbeatRequest();
        log.debug("Heartbeat sent");
    }

    /**
     * Send get participants list request (cstmrreg.35.01) — MPCSSv2.
     * Section 6.2.5: Pull list of AHIs with their PKI public keys.
     */
    public String sendGetParticipantsList() {
        String messageId = messageIdGenerator.generate();

        String xml = """
                <Document>
                  <GetPrtcptsLstReq>
                    <MsgId>%s</MsgId>
                  </GetPrtcptsLstReq>
                </Document>""".formatted(messageId);

        responseProducer.sendRegistrationMessage("CSTMRREG.35", xml);
        log.info("Sent get participants list - MsgId: {}", messageId);
        return messageId;
    }

    /**
     * Handle participants list response (cstmrreg.36.01).
     * Store participant list with PKI public keys.
     */
    public void handleParticipantsListResponse(String rawXml) {
        log.info("Participants list response received");
        // Parse and store participant info (name, code, route code, PKI public key)
    }
}

