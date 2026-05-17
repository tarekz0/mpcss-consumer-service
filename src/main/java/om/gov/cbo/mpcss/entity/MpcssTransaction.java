package om.gov.cbo.mpcss.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Persists payment transactions sent/received through MPCSS.
 * Covers pacs.008 (credit), pacs.003 (debit), pacs.004 (return).
 */
@Entity
@Table(name = "mpcss_transactions", indexes = {
        @Index(name = "idx_msg_id", columnList = "messageId", unique = true),
        @Index(name = "idx_end_to_end", columnList = "endToEndId"),
        @Index(name = "idx_status", columnList = "status"),
        @Index(name = "idx_created", columnList = "createdAt")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MpcssTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Our generated message ID: {4-char prefix}{12-char ref} */
    @Column(nullable = false, unique = true, length = 16)
    private String messageId;

    /** PACS_008, PACS_003, PACS_004 */
    @Column(nullable = false, length = 20)
    private String messageType;

    /** OUTWARD (we sent) or INWARD (we received) */
    @Column(nullable = false, length = 10)
    @Enumerated(EnumType.STRING)
    private Direction direction;

    /** PENDING, SENT, ACCEPTED, REJECTED, TIMEOUT, SETTLED */
    @Column(nullable = false, length = 20)
    private String status;

    @Column(precision = 18, scale = 5)
    private BigDecimal amount;

    @Column(length = 3)
    private String currency;

    private LocalDate settlementDate;

    @Column(length = 10)
    private String localInstrumentCode;

    @Column(length = 35)
    private String categoryPurpose;

    // Debtor (Payer)
    @Column(length = 140)
    private String debtorName;
    @Column(length = 34)
    private String debtorAccountId;
    @Column(length = 11)
    private String debtorAgentBic;

    // Creditor (Beneficiary)
    @Column(length = 140)
    private String creditorName;
    @Column(length = 34)
    private String creditorAccountId;
    @Column(length = 11)
    private String creditorAgentBic;

    @Column(length = 35)
    private String endToEndId;

    @Column(length = 35)
    private String transactionId;

    @Column(length = 35)
    private String instructionId;

    @Column(length = 140)
    private String remittanceInfo;

    private boolean onUs;

    // Status report fields (from pacs.002 reply)
    @Column(length = 4)
    private String replyStatus;

    @Column(length = 10)
    private String replyReasonCode;

    @Column(length = 500)
    private String replyAdditionalInfo;

    /** JMS correlation ID for matching request-reply */
    @Column(length = 100)
    private String correlationId;

    /** Raw XML content of outward message */
    @Column(columnDefinition = "TEXT")
    private String rawOutwardXml;

    /** Raw XML content of inward reply */
    @Column(columnDefinition = "TEXT")
    private String rawInwardXml;

    /** PEP encrypted data (MPCSSv2 PIN/OTP) */
    @Column(columnDefinition = "TEXT")
    private String pepEncryptedData;

    /** Forwarded to core banking? */
    private boolean forwardedToCoreBanking;

    /** Core banking reference ID */
    @Column(length = 100)
    private String coreBankingRef;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private LocalDateTime repliedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum Direction {
        OUTWARD, INWARD
    }
}

