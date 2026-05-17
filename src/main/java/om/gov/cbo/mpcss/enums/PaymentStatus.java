package om.gov.cbo.mpcss.enums;

/**
 * Payment Status Report Group Status codes as defined in Section 9.7.
 */
public enum PaymentStatus {

    /** Payment rejected by PS-mpClear or receiving PSP */
    RJCT("Rejected"),

    /** Accepted Settlement In Process - all validations passed, payment accepted for execution */
    ACSP("AcceptedSettlementInProcess"),

    /** Accepted Settlement Completed - settlement on debtor's account completed */
    ACSC("AcceptedSettlementCompleted");

    private final String description;

    PaymentStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public static PaymentStatus fromCode(String code) {
        if (code == null) return null;
        return valueOf(code.trim().toUpperCase());
    }
}

