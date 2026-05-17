package om.gov.cbo.mpcss.signature;

import om.gov.cbo.mpcss.config.MpcssProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.Certificate;
import java.util.Base64;

/**
 * Digital Signature Validator and Signer as defined in Section 10.6.
 *
 * <p>Signing procedure (Section 10.6):
 * <ol>
 *   <li>Construct message token: content + date (e.g. XML content + "2019-01-31T03:20:59")</li>
 *   <li>Create message digest from concatenated string (byte array)</li>
 *   <li>Encrypt digest using SHA256withRSA and PSP private key</li>
 *   <li>Convert to Base64 string</li>
 * </ol>
 *
 * <p>For binary messages, only the binary content is used (no date concatenation).</p>
 */
@Slf4j
@Component
public class DigitalSignatureValidator {

    private final MpcssProperties properties;
    private final ResourceLoader resourceLoader;

    private PrivateKey privateKey;
    private PublicKey publicKey;
    private KeyStore keyStore;
    private KeyStore trustStore;

    public DigitalSignatureValidator(MpcssProperties properties, ResourceLoader resourceLoader) {
        this.properties = properties;
        this.resourceLoader = resourceLoader;
    }

    @PostConstruct
    public void init() {
        if (!properties.getSignature().isEnabled()) {
            log.warn("Digital signature validation is DISABLED. Messages will not be verified.");
            return;
        }

        try {
            // Load keystore (our private key for signing outward messages)
            keyStore = loadKeyStore(
                    properties.getSignature().getKeystorePath(),
                    properties.getSignature().getKeystorePassword()
            );
            privateKey = (PrivateKey) keyStore.getKey(
                    properties.getSignature().getKeyAlias(),
                    properties.getSignature().getKeyPassword().toCharArray()
            );

            // Load truststore (PS-mpClear public key for verifying inward messages)
            trustStore = loadKeyStore(
                    properties.getSignature().getTruststorePath(),
                    properties.getSignature().getTruststorePassword()
            );

            log.info("Digital signature keys loaded successfully. Algorithm: {}",
                    properties.getSignature().getAlgorithm());
        } catch (Exception e) {
            log.warn("Failed to load digital signature keys: {}. " +
                    "Signature validation will be skipped until keys are configured.", e.getMessage());
        }
    }

    /**
     * Verify the digital signature of an inward message from PS-mpClear.
     *
     * @param content        The message content
     * @param dateTime       The message date (concatenated with content for verification)
     * @param signatureB64   The Base64-encoded digital signature
     * @param certificateNum The certificate number from JMS header
     * @return true if signature is valid, false otherwise
     */
    public boolean verifySignature(String content, String dateTime, String signatureB64,
                                   String certificateNum) {
        if (!properties.getSignature().isEnabled()) {
            log.debug("Signature validation disabled, skipping verification");
            return true;
        }

        if (signatureB64 == null || signatureB64.isBlank()) {
            log.warn("No signature provided in message");
            return false;
        }

        try {
            // Construct the token: content + date (Section 10.6 step 1)
            String messageToken = content + dateTime;

            // Get the public key of the sender (PS-mpClear)
            PublicKey senderPublicKey = resolvePublicKey(certificateNum);
            if (senderPublicKey == null) {
                log.error("Cannot resolve public key for certificate: {}", certificateNum);
                return false;
            }

            // Verify signature
            Signature sig = Signature.getInstance(properties.getSignature().getAlgorithm());
            sig.initVerify(senderPublicKey);
            sig.update(messageToken.getBytes(StandardCharsets.UTF_8));

            byte[] signatureBytes = Base64.getDecoder().decode(signatureB64);
            boolean verified = sig.verify(signatureBytes);

            if (!verified) {
                log.warn("Digital signature verification FAILED for message");
            } else {
                log.debug("Digital signature verified successfully");
            }

            return verified;
        } catch (Exception e) {
            log.error("Error verifying digital signature: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Verify signature for binary messages (only content, no date concatenation).
     */
    public boolean verifyBinarySignature(byte[] content, String signatureB64, String certificateNum) {
        if (!properties.getSignature().isEnabled()) return true;

        try {
            PublicKey senderPublicKey = resolvePublicKey(certificateNum);
            if (senderPublicKey == null) return false;

            Signature sig = Signature.getInstance(properties.getSignature().getAlgorithm());
            sig.initVerify(senderPublicKey);
            sig.update(content);

            byte[] signatureBytes = Base64.getDecoder().decode(signatureB64);
            return sig.verify(signatureBytes);
        } catch (Exception e) {
            log.error("Error verifying binary digital signature: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Sign an outward message using our private key (Section 10.6).
     *
     * @param content  The message content
     * @param dateTime The message date
     * @return Base64-encoded digital signature
     */
    public String signMessage(String content, String dateTime) {
        if (!properties.getSignature().isEnabled() || privateKey == null) {
            log.warn("Cannot sign message: signature disabled or private key not loaded");
            return "";
        }

        try {
            String messageToken = content + dateTime;

            Signature sig = Signature.getInstance(properties.getSignature().getAlgorithm());
            sig.initSign(privateKey);
            sig.update(messageToken.getBytes(StandardCharsets.UTF_8));

            byte[] signatureBytes = sig.sign();
            return Base64.getEncoder().encodeToString(signatureBytes);
        } catch (Exception e) {
            log.error("Error signing message: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to sign message", e);
        }
    }

    /**
     * Sign binary content (no date concatenation).
     */
    public String signBinaryMessage(byte[] content) {
        if (!properties.getSignature().isEnabled() || privateKey == null) return "";

        try {
            Signature sig = Signature.getInstance(properties.getSignature().getAlgorithm());
            sig.initSign(privateKey);
            sig.update(content);

            byte[] signatureBytes = sig.sign();
            return Base64.getEncoder().encodeToString(signatureBytes);
        } catch (Exception e) {
            log.error("Error signing binary message: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to sign binary message", e);
        }
    }

    // ─── Private Methods ────────────────────────────────────────────────

    private PublicKey resolvePublicKey(String certificateNum) {
        if (trustStore == null) return null;

        try {
            // Try exact alias first, then fallback to first available
            String alias = certificateNum != null ? certificateNum : "mpcss-server";
            Certificate cert = trustStore.getCertificate(alias);
            if (cert == null) {
                // Fallback: iterate through aliases
                var aliases = trustStore.aliases();
                while (aliases.hasMoreElements()) {
                    cert = trustStore.getCertificate(aliases.nextElement());
                    if (cert != null) break;
                }
            }
            return cert != null ? cert.getPublicKey() : null;
        } catch (Exception e) {
            log.error("Error resolving public key: {}", e.getMessage());
            return null;
        }
    }

    private KeyStore loadKeyStore(String path, String password) throws Exception {
        KeyStore ks = KeyStore.getInstance("JKS");
        try (InputStream is = resourceLoader.getResource(path).getInputStream()) {
            ks.load(is, password.toCharArray());
        }
        return ks;
    }
}

