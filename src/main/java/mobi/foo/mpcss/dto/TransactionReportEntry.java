package mobi.foo.mpcss.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for Transaction Report entries as defined in Section 8.3.2.
 * Each entry represents one transaction from the clearing session.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionReportEntry {

    private String messageId;
    /** CR (Push Payment) / DB (Pull Payment) */
    private String type;
    /** P2P, P2B, P2G */
    private String trxType;
    private BigDecimal amount;
    private String transactionDate;
    private String endToEndId;
    private String payerIdentification;
    private String payerMobileNo;
    private String payerAliasName;
    private String payeeIdentification;
    private String payeeMobileNo;
    private String payeeAliasName;
    private String terminalId;
    private String groupMerchantId;
    private String consumerId;
    private String invoiceNo;
    private BigDecimal networkFee;
    private BigDecimal interchangeFee;
    private String payeeBank;
    private String payerBank;
    private String pointOfInitiationMethod;
    private String merchantCategoryCode;
    private String tipOrConvenienceIndicator;
    private BigDecimal tipSurcharge;
    private String countryCode;
    private String merchantName;
    private String merchantCity;
    /** Transaction final status: ACSC or RJCT */
    private String state;
    /** Rejection reason code if state=RJCT */
    private String reason;
    private String onUs;
    private String additionalInfo;
}

