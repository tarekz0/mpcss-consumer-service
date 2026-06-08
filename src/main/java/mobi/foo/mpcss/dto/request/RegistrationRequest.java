package mobi.foo.mpcss.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * REST request to register a customer/account in MPCSS national directory.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegistrationRequest {

    /** Type: CUSTOMER_OPEN, CUSTOMER_MAINTAIN, CUSTOMER_CLOSE,
     *        ACCOUNT_OPEN, ACCOUNT_MAINTAIN, ACCOUNT_CLOSE */
    @NotBlank(message = "Registration type is required")
    private String registrationType;

    // Customer identification
    private String customerIdType;

    @NotBlank(message = "Customer ID number is required")
    private String customerIdNumber;

    private String idIssuingCountry;

    private String firstName;
    private String lastName;
    private String nickname;

    @NotBlank(message = "Mobile number is required")
    private String mobileNumber;

    // Account details (for account operations)
    private String accountType;
    private String registrationCode;
    private Boolean isBanked;
    private String currency;
    private String accountAlias;
    private Boolean isDefaultAccount;
    private String merchantId;
    private String additionalInfo;
}

