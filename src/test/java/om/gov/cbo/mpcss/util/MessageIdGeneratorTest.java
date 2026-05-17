package om.gov.cbo.mpcss.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MessageIdGeneratorTest {

    @Test
    void shouldValidateCorrectMessageId() {
        assertTrue(MessageIdGenerator.isValid("BNK1000000000001"));
        assertTrue(MessageIdGenerator.isValid("ABC2123456789012"));
    }

    @Test
    void shouldRejectInvalidMessageId() {
        assertFalse(MessageIdGenerator.isValid(null));
        assertFalse(MessageIdGenerator.isValid(""));
        assertFalse(MessageIdGenerator.isValid("SHORT"));
        assertFalse(MessageIdGenerator.isValid("BNK100000000000100")); // too long
    }

    @Test
    void shouldExtractParticipantCode() {
        assertEquals("BNK1", MessageIdGenerator.extractParticipantCode("BNK1000000000001"));
        assertNull(MessageIdGenerator.extractParticipantCode("invalid"));
    }
}

