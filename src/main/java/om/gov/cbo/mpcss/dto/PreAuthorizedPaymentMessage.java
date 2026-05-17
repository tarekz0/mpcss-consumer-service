package om.gov.cbo.mpcss.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for MPCSSv2 pre-authorized payment messages (camt.056/029).
 * As defined in Section 6.3.4.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PreAuthorizedPaymentMessage {

    private String messageId;
    private String preAuthId;

    // Payer info
    private String payerName;
    private String payerAccountId;
    private String payerPspBic;
    private String payerAhiBic;

    // Beneficiary info
    private String beneficiaryName;
    private String beneficiaryAccountId;
    private String beneficiaryPspBic;

    // Payment details
    private BigDecimal authorizedAmount;
    private BigDecimal completionAmount;
    private String currency;
    private String purpose;
    private String expiryDate;

    // Status
    private String status;
    private String reasonCode;
    private String additionalInfo;
}

