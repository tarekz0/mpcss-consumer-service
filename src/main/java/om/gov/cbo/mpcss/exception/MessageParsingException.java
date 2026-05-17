package om.gov.cbo.mpcss.exception;

/**
 * Exception thrown when XML message parsing fails.
 * Corresponds to rejection code 1002 (Parsing error).
 */
public class MessageParsingException extends MpcssProcessingException {

    public MessageParsingException(String message) {
        super(message, om.gov.cbo.mpcss.enums.PaymentRejectReasonCode.PARSING_ERROR);
    }

    public MessageParsingException(String message, Throwable cause) {
        super(message, cause, om.gov.cbo.mpcss.enums.PaymentRejectReasonCode.PARSING_ERROR);
    }
}

