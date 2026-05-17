package om.gov.cbo.mpcss.enums;

/**
 * Payment rejection reason codes as defined in Section 11 (Appendix: Message Rejection Codes).
 * User reasons: 1-999, System reasons: 1000+
 */
public enum PaymentRejectReasonCode {

    // ─── User Reasons (PSP/Participant) ─────────────────────────────────
    INVALID_ACCOUNT(1, "Invalid account"),
    ACCOUNT_CLOSED_BLOCKED(2, "Account is closed/blocked"),
    DECEASED_ACCOUNT_HOLDER(3, "Deceased account holder"),
    DORMANT_ACCOUNT(4, "Dormant account"),
    INSUFFICIENT_FUNDS(5, "Insufficient funds"),
    DUPLICATE_TRANSACTION(6, "Duplicate transaction"),

    // ─── System Reasons (PS-mpClear) ────────────────────────────────────
    PROCESSED_SUCCESSFULLY(1000, "Processed successfully"),
    TECHNICAL_ERROR(1001, "Technical error"),
    PARSING_ERROR(1002, "Parsing error"),
    DIGITAL_SIGNATURE_ERROR(1003, "Digital signature/Security error"),
    INVALID_ID_FORMAT(1004, "Invalid ID format"),
    SENDER_NOT_ALLOWED(1005, "Sender is not allowed to send message type"),
    RECEIVER_NOT_ALLOWED(1006, "Receiver is not allowed to receive message type"),
    PURPOSE_NOT_ALLOWED(1007, "Purpose is not allowed for sending"),
    INVALID_REASON(1008, "Invalid reason"),
    NO_SESSION_AVAILABLE(1009, "No session available"),
    AUTO_REPLIED(1010, "Auto replied"),
    DEBIT_CAP_EXCEEDED(1016, "Debit cap exceeded"),
    CREDIT_CAP_EXCEEDED(1017, "Credit cap exceeded"),
    LIMITS_EXCEEDED(1018, "Limits exceeded"),
    TRANSACTION_AMOUNT_OUT_OF_RANGE(1019, "Transaction amount out of range"),
    TRANSACTION_COUNT_OUT_OF_RANGE(1020, "Transaction count out of range"),
    UNREGISTERED_PSP_PAYMENT(1100, "Unregistered PSP Payment"),
    ALIAS_ALREADY_USED(1101, "Alias already used"),
    MAX_ACCOUNTS_REACHED(1102, "Maximum number of accounts reached"),
    COULD_NOT_RESOLVE_DEBTOR(1103, "Could not resolve debtor"),
    COULD_NOT_RESOLVE_CREDITOR(1104, "Could not resolve creditor"),
    REPLY_TIMEOUT_REACHED(1105, "Reply timeout reached"),
    PAYMENT_AUTHORIZATION_FAILED(1106, "Payment Authorization Failed"),
    RISK_THRESHOLD_BREACHED(1107, "Risk threshold breached");

    private final int code;
    private final String description;

    PaymentRejectReasonCode(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static PaymentRejectReasonCode fromCode(int code) {
        for (PaymentRejectReasonCode reason : values()) {
            if (reason.code == code) {
                return reason;
            }
        }
        throw new IllegalArgumentException("Unknown rejection reason code: " + code);
    }
}

