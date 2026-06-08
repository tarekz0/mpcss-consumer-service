package mobi.foo.mpcss.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * REST response for registration operations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegistrationResponse {

    private String messageId;
    private String registrationType;
    private String status;
    private String reasonCode;
    private String additionalInfo;
    private String mobileNumber;
    private String customerIdNumber;
    private LocalDateTime createdAt;
}

