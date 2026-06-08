package mobi.foo.mpcss.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * REST response for payment operations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {

    private String messageId;
    private String status;
    private String statusDescription;
    private BigDecimal amount;
    private String currency;
    private String debtorName;
    private String debtorAccount;
    private String creditorName;
    private String creditorAccount;
    private String endToEndId;
    private String reasonCode;
    private String additionalInfo;
    private LocalDateTime createdAt;
    private LocalDateTime repliedAt;
}

