package mobi.foo.mpcss.exception;

/**
 * Exception thrown when digital signature validation fails.
 * Corresponds to rejection code 1003 (Digital signature/Security error).
 */
public class SignatureValidationException extends MpcssProcessingException {

    public SignatureValidationException(String message) {
        super(message, mobi.foo.mpcss.enums.PaymentRejectReasonCode.DIGITAL_SIGNATURE_ERROR);
    }

    public SignatureValidationException(String message, Throwable cause) {
        super(message, cause, mobi.foo.mpcss.enums.PaymentRejectReasonCode.DIGITAL_SIGNATURE_ERROR);
    }
}

