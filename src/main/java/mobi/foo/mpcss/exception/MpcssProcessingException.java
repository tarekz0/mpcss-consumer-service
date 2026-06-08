package mobi.foo.mpcss.exception;

import mobi.foo.mpcss.enums.PaymentRejectReasonCode;

/**
 * Exception thrown when MPCSS message processing fails.
 */
public class MpcssProcessingException extends RuntimeException {

    private final PaymentRejectReasonCode reasonCode;
    private final String originalMessageId;

    public MpcssProcessingException(String message, PaymentRejectReasonCode reasonCode) {
        super(message);
        this.reasonCode = reasonCode;
        this.originalMessageId = null;
    }

    public MpcssProcessingException(String message, PaymentRejectReasonCode reasonCode,
                                     String originalMessageId) {
        super(message);
        this.reasonCode = reasonCode;
        this.originalMessageId = originalMessageId;
    }

    public MpcssProcessingException(String message, Throwable cause, PaymentRejectReasonCode reasonCode) {
        super(message, cause);
        this.reasonCode = reasonCode;
        this.originalMessageId = null;
    }

    public PaymentRejectReasonCode getReasonCode() {
        return reasonCode;
    }

    public String getOriginalMessageId() {
        return originalMessageId;
    }
}

