package om.gov.cbo.mpcss.model;

import om.gov.cbo.mpcss.enums.MpcssMessageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents the MPCSS message wrapper structure as defined in Section 10.2.
 * Non-binary messages are wrapped in an XML envelope:
 * <pre>{@code
 * <MpcssMessage>
 *   <type>PACS.008</type>
 *   <format>MX</format>
 *   <date>2024-01-01T10:00:00</date>
 *   <signature>Base64EncodedSignature</signature>
 *   <content>...ISO20022 XML...</content>
 * </MpcssMessage>
 * }</pre>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MpcssMessageWrapper {

    /** Message type code, e.g. "PACS.008", "CSTMRREG.01" */
    private String type;

    /** Message format: "MX" for ISO20022, empty for non-payment messages */
    private String format;

    /** Message creation date in ISO DateTime format */
    private String date;

    /** Digital signature in Base64 */
    private String signature;

    /** The actual message content (ISO 20022 XML body) */
    private String content;

    /** JMS correlation ID for request-reply correlation (Section 10.5) */
    private String correlationId;

    /** Certificate number from JMS header for signature verification */
    private String certificateNumber;

    /** Resolved message type enum */
    private MpcssMessageType messageType;
}

