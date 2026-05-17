# MPCSS Consumer Service

**Mobile Payment Clearing and Switching System** — ActiveMQ Artemis Consumer Microservice

A Spring Boot microservice that integrates with the Central Bank of Oman's PS-mpClear system via Apache ActiveMQ Artemis. Built according to the **MPCSS Interface Specifications Document v3.3**.

## Architecture

```
mpcss-consumer-service
├── config/
│   ├── JmsConfig                      # ActiveMQ Artemis JMS configuration
│   └── MpcssProperties                # MPCSS participant & queue properties
├── listener/
│   └── MpcssMessageListener           # JMS listeners for all inward queues
├── processor/
│   └── MpcssMessageProcessor          # Central message routing & business logic
├── parser/
│   └── Iso20022XmlParser              # ISO 20022 XML message parser
├── signature/
│   └── DigitalSignatureValidator      # SHA256withRSA digital signature signing/verification
├── producer/
│   └── MpcssResponseProducer          # Outward message producer (replies, payments, etc.)
├── dto/
│   ├── CreditTransferMessage          # pacs.008 - FIToFICustomerCreditTransfer
│   ├── PaymentStatusReport            # pacs.002 - Payment Status Report
│   ├── RegistrationMessage            # cstmrreg.* - Registration messages
│   ├── RecurringPaymentMessage        # pain.009/011/012 - Recurring payments
│   ├── PreAuthorizedPaymentMessage    # camt.056/029 - Pre-authorized payments
│   ├── NcpReport                      # NCP Report (binary)
│   ├── TransactionReportEntry         # Transaction Report entries (binary)
│   └── PepData                        # PIN Encryption Page data structure
├── model/
│   └── MpcssMessageWrapper            # MPCSS XML message envelope
├── enums/
│   ├── MpcssMessageType               # All MPCSSv1 & v2 message types
│   ├── PaymentStatus                  # RJCT, ACSP, ACSC
│   ├── PaymentRejectReasonCode        # User (1-999) & System (1000+) rejection codes
│   └── QueueCategory                  # Queue naming convention helper
├── exception/
│   ├── MpcssProcessingException       # Base processing exception
│   ├── SignatureValidationException   # Signature failure (code 1003)
│   └── MessageParsingException        # Parsing failure (code 1002)
└── util/
    └── MessageIdGenerator             # MsgId: {4-char prefix}{12-char reference}
```

## Supported Message Types

### MPCSSv1 Messages
| Message | ISO Code | Description |
|---------|----------|-------------|
| pacs.008.001.05 | PACS.008 | Direct Credit (FIToFICustomerCreditTransfer) |
| pacs.003.001.05 | PACS.003 | Direct Debit |
| pacs.004.001.05 | PACS.004 | Payment Return (Refund) |
| pacs.002.001.06 | PACS.002 | Payment Status Report |
| cstmrreg.01-08 | CSTMRREG.* | Customer/Account Registration CRUD |
| cstmrreg.10.01 | CSTMRREG.10 | Registration Response |
| cstmrreg.20/21 | CSTMRREG.20/21 | Customer Name Verification |
| cstmrreg.25/26 | CSTMRREG.25/26 | Check Default Account |

### MPCSSv2 Messages (New)
| Message | ISO Code | Description |
|---------|----------|-------------|
| cstmrreg.30/31 | CSTMRREG.30/31 | Accounts List Request/Response |
| cstmrreg.33/34 | CSTMRREG.33/34 | Set PIN Request/Response |
| cstmrreg.35/36 | CSTMRREG.35/36 | Get Participants List |
| cstmrreg.37/38 | CSTMRREG.37/38 | Get Wallets List |
| pain.009.001.06 | PAIN.009 | Recurring Payment Approval Request |
| pain.011.001.06 | PAIN.011 | Recurring Payment Cancellation |
| pain.012.001.06 | PAIN.012 | Recurring Payment Status Update |
| camt.056.001.04 | CAMT.056 | Pre-Authorized Payment Void/Expiry |
| camt.029.001.04 | CAMT.029 | Pre-Authorized Payment Status Update |

## Queue Structure

Each participant has the following queues (Section 10):

| Queue | Direction | Format |
|-------|-----------|--------|
| `mpc.{name}.payment.inward/outward` | Both | XML (pacs.008/003/004) |
| `mpc.{name}.reply.inward/outward` | Both | XML (pacs.002) |
| `mpc.{name}.reg.inward/outward` | Both | XML (cstmrreg.*) |
| `mpc.{name}.regfile.inward/outward` | Both | Binary/ZIP (bulk CSV) |
| `mpc.{name}.heartbeat.inward/outward` | Both | XML |
| `mpc.{name}.paymentenquiry.inward/outward` | Both | XML |
| `mpc.{name}.nameverification.inward/outward` | Both | XML (cstmrreg.20/21) |
| `mpc.{name}.defaultaccount.inward/outward` | Both | XML (cstmrreg.25/26) |
| `mpc.{name}.reports.inward` | Inward only | Binary/ZIP (NCP, Recon, Txn) |

## Digital Signature (Section 10.6)

- **Algorithm**: SHA-256 hashing + RSA encryption
- **Non-binary**: Sign `content + dateTime` → SHA256withRSA → Base64
- **Binary**: Sign binary content only → SHA256withRSA → Base64
- **JMS Header**: `certificateNumber` for key lookup

## Configuration

Key configuration in `application.yml`:
```yaml
mpcss:
  participant:
    short-name: bankx          # Used in queue names
    numeric-code: BNK1         # 4-char MsgId prefix
    bic: BANKOMRUXXX           # Participant BIC
  signature:
    enabled: true
    keystore-path: classpath:keystore/mpcss-keystore.jks
    truststore-path: classpath:keystore/mpcss-truststore.jks
```

## Build & Run

```bash
# Build
./mvnw clean package

# Run
./mvnw spring-boot:run

# Run with custom participant
MPCSS_PARTICIPANT_SHORT_NAME=nbo MPCSS_PARTICIPANT_CODE=NBO1 ./mvnw spring-boot:run
```

## Prerequisites

- Java 17+
- Apache ActiveMQ Artemis broker
- CBO-issued digital certificate (JKS keystore)
- CBO MQ server client certificate

