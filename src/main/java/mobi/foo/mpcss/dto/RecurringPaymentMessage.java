package mobi.foo.mpcss.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for MPCSSv2 recurring payment messages (pain.009/011/012).
 * As defined in Section 6.3.3 and Section 8.3.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecurringPaymentMessage {

    private String messageId;
    private String recurringPaymentId;
    private LocalDateTime creationDateTime;

    // Payer info
    private String payerAccountAlias;
    private String payerName;
    private String payerPspBic;
    private String payerAhiBic;

    // Beneficiary info
    private String beneficiaryName;
    private String beneficiaryAccountId;
    private String beneficiaryPspBic;
    private String beneficiaryAhiBic;

    // Payment terms
    private BigDecimal amount;
    private String currency;
    private String frequency;
    private String startDate;
    private String endDate;
    private String purpose;

    // Status
    private String status;
    private String reasonCode;
    private String additionalInfo;

    // Signed authorization (from AHI)
    private String signedAuthorization;
}

