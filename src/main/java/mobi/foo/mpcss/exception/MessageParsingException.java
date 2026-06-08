package mobi.foo.mpcss.exception;

/**
 * Exception thrown when XML message parsing fails.
 * Corresponds to rejection code 1002 (Parsing error).
 */
public class MessageParsingException extends MpcssProcessingException {

    public MessageParsingException(String message) {
        super(message, mobi.foo.mpcss.enums.PaymentRejectReasonCode.PARSING_ERROR);
    }

    public MessageParsingException(String message, Throwable cause) {
        super(message, cause, mobi.foo.mpcss.enums.PaymentRejectReasonCode.PARSING_ERROR);
    }
}

