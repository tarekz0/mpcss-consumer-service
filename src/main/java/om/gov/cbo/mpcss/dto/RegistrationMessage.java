package om.gov.cbo.mpcss.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for Registration messages (cstmrreg.*).
 * Covers customer/account registration requests and responses
 * as defined in Section 3.1 and Section 4.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegistrationMessage {

    /** Message identification */
    private String messageId;

    /** Message type (e.g. cstmrreg.01.01) */
    private String messageType;

    /** Customer identification type code */
    private String customerIdType;

    /** Customer identification number */
    private String customerIdNumber;

    /** ID issuing country code */
    private String idIssuingCountry;

    /** Customer first name */
    private String firstName;

    /** Customer last name */
    private String lastName;

    /** Customer nickname/alias */
    private String nickname;

    /** Mobile number */
    private String mobileNumber;

    /** Account type */
    private String accountType;

    /** Account registration code */
    private String registrationCode;

    /** Is account banked (true/false) */
    private Boolean isBanked;

    /** Account currency */
    private String currency;

    /** Account alias */
    private String accountAlias;

    /** Is default account */
    private Boolean isDefaultAccount;

    /** Merchant ID (for institutional accounts) */
    private String merchantId;

    /** Additional information */
    private String additionalInfo;

    // ─── Response fields (cstmrreg.10.01) ───────────────────────────────
    /** Status: ACPT (accepted) or RJCT (rejected) */
    private String status;

    /** Reason code for the response */
    private String reasonCode;

    /** Original message ID in response */
    private String originalMessageId;
}

