package om.gov.cbo.mpcss.parser;

import om.gov.cbo.mpcss.dto.CreditTransferMessage;
import om.gov.cbo.mpcss.dto.PaymentStatusReport;
import om.gov.cbo.mpcss.enums.PaymentStatus;
import om.gov.cbo.mpcss.model.MpcssMessageWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class Iso20022XmlParserTest {

    private Iso20022XmlParser parser;

    @BeforeEach
    void setUp() {
        parser = new Iso20022XmlParser();
    }

    @Test
    void shouldParseMessageWrapper() {
        String xml = """
                <MpcssMessage>
                    <type>PACS.008</type>
                    <format>MX</format>
                    <date>2025-01-31T10:30:00</date>
                    <signature>abc123==</signature>
                    <content>&lt;Document/&gt;</content>
                </MpcssMessage>""";

        MpcssMessageWrapper wrapper = parser.parseMessageWrapper(xml);

        assertEquals("PACS.008", wrapper.getType());
        assertEquals("MX", wrapper.getFormat());
        assertEquals("2025-01-31T10:30:00", wrapper.getDate());
        assertEquals("abc123==", wrapper.getSignature());
        assertNotNull(wrapper.getContent());
    }

    @Test
    void shouldParseCreditTransfer() {
        String xml = """
                <Document xmlns="urn:iso:std:iso:20022:tech:xsd:pacs.008.001.05">
                  <FIToFICstmrCdtTrf>
                    <GrpHdr>
                      <MsgId>BNK1000000000001</MsgId>
                      <CreDtTm>2025-03-06T17:35:18</CreDtTm>
                      <NbOfTxs>1</NbOfTxs>
                      <TtlIntrBkSttlmAmt Ccy="OMR">100.000</TtlIntrBkSttlmAmt>
                      <IntrBkSttlmDt>2025-03-06</IntrBkSttlmDt>
                      <SttlmInf>
                        <SttlmMtd>CLRG</SttlmMtd>
                        <ClrSys><Prtry>CBO</Prtry></ClrSys>
                      </SttlmInf>
                      <PmtTpInf>
                        <LclInstrm><Cd>TEL</Cd></LclInstrm>
                        <CtgyPurp><Prtry>MP</Prtry></CtgyPurp>
                      </PmtTpInf>
                    </GrpHdr>
                    <CdtTrfTxInf>
                      <PmtId>
                        <EndToEndId>E2E001</EndToEndId>
                        <TxId>TX001</TxId>
                      </PmtId>
                      <IntrBkSttlmAmt Ccy="OMR">100.000</IntrBkSttlmAmt>
                      <Dbtr><Nm>Ahmed Al-Balushi</Nm></Dbtr>
                      <DbtrAcct><Id><IBAN>OM12BANK0000001234567890</IBAN></Id></DbtrAcct>
                      <DbtrAgt><FinInstnId><BICFI>BANKOMRUXXX</BICFI></FinInstnId></DbtrAgt>
                      <Cdtr><Nm>Fatima Al-Habsi</Nm></Cdtr>
                      <CdtrAcct><Id><IBAN>OM34NBOK0000009876543210</IBAN></Id></CdtrAcct>
                      <CdtrAgt><FinInstnId><BICFI>NBOKOMRUXXX</BICFI></FinInstnId></CdtrAgt>
                      <RmtInf><Ustrd>Payment for services</Ustrd></RmtInf>
                    </CdtTrfTxInf>
                  </FIToFICstmrCdtTrf>
                </Document>""";

        CreditTransferMessage msg = parser.parseCreditTransfer(xml);

        assertEquals("BNK1000000000001", msg.getMessageId());
        assertEquals(1, msg.getNumberOfTransactions());
        assertEquals("OMR", msg.getSettlementCurrency());
        assertEquals("CLRG", msg.getSettlementMethod());
        assertEquals("CBO", msg.getClearingSystem());
        assertEquals("TEL", msg.getLocalInstrumentCode());
        assertEquals("MP", msg.getCategoryPurpose());
        assertEquals("Ahmed Al-Balushi", msg.getDebtorName());
        assertEquals("OM12BANK0000001234567890", msg.getDebtorAccountId());
        assertEquals("BANKOMRUXXX", msg.getDebtorAgentBic());
        assertEquals("Fatima Al-Habsi", msg.getCreditorName());
        assertEquals("OM34NBOK0000009876543210", msg.getCreditorAccountId());
        assertEquals("NBOKOMRUXXX", msg.getCreditorAgentBic());
        assertEquals("E2E001", msg.getEndToEndId());
        assertEquals("TX001", msg.getTransactionId());
        assertEquals("Payment for services", msg.getRemittanceInfo());
    }

    @Test
    void shouldParsePaymentStatusReport() {
        String xml = """
                <Document xmlns="urn:iso:std:iso:20022:tech:xsd:pacs.002.001.06">
                  <FIToFIPmtStsRpt>
                    <GrpHdr>
                      <MsgId>BNK2000000000002</MsgId>
                      <CreDtTm>2025-03-06T17:36:00</CreDtTm>
                    </GrpHdr>
                    <OrgnlGrpInfAndSts>
                      <OrgnlMsgId>BNK1000000000001</OrgnlMsgId>
                      <OrgnlMsgNmId>pacs.008.001.05</OrgnlMsgNmId>
                      <GrpSts>RJCT</GrpSts>
                    </OrgnlGrpInfAndSts>
                    <TxInfAndSts>
                      <OrgnlEndToEndId>E2E001</OrgnlEndToEndId>
                      <StsRsnInf>
                        <Rsn><Prtry>5</Prtry></Rsn>
                        <AddtlInf>Insufficient funds</AddtlInf>
                      </StsRsnInf>
                    </TxInfAndSts>
                  </FIToFIPmtStsRpt>
                </Document>""";

        PaymentStatusReport report = parser.parsePaymentStatusReport(xml);

        assertEquals("BNK2000000000002", report.getMessageId());
        assertEquals("BNK1000000000001", report.getOriginalMessageId());
        assertEquals("pacs.008.001.05", report.getOriginalMessageNameId());
        assertEquals(PaymentStatus.RJCT, report.getGroupStatus());
        assertEquals("5", report.getReasonCode());
        assertEquals("Insufficient funds", report.getAdditionalInfo());
        assertEquals("E2E001", report.getOriginalEndToEndId());
    }
}

