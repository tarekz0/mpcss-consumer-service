package mobi.foo.mpcss.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO representing a parsed pacs.008.001.05 Credit Transfer message.
 * Maps the core fields from Section 5.1 Credit Message Specifications.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreditTransferMessage {

    // ─── Group Header (GrpHdr) ──────────────────────────────────────────
    /** MsgId: {ParticipantNumericCode}{MessageReferenceID} - 16 chars */
    private String messageId;

    /** CreDtTm: Message creation date time */
    private LocalDateTime creationDateTime;

    /** NbOfTxs: Always fixed to 1 */
    private int numberOfTransactions;

    /** TtlIntrBkSttlmAmt: Total interbank settlement amount with currency */
    private BigDecimal totalInterbankSettlementAmount;

    /** Currency attribute of TtlIntrBkSttlmAmt (e.g. "OMR") */
    private String settlementCurrency;

    /** IntrBkSttlmDt: Interbank settlement date */
    private LocalDate interbankSettlementDate;

    /** SttlmMtd: Always fixed to "CLRG" */
    private String settlementMethod;

    /** ClrSys/Prtry: Always fixed to "CBO" */
    private String clearingSystem;

    // ─── Payment Type Information ───────────────────────────────────────
    /** LclInstrm/Cd: Channel - always "TEL" */
    private String localInstrumentCode;

    /** CtgyPurp/Prtry: Mobile payment category purpose */
    private String categoryPurpose;

    // ─── Instructing/Instructed Agents ───────────────────────────────────
    /** InstgAgt: Instructing agent BIC */
    private String instructingAgentBic;

    /** InstdAgt: Instructed agent BIC */
    private String instructedAgentBic;

    // ─── Debtor (Payer) Information ─────────────────────────────────────
    /** Dbtr/Nm: Debtor name */
    private String debtorName;

    /** DbtrAcct/Id: Debtor account identification */
    private String debtorAccountId;

    /** DbtrAgt: Debtor agent BIC */
    private String debtorAgentBic;

    // ─── Creditor (Beneficiary) Information ─────────────────────────────
    /** Cdtr/Nm: Creditor name */
    private String creditorName;

    /** CdtrAcct/Id: Creditor account identification */
    private String creditorAccountId;

    /** CdtrAgt: Creditor agent BIC */
    private String creditorAgentBic;

    // ─── Transaction Details ────────────────────────────────────────────
    /** EndToEndId: End-to-end identification */
    private String endToEndId;

    /** TxId: Transaction identification */
    private String transactionId;

    /** InstrId: Instruction identification */
    private String instructionId;

    /** IntrBkSttlmAmt: Transaction amount */
    private BigDecimal transactionAmount;

    /** RmtInf: Remittance information */
    private String remittanceInfo;

    /** Indicates if this is an OnUs transaction */
    private boolean onUs;

    /** PEP encrypted data (MPCSSv2 - PIN/OTP) */
    private String pepEncryptedData;
}

