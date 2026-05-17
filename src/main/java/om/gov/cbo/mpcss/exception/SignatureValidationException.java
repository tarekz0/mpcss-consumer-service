package om.gov.cbo.mpcss.exception;

/**
 * Exception thrown when digital signature validation fails.
 * Corresponds to rejection code 1003 (Digital signature/Security error).
 */
public class SignatureValidationException extends MpcssProcessingException {

    public SignatureValidationException(String message) {
        super(message, om.gov.cbo.mpcss.enums.PaymentRejectReasonCode.DIGITAL_SIGNATURE_ERROR);
    }

    public SignatureValidationException(String message, Throwable cause) {
        super(message, cause, om.gov.cbo.mpcss.enums.PaymentRejectReasonCode.DIGITAL_SIGNATURE_ERROR);
    }
}

