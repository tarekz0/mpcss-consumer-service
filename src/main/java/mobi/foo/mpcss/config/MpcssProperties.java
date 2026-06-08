package mobi.foo.mpcss.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for MPCSS participant and queue settings.
 * Maps to the 'mpcss' prefix in application.yml.
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "mpcss")
public class MpcssProperties {

    private Participant participant = new Participant();
    private Queues queues = new Queues();
    private Signature signature = new Signature();

    @Data
    public static class Participant {
        /** Short name used in queue naming, e.g. "nbo" */
        private String shortName;
        /** 4-char numeric code prefix for MsgId, e.g. "BNK1" */
        private String numericCode;
        /** BIC code of the participant */
        private String bic;
    }

    @Data
    public static class Queues {
        // Inward (consume)
        private String paymentInward;
        private String replyInward;
        private String registrationInward;
        private String bulkRegistrationInward;
        private String heartbeatInward;
        private String paymentEnquiryInward;
        private String nameVerificationInward;
        private String defaultAccountInward;
        private String reportsInward;

        // Outward (produce)
        private String paymentOutward;
        private String replyOutward;
        private String registrationOutward;
        private String bulkRegistrationOutward;
        private String heartbeatOutward;
        private String paymentEnquiryOutward;
        private String nameVerificationOutward;
        private String defaultAccountOutward;
    }

    @Data
    public static class Signature {
        private boolean enabled = true;
        private String algorithm = "SHA256withRSA";
        private String keystorePath;
        private String keystorePassword;
        private String keyAlias;
        private String keyPassword;
        private String truststorePath;
        private String truststorePassword;
    }
}

