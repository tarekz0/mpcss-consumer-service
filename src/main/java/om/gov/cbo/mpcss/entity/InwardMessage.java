package om.gov.cbo.mpcss.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Logs all inward messages received from MPCSS for audit/debugging.
 */
@Entity
@Table(name = "inward_messages", indexes = {
        @Index(name = "idx_inward_type", columnList = "messageType"),
        @Index(name = "idx_inward_queue", columnList = "queueName"),
        @Index(name = "idx_inward_received", columnList = "receivedAt")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InwardMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 100)
    private String jmsMessageId;

    @Column(length = 100)
    private String correlationId;

    @Column(length = 30)
    private String messageType;

    @Column(length = 100)
    private String queueName;

    private boolean signatureValid;

    @Column(columnDefinition = "LONGTEXT")
    private String rawContent;

    private boolean processed;

    @Column(length = 500)
    private String processingError;

    @Column(nullable = false, updatable = false)
    private LocalDateTime receivedAt;

    private LocalDateTime processedAt;

    @PrePersist
    protected void onCreate() {
        receivedAt = LocalDateTime.now();
    }
}

