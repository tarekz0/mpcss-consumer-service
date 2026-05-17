package om.gov.cbo.mpcss.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for PEP (Pin Encryption Page) data structure.
 * As defined in Section 13 (Appendix: PEP Guideline).
 *
 * <p>Sensitive fields (PartialCardNo, CardExpiryDate, SoftKeyToken, MPIN/OTP)
 * are encrypted with the AHI's public key and can only be decrypted by the AHI.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PepData {

    /** Date and time of encrypted data submission */
    private String dateTime;

    /** Original message ID */
    private String originalMessageId;

    /** Registration, Payment, Pre-auth, Recurring, Balance, Statement */
    private String requestType;

    /** Partial card number (encrypted) - for card-based auth */
    private String partialCardNo;

    /** Card expiry date (encrypted) */
    private String cardExpiryDate;

    /** PKI or soft key token (encrypted) */
    private String softKeyToken;

    /** MPIN or OTP (encrypted) - for transaction authorization */
    private String mpinOrOtp;

    /** Debit account number (displayed in PEP) */
    private String debitAccount;

    /** Beneficiary or merchant name */
    private String paymentTo;

    /** Transaction amount */
    private String transactionAmount;

    /** Account balance */
    private String accountBalance;

    /** Reserved for future use (encrypted) */
    private String filler1;

    /** Reserved for future use */
    private String filler2;
}

