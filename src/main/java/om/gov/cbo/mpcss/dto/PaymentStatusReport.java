package om.gov.cbo.mpcss.dto;

import om.gov.cbo.mpcss.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO representing a parsed pacs.002.001.06 Payment Status Report message.
 * Maps the core fields from Section 5.2 Payment Status Report Specifications.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentStatusReport {

    /** MsgId: Message identification */
    private String messageId;

    /** CreDtTm: Creation date time */
    private LocalDateTime creationDateTime;

    /** OrgnlMsgId: Original message identification */
    private String originalMessageId;

    /** OrgnlMsgNmId: Original message name identification (e.g. pacs.008.001.05) */
    private String originalMessageNameId;

    /** GrpSts: Group status - RJCT, ACSP, ACSC */
    private PaymentStatus groupStatus;

    /** StsRsnInf/Rsn/Prtry: Rejection reason code */
    private String reasonCode;

    /** AddtlInf: Additional information about the status */
    private String additionalInfo;

    /** OrgnlEndToEndId: Original end-to-end identification */
    private String originalEndToEndId;

    /** OrgnlTxId: Original transaction identification */
    private String originalTransactionId;

    /** Accepted/Rejected status as clear text */
    private String statusDescription;
}

