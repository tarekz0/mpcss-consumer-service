package mobi.foo.mpcss.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * REST request to initiate a Direct Credit payment via MPCSS (pacs.008).
 * Called by mobile app or internal service.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequest {

    /** Debtor (payer) account IBAN or mobile number */
    @NotBlank(message = "Debtor account is required")
    private String debtorAccount;

    /** Debtor name */
    @NotBlank(message = "Debtor name is required")
    private String debtorName;

    /** Creditor (beneficiary) - mobile number, alias name, or IBAN */
    @NotBlank(message = "Creditor identifier is required")
    private String creditorIdentifier;

    /** How to identify creditor: MOBILE, ALIAS, IBAN */
    @NotBlank(message = "Creditor identifier type is required")
    private String creditorIdentifierType;

    /** Creditor name (optional - will be resolved via name verification) */
    private String creditorName;

    /** Payment amount */
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.001", message = "Amount must be positive")
    private BigDecimal amount;

    /** Currency (default OMR) */
    @Size(min = 3, max = 3)
    private String currency;

    /** Category purpose (e.g. MP for mobile payment) */
    private String categoryPurpose;

    /** Remittance/payment description */
    private String description;

    /** PEP encrypted MPIN/OTP for MPCSSv2 */
    private String pepEncryptedData;

    /** End-to-end ID (merchant reference for QR payments) */
    private String endToEndId;
}

