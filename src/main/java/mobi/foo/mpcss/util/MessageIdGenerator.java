package mobi.foo.mpcss.util;

import mobi.foo.mpcss.config.MpcssProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Utility for generating MPCSS message IDs as per Section 10.7.
 * Format: {ParticipantNumericCode}{MessageReferenceID}
 * - Participant Prefix: 4 characters (e.g. "BNK1")
 * - Message Reference ID: 12 alphanumeric characters (unique per PSP)
 */
@Component
@RequiredArgsConstructor
public class MessageIdGenerator {

    private final MpcssProperties properties;
    private final AtomicLong sequence = new AtomicLong(0);

    /**
     * Generate a unique 16-character message ID.
     * Example: BNK1000000000001
     */
    public String generate() {
        String prefix = properties.getParticipant().getNumericCode();
        if (prefix == null || prefix.length() != 4) {
            throw new IllegalStateException("Participant numeric code must be 4 characters, got: " + prefix);
        }
        long seq = sequence.incrementAndGet() % 1_000_000_000_000L;
        return prefix + String.format("%012d", seq);
    }

    /**
     * Validate a message ID format: 4-char prefix + 12-char alphanumeric reference.
     */
    public static boolean isValid(String messageId) {
        return messageId != null && messageId.length() == 16
                && messageId.substring(0, 4).matches("[A-Z0-9]{4}")
                && messageId.substring(4).matches("[A-Za-z0-9]{12}");
    }

    /**
     * Extract the participant code from a message ID.
     */
    public static String extractParticipantCode(String messageId) {
        if (!isValid(messageId)) return null;
        return messageId.substring(0, 4);
    }
}

