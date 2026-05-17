# MPCSS Consumer Service

**Mobile Payment Clearing and Switching System** — ActiveMQ Artemis Consumer Microservice

A Spring Boot microservice that integrates with the Central Bank of Oman's PS-mpClear system via Apache ActiveMQ Artemis. Built according to the **MPCSS Interface Specifications Document v3.3**.

Role: **PSP (Payment Service Provider)** — sends payment requests and receives responses from MPCSS.

> 📖 For full developer documentation, see [DEVELOPER_GUIDE.md](DEVELOPER_GUIDE.md)

---

## Architecture

```
mpcss-consumer-service/
├── config/                  # ActiveMQ Artemis + participant configuration
├── controller/              # REST APIs (payments, registration, enquiry)
├── service/                 # Business logic (PaymentService, RegistrationService, etc.)
├── listener/                # 10 JMS listeners for all inward queues
├── processor/               # Message routing → services
├── parser/                  # ISO 20022 XML parser
├── signature/               # SHA256withRSA digital signature sign/verify
├── producer/                # Outward message producer (builds XML + signs + sends)
├── entity/                  # JPA entities (MySQL: transactions, registrations, audit)
├── repository/              # Spring Data JPA repositories
├── client/                  # Core Banking REST client (message forwarding)
├── dto/                     # ISO 20022 DTOs + REST request/response models
├── enums/                   # 28+ message types, rejection codes, status codes
├── exception/               # MPCSS-specific exceptions with rejection codes
└── util/                    # Message ID generator
```

## Supported Messages

**MPCSSv1**: pacs.008 (Credit), pacs.003 (Debit), pacs.004 (Refund), pacs.002 (Status Report), cstmrreg.01-10 (Registration), cstmrreg.20/21 (Name Verification), cstmrreg.25/26 (Default Account)

**MPCSSv2**: cstmrreg.30-38 (Accounts List, Set PIN, Participants, Wallets), pain.009/011/012 (Recurring Payments), camt.056/029 (Pre-Authorized Payments)

## Queue Structure

| Queue | Format |
|-------|--------|
| `mpc.{name}.payment.inward/outward` | XML (pacs.008/003/004) |
| `mpc.{name}.reply.inward/outward` | XML (pacs.002) |
| `mpc.{name}.reg.inward/outward` | XML (cstmrreg.*) |
| `mpc.{name}.regfile.inward/outward` | Binary/ZIP |
| `mpc.{name}.heartbeat.inward/outward` | XML |
| `mpc.{name}.paymentenquiry.inward/outward` | XML |
| `mpc.{name}.nameverification.inward/outward` | XML |
| `mpc.{name}.defaultaccount.inward/outward` | XML |
| `mpc.{name}.reports.inward` | Binary/ZIP |

## Quick Start

```bash
# Build
./gradlew clean build

# Run
./gradlew bootRun

# Run with custom config
MPCSS_PARTICIPANT_SHORT_NAME=nbo \
MPCSS_PARTICIPANT_CODE=NBO1 \
MPCSS_SIGNATURE_ENABLED=false \
./gradlew bootRun
```

## Prerequisites

- Java 17+
- MySQL 8+
- Apache ActiveMQ Artemis (hosted by CBO)
- CBO-issued digital certificate (JKS keystore)

## Configuration

All settings can be changed via `application.yml` or environment variables.
See [DEVELOPER_GUIDE.md](DEVELOPER_GUIDE.md) for full details.

| Setting | Env Variable |
|---------|--------------|
| Artemis broker | `ARTEMIS_BROKER_URL` |
| MySQL | `MYSQL_URL`, `MYSQL_USER`, `MYSQL_PASSWORD` |
| Participant | `MPCSS_PARTICIPANT_SHORT_NAME`, `MPCSS_PARTICIPANT_CODE`, `MPCSS_PARTICIPANT_BIC` |
| Certificates | `MPCSS_KEYSTORE_PASSWORD`, `MPCSS_KEY_ALIAS`, `MPCSS_TRUSTSTORE_PASSWORD` |
| Signature | `MPCSS_SIGNATURE_ENABLED` |
| Core Banking | `CORE_BANKING_URL` |
