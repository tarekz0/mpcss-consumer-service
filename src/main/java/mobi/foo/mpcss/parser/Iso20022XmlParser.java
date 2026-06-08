package mobi.foo.mpcss.parser;

import mobi.foo.mpcss.dto.CreditTransferMessage;
import mobi.foo.mpcss.dto.PaymentStatusReport;
import mobi.foo.mpcss.dto.RegistrationMessage;
import mobi.foo.mpcss.enums.PaymentStatus;
import mobi.foo.mpcss.model.MpcssMessageWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Parser for ISO 20022 XML messages used in PS-mpClear.
 * Handles parsing of pacs.008, pacs.002, cstmrreg.* and other message formats
 * as defined in the MPCSS Interface Specifications Document.
 */
@Slf4j
@Component
public class Iso20022XmlParser {

    private static final DateTimeFormatter ISO_DATETIME = DateTimeFormatter.ISO_DATE_TIME;
    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_DATE;

    /**
     * Parse the MPCSS message wrapper XML envelope (Section 10.2).
     * Structure: {@code <MpcssMessage><type/><format/><date/><signature/><content/></MpcssMessage>}
     */
    public MpcssMessageWrapper parseMessageWrapper(String rawXml) {
        try {
            Document doc = parseXmlDocument(rawXml);
            Element root = doc.getDocumentElement();

            return MpcssMessageWrapper.builder()
                    .type(getElementText(root, "type"))
                    .format(getElementText(root, "format"))
                    .date(getElementText(root, "date"))
                    .signature(getElementText(root, "signature"))
                    .content(getElementText(root, "content"))
                    .build();
        } catch (Exception e) {
            log.error("Failed to parse MPCSS message wrapper: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to parse MPCSS message wrapper", e);
        }
    }

    /**
     * Parse pacs.008.001.05 - FIToFICustomerCreditTransfer (Section 5.1).
     */
    public CreditTransferMessage parseCreditTransfer(String xmlContent) {
        try {
            Document doc = parseXmlDocument(xmlContent);
            Element root = doc.getDocumentElement();

            CreditTransferMessage.CreditTransferMessageBuilder builder = CreditTransferMessage.builder();

            // Group Header
            Element grpHdr = getFirstElement(root, "GrpHdr");
            if (grpHdr != null) {
                builder.messageId(getElementText(grpHdr, "MsgId"))
                        .creationDateTime(parseDateTime(getElementText(grpHdr, "CreDtTm")))
                        .numberOfTransactions(parseIntSafe(getElementText(grpHdr, "NbOfTxs")));

                Element ttlAmt = getFirstElement(grpHdr, "TtlIntrBkSttlmAmt");
                if (ttlAmt != null) {
                    builder.totalInterbankSettlementAmount(new BigDecimal(ttlAmt.getTextContent().trim()))
                            .settlementCurrency(ttlAmt.getAttribute("Ccy"));
                }

                builder.interbankSettlementDate(parseDate(getElementText(grpHdr, "IntrBkSttlmDt")));

                // Settlement Info
                Element sttlmInf = getFirstElement(grpHdr, "SttlmInf");
                if (sttlmInf != null) {
                    builder.settlementMethod(getElementText(sttlmInf, "SttlmMtd"));
                    Element clrSys = getFirstElement(sttlmInf, "ClrSys");
                    if (clrSys != null) {
                        builder.clearingSystem(getElementText(clrSys, "Prtry"));
                    }
                }

                // Payment Type Info
                Element pmtTpInf = getFirstElement(grpHdr, "PmtTpInf");
                if (pmtTpInf != null) {
                    Element lclInstrm = getFirstElement(pmtTpInf, "LclInstrm");
                    if (lclInstrm != null) {
                        builder.localInstrumentCode(getElementText(lclInstrm, "Cd"));
                    }
                    Element ctgyPurp = getFirstElement(pmtTpInf, "CtgyPurp");
                    if (ctgyPurp != null) {
                        builder.categoryPurpose(getElementText(ctgyPurp, "Prtry"));
                    }
                }
            }

            // Credit Transfer Transaction Information
            Element cdtTrfTxInf = getFirstElement(root, "CdtTrfTxInf");
            if (cdtTrfTxInf != null) {
                // Payment ID
                Element pmtId = getFirstElement(cdtTrfTxInf, "PmtId");
                if (pmtId != null) {
                    builder.endToEndId(getElementText(pmtId, "EndToEndId"))
                            .transactionId(getElementText(pmtId, "TxId"))
                            .instructionId(getElementText(pmtId, "InstrId"));
                }

                // Transaction Amount
                Element intrBkSttlmAmt = getFirstElement(cdtTrfTxInf, "IntrBkSttlmAmt");
                if (intrBkSttlmAmt != null) {
                    builder.transactionAmount(new BigDecimal(intrBkSttlmAmt.getTextContent().trim()));
                }

                // Instructing Agent
                Element instgAgt = getFirstElement(cdtTrfTxInf, "InstgAgt");
                if (instgAgt != null) {
                    builder.instructingAgentBic(extractBic(instgAgt));
                }

                // Instructed Agent
                Element instdAgt = getFirstElement(cdtTrfTxInf, "InstdAgt");
                if (instdAgt != null) {
                    builder.instructedAgentBic(extractBic(instdAgt));
                }

                // Debtor
                Element dbtr = getFirstElement(cdtTrfTxInf, "Dbtr");
                if (dbtr != null) {
                    builder.debtorName(getElementText(dbtr, "Nm"));
                }
                Element dbtrAcct = getFirstElement(cdtTrfTxInf, "DbtrAcct");
                if (dbtrAcct != null) {
                    builder.debtorAccountId(extractAccountId(dbtrAcct));
                }
                Element dbtrAgt = getFirstElement(cdtTrfTxInf, "DbtrAgt");
                if (dbtrAgt != null) {
                    builder.debtorAgentBic(extractBic(dbtrAgt));
                }

                // Creditor
                Element cdtr = getFirstElement(cdtTrfTxInf, "Cdtr");
                if (cdtr != null) {
                    builder.creditorName(getElementText(cdtr, "Nm"));
                }
                Element cdtrAcct = getFirstElement(cdtTrfTxInf, "CdtrAcct");
                if (cdtrAcct != null) {
                    builder.creditorAccountId(extractAccountId(cdtrAcct));
                }
                Element cdtrAgt = getFirstElement(cdtTrfTxInf, "CdtrAgt");
                if (cdtrAgt != null) {
                    builder.creditorAgentBic(extractBic(cdtrAgt));
                }

                // Remittance Information
                Element rmtInf = getFirstElement(cdtTrfTxInf, "RmtInf");
                if (rmtInf != null) {
                    builder.remittanceInfo(getElementText(rmtInf, "Ustrd"));
                }
            }

            return builder.build();
        } catch (Exception e) {
            log.error("Failed to parse pacs.008 credit transfer: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to parse credit transfer message", e);
        }
    }

    /**
     * Parse pacs.002.001.06 - Payment Status Report (Section 5.2).
     */
    public PaymentStatusReport parsePaymentStatusReport(String xmlContent) {
        try {
            Document doc = parseXmlDocument(xmlContent);
            Element root = doc.getDocumentElement();

            PaymentStatusReport.PaymentStatusReportBuilder builder = PaymentStatusReport.builder();

            // Group Header
            Element grpHdr = getFirstElement(root, "GrpHdr");
            if (grpHdr != null) {
                builder.messageId(getElementText(grpHdr, "MsgId"))
                        .creationDateTime(parseDateTime(getElementText(grpHdr, "CreDtTm")));
            }

            // Original Group Information
            Element orgnlGrpInf = getFirstElement(root, "OrgnlGrpInfAndSts");
            if (orgnlGrpInf != null) {
                builder.originalMessageId(getElementText(orgnlGrpInf, "OrgnlMsgId"))
                        .originalMessageNameId(getElementText(orgnlGrpInf, "OrgnlMsgNmId"));

                String grpSts = getElementText(orgnlGrpInf, "GrpSts");
                if (grpSts != null) {
                    builder.groupStatus(PaymentStatus.fromCode(grpSts));
                }
            }

            // Transaction Information and Status
            Element txInfAndSts = getFirstElement(root, "TxInfAndSts");
            if (txInfAndSts != null) {
                builder.originalEndToEndId(getElementText(txInfAndSts, "OrgnlEndToEndId"))
                        .originalTransactionId(getElementText(txInfAndSts, "OrgnlTxId"));

                Element stsRsnInf = getFirstElement(txInfAndSts, "StsRsnInf");
                if (stsRsnInf != null) {
                    Element rsn = getFirstElement(stsRsnInf, "Rsn");
                    if (rsn != null) {
                        builder.reasonCode(getElementText(rsn, "Prtry"));
                    }
                    builder.additionalInfo(getElementText(stsRsnInf, "AddtlInf"));
                }
            }

            return builder.build();
        } catch (Exception e) {
            log.error("Failed to parse pacs.002 status report: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to parse payment status report", e);
        }
    }

    /**
     * Parse registration messages (cstmrreg.*).
     */
    public RegistrationMessage parseRegistrationMessage(String xmlContent, String messageType) {
        try {
            Document doc = parseXmlDocument(xmlContent);
            Element root = doc.getDocumentElement();

            RegistrationMessage.RegistrationMessageBuilder builder = RegistrationMessage.builder()
                    .messageType(messageType);

            // Common fields
            builder.messageId(getElementText(root, "MsgId"));

            // Customer identification
            Element custId = getFirstElement(root, "CstmrId");
            if (custId != null) {
                builder.customerIdType(getElementText(custId, "IdTp"))
                        .customerIdNumber(getElementText(custId, "Id"))
                        .idIssuingCountry(getElementText(custId, "IdIssCtry"));
            }

            // Customer details
            builder.firstName(getElementText(root, "FrstNm"))
                    .lastName(getElementText(root, "LstNm"))
                    .nickname(getElementText(root, "NckNm"))
                    .mobileNumber(getElementText(root, "MobNb"));

            // Account details
            Element acctDtls = getFirstElement(root, "AcctDtls");
            if (acctDtls == null) acctDtls = root;

            builder.accountType(getElementText(acctDtls, "AcctTp"))
                    .registrationCode(getElementText(acctDtls, "RegnCd"))
                    .currency(getElementText(acctDtls, "Ccy"))
                    .accountAlias(getElementText(acctDtls, "AcctAlias"))
                    .merchantId(getElementText(acctDtls, "MrchntId"));

            String isBanked = getElementText(acctDtls, "IsBnkd");
            if (isBanked != null) {
                builder.isBanked(Boolean.parseBoolean(isBanked));
            }

            String isDflt = getElementText(acctDtls, "IsDfltAcct");
            if (isDflt != null) {
                builder.isDefaultAccount(Boolean.parseBoolean(isDflt));
            }

            builder.additionalInfo(getElementText(root, "AddtlInf"));

            // Response fields (cstmrreg.10.01)
            Element orgnlMsgSts = getFirstElement(root, "OrgnlMsgSts");
            if (orgnlMsgSts != null) {
                builder.status(getElementText(orgnlMsgSts, "Sts"))
                        .reasonCode(getElementText(orgnlMsgSts, "RsnCd"))
                        .originalMessageId(getElementText(root, "OrgnlMsgId"));
            }

            return builder.build();
        } catch (Exception e) {
            log.error("Failed to parse registration message [{}]: {}", messageType, e.getMessage(), e);
            throw new RuntimeException("Failed to parse registration message", e);
        }
    }

    // ─── Helper Methods ─────────────────────────────────────────────────

    private Document parseXmlDocument(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        // Disable external entities for security
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(new InputSource(new StringReader(xml)));
    }

    private Element getFirstElement(Element parent, String tagName) {
        if (parent == null) return null;
        NodeList nodes = parent.getElementsByTagNameNS("*", tagName);
        if (nodes.getLength() == 0) {
            nodes = parent.getElementsByTagName(tagName);
        }
        return nodes.getLength() > 0 ? (Element) nodes.item(0) : null;
    }

    private String getElementText(Element parent, String tagName) {
        Element element = getFirstElement(parent, tagName);
        return element != null ? element.getTextContent().trim() : null;
    }

    private String extractBic(Element agentElement) {
        Element finInstnId = getFirstElement(agentElement, "FinInstnId");
        if (finInstnId != null) {
            String bic = getElementText(finInstnId, "BICFI");
            if (bic == null) bic = getElementText(finInstnId, "BIC");
            return bic;
        }
        return null;
    }

    private String extractAccountId(Element acctElement) {
        Element id = getFirstElement(acctElement, "Id");
        if (id != null) {
            String iban = getElementText(id, "IBAN");
            if (iban != null) return iban;
            Element othr = getFirstElement(id, "Othr");
            if (othr != null) return getElementText(othr, "Id");
        }
        return null;
    }

    private LocalDateTime parseDateTime(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return LocalDateTime.parse(value, ISO_DATETIME);
        } catch (Exception e) {
            log.warn("Unable to parse datetime: {}", value);
            return null;
        }
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return LocalDate.parse(value, ISO_DATE);
        } catch (Exception e) {
            log.warn("Unable to parse date: {}", value);
            return null;
        }
    }

    private int parseIntSafe(String value) {
        if (value == null || value.isBlank()) return 0;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}

