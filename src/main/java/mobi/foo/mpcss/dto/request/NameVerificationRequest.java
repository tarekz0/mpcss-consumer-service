package mobi.foo.mpcss.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * REST request for name verification (cstmrreg.20.01).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NameVerificationRequest {

    /** Mobile number or alias to verify */
    @NotBlank(message = "Identifier is required")
    private String identifier;

    /** MOBILE or ALIAS */
    @NotBlank(message = "Identifier type is required")
    private String identifierType;
}

