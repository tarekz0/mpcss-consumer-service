package om.gov.cbo.mpcss.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for NCP (Net Clearing Position) Report as defined in Section 10.3 / 8.3.1.
 * Received as binary message at end of each clearing session.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NcpReport {

    private BigDecimal totalDebit;
    private BigDecimal totalCredit;
    private BigDecimal netPosition;
    private String settlementDate;
    private int sessionSequence;
    private String currency;
    private String participant;
    private int settlementRetry;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParticipantDetail {
        private String participantBic;
        private BigDecimal totalCredit;
        private BigDecimal totalDebit;
        private BigDecimal totalNetworkFeeCredit;
        private BigDecimal totalNetworkFeeDebit;
        private BigDecimal totalSubscriptionFeeCredit;
        private BigDecimal totalSubscriptionFeeDebit;
        private BigDecimal totalAdhocFeeCredit;
        private BigDecimal totalAdhocFeeDebit;
        private BigDecimal totalInterchangeFeeCredit;
        private BigDecimal totalInterchangeFeeDebit;
        private BigDecimal netAmount;
    }
}

