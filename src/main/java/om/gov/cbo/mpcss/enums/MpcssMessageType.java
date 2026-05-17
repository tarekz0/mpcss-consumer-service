package om.gov.cbo.mpcss.enums;

/**
 * MPCSS Message Types as defined in Section 12 (Appendix: Messages Tables).
 * Covers both MPCSSv1 and MPCSSv2 message types used for queue routing
 * and message processing.
 */
public enum MpcssMessageType {

    // ─── MPCSSv1 Payment Messages ───────────────────────────────────────
    PACS_008("PACS.008", "pacs.008.001.05", "Direct Credit (FIToFICustomerCreditTransfer)"),
    PACS_003("PACS.003", "pacs.003.001.05", "Direct Debit"),
    PACS_004("PACS.004", "pacs.004.001.05", "Payment Return (Refund)"),
    PACS_002("PACS.002", "pacs.002.001.06", "Payment Status Report"),

    // ─── MPCSSv1 Registration Messages ──────────────────────────────────
    CSTMRREG_01("CSTMRREG.01", "cstmrreg.01.01", "Customer Record Opening Request"),
    CSTMRREG_02("CSTMRREG.02", "cstmrreg.02.01", "Customer Record Maintenance Request"),
    CSTMRREG_03("CSTMRREG.03", "cstmrreg.03.01", "Customer Record Closing Request"),
    CSTMRREG_06("CSTMRREG.06", "cstmrreg.06.01", "Account Opening Request"),
    CSTMRREG_07("CSTMRREG.07", "cstmrreg.07.01", "Account Maintenance Request"),
    CSTMRREG_08("CSTMRREG.08", "cstmrreg.08.01", "Account Closing Request"),
    CSTMRREG_10("CSTMRREG.10", "cstmrreg.10.01", "Registration Response"),
    CSTMRREG_20("CSTMRREG.20", "cstmrreg.20.01", "Customer Name Verification Request"),
    CSTMRREG_21("CSTMRREG.21", "cstmrreg.21.01", "Customer Name Verification Response"),
    CSTMRREG_25("CSTMRREG.25", "cstmrreg.25.01", "Check Default Account Request"),
    CSTMRREG_26("CSTMRREG.26", "cstmrreg.26.01", "Check Default Account Response"),

    // ─── MPCSSv2 Registration Messages (NEW) ────────────────────────────
    CSTMRREG_30("CSTMRREG.30", "cstmrreg.30.01", "Accounts List Request"),
    CSTMRREG_31("CSTMRREG.31", "cstmrreg.31.01", "Accounts List Response"),
    CSTMRREG_33("CSTMRREG.33", "cstmrreg.33.01", "Set PIN Request"),
    CSTMRREG_34("CSTMRREG.34", "cstmrreg.34.01", "Set PIN Response"),
    CSTMRREG_35("CSTMRREG.35", "cstmrreg.35.01", "Get Participants List Request"),
    CSTMRREG_36("CSTMRREG.36", "cstmrreg.36.01", "Get Participants List Response"),
    CSTMRREG_37("CSTMRREG.37", "cstmrreg.37.01", "Get Wallets List Request"),
    CSTMRREG_38("CSTMRREG.38", "cstmrreg.38.01", "Get Wallets List Response"),

    // ─── MPCSSv2 Recurring Payment Messages (NEW) ───────────────────────
    PAIN_009("PAIN.009", "pain.009.001.06", "Recurring Payment Approval Request"),
    PAIN_011("PAIN.011", "pain.011.001.06", "Recurring Payment Cancellation Request"),
    PAIN_012("PAIN.012", "pain.012.001.06", "Recurring Payment Status Update"),

    // ─── MPCSSv2 Pre-Authorized Payment Messages (NEW) ──────────────────
    CAMT_056("CAMT.056", "camt.056.001.04", "Pre-Authorized Payment Void/Expiry Request"),
    CAMT_029("CAMT.029", "camt.029.001.04", "Pre-Authorized Payment Void/Expiry Status Update"),

    // ─── Binary Message Types ───────────────────────────────────────────
    REGISTRATION_REQUEST("REGISTRATION_REQUEST", "bulk.reg.request", "Bulk Registration Request"),
    REGISTRATION_RESPONSE("REGISTRATION_RESPONSE", "bulk.reg.response", "Bulk Registration Response"),
    NCP_REPORT("NCP_REPORT", "ncp.report", "NCP Report"),
    RECON_REPORT("RECON_REPORT", "recon.report", "Reconciliation Report"),
    TRANS_REPORT("TRANS_REPORT", "trans.report", "Transactions Report");

    private final String code;
    private final String isoCode;
    private final String description;

    MpcssMessageType(String code, String isoCode, String description) {
        this.code = code;
        this.isoCode = isoCode;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getIsoCode() {
        return isoCode;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Resolve message type from the <type> XML element value.
     */
    public static MpcssMessageType fromCode(String code) {
        if (code == null) return null;
        for (MpcssMessageType type : values()) {
            if (type.code.equalsIgnoreCase(code.trim())) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown MPCSS message type code: " + code);
    }

    /**
     * Resolve message type from ISO message code (e.g. "pacs.008.001.05").
     */
    public static MpcssMessageType fromIsoCode(String isoCode) {
        if (isoCode == null) return null;
        for (MpcssMessageType type : values()) {
            if (type.isoCode.equalsIgnoreCase(isoCode.trim())) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown ISO message code: " + isoCode);
    }

    public boolean isPaymentMessage() {
        return this == PACS_008 || this == PACS_003 || this == PACS_004;
    }

    public boolean isStatusReport() {
        return this == PACS_002;
    }

    public boolean isRegistrationMessage() {
        return code.startsWith("CSTMRREG");
    }

    public boolean isRecurringPayment() {
        return code.startsWith("PAIN");
    }

    public boolean isPreAuthorizedPayment() {
        return code.startsWith("CAMT");
    }

    public boolean isBinaryMessage() {
        return this == REGISTRATION_REQUEST || this == REGISTRATION_RESPONSE
                || this == NCP_REPORT || this == RECON_REPORT || this == TRANS_REPORT;
    }
}

