package mobi.foo.mpcss.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Persists customer/account registration requests and responses.
 * Covers cstmrreg.01-08 (requests) and cstmrreg.10 (responses).
 */
@Entity
@Table(name = "customer_registrations", indexes = {
        @Index(name = "idx_reg_msg_id", columnList = "messageId", unique = true),
        @Index(name = "idx_reg_mobile", columnList = "mobileNumber"),
        @Index(name = "idx_reg_status", columnList = "status")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerRegistration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 16)
    private String messageId;

    /** cstmrreg.01.01 through cstmrreg.08.01 */
    @Column(nullable = false, length = 30)
    private String messageType;

    /** PENDING, ACCEPTED, REJECTED */
    @Column(nullable = false, length = 20)
    private String status;

    @Column(length = 10)
    private String customerIdType;

    @Column(length = 50)
    private String customerIdNumber;

    @Column(length = 2)
    private String idIssuingCountry;

    @Column(length = 70)
    private String firstName;

    @Column(length = 70)
    private String lastName;

    @Column(length = 70)
    private String nickname;

    @Column(length = 35)
    private String mobileNumber;

    @Column(length = 5)
    private String accountType;

    @Column(length = 35)
    private String registrationCode;

    private Boolean isBanked;

    @Column(length = 3)
    private String currency;

    @Column(length = 35)
    private String accountAlias;

    private Boolean isDefaultAccount;

    @Column(length = 35)
    private String merchantId;

    @Column(length = 500)
    private String additionalInfo;

    // Response from MPCSS (cstmrreg.10)
    @Column(length = 10)
    private String responseReasonCode;

    @Column(length = 500)
    private String responseAdditionalInfo;

    @Column(columnDefinition = "TEXT")
    private String rawOutwardXml;

    @Column(columnDefinition = "TEXT")
    private String rawInwardXml;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

