package om.gov.cbo.mpcss.enums;

/**
 * Queue categories matching the MPCSS queue naming convention (Section 10).
 * Queue format: mpc.{participant_short_name}.{category}.{direction}
 */
public enum QueueCategory {

    PAYMENT("payment", "Payment messages (pacs.008, pacs.003, pacs.004)"),
    REPLY("reply", "Payment status reports (pacs.002)"),
    REGISTRATION("reg", "Individual registration messages (cstmrreg.*)"),
    BULK_REGISTRATION("regfile", "Bulk registration files (CSV, compressed)"),
    HEARTBEAT("heartbeat", "Heartbeat messages"),
    PAYMENT_ENQUIRY("paymentenquiry", "Payment enquiry messages"),
    NAME_VERIFICATION("nameverification", "Customer name verification (cstmrreg.20/21)"),
    DEFAULT_ACCOUNT("defaultaccount", "Check default account (cstmrreg.25/26)"),
    REPORTS("reports", "Report files (NCP, Reconciliation, Transaction)");

    private final String queueSegment;
    private final String description;

    QueueCategory(String queueSegment, String description) {
        this.queueSegment = queueSegment;
        this.description = description;
    }

    public String getQueueSegment() {
        return queueSegment;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Build inward queue name for a participant.
     */
    public String buildInwardQueueName(String participantShortName) {
        return "mpc." + participantShortName + "." + queueSegment + ".inward";
    }

    /**
     * Build outward queue name for a participant.
     */
    public String buildOutwardQueueName(String participantShortName) {
        if (this == REPORTS) {
            throw new UnsupportedOperationException("Reports queue is inward only");
        }
        return "mpc." + participantShortName + "." + queueSegment + ".outward";
    }
}

