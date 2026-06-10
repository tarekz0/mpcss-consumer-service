# MPCSS Consumer Service — Developer Guide

> هذا الملف يشرح كل تفاصيل المشروع، إيه الجاهز وإيه اللي محتاج تغييره من عندك.

---

## Table of Contents

- [System Flow](#system-flow)
- [Package Guide](#package-guide)
- [REST APIs](#rest-apis)
- [What's Ready](#whats-ready)
- [What You Need to Configure](#what-you-need-to-configure)

---

## System Flow

```
┌─────────────┐     REST API      ┌───────────────────┐    ActiveMQ     ┌──────────┐
│ Mobile App  │ ──────────────►  │ mpcss-consumer    │ ────────────►  │  MPCSS   │
│ or Service  │                  │    service        │                │  (CBO)   │
└─────────────┘                  └───────────────────┘                └──────────┘
                                        │  ▲                              │
                                        │  │  JMS Inward Messages         │
                                        ▼  │                              │
                                  ┌──────────────┐                       │
                                  │   MySQL DB   │                       │
                                  └──────────────┘                       │
                                        │                                │
                                        ▼                                │
                                 ┌──────────────┐       ◄────────────────┘
                                 │ Core Banking │
                                 │   Service    │
                                 └──────────────┘
```

### Outward Flow (We send to MPCSS)
1. Mobile App calls REST API (e.g. `POST /api/v1/payments/credit`)
2. `PaymentService` builds ISO 20022 XML (pacs.008), saves to MySQL
3. `MpcssResponseProducer` signs the message + sends to ActiveMQ outward queue
4. MPCSS receives and processes

### Inward Flow (We receive from MPCSS)
1. MPCSS sends message to our inward queue (e.g. pacs.002 status report)
2. `MpcssMessageListener` picks it up from ActiveMQ
3. `DigitalSignatureValidator` verifies the signature
4. `MpcssMessageProcessor` routes to the correct service
5. Service saves to MySQL + forwards to Core Banking via REST

---

## Package Guide

| Package | Role | Key Details |
|---------|------|-------------|
| `config/` | Settings | ActiveMQ listeners + participant identity (short-name, BIC, queues) |
| `controller/` | REST APIs | 12 endpoints for mobile app + internal services |
| `service/` | Business Logic | Builds XML, saves to DB, forwards to core banking |
| `listener/` | JMS Consumer | Listens on 10 inward queues (XML + binary) |
| `processor/` | Message Router | Determines message type → delegates to service |
| `parser/` | XML Parser | Converts ISO 20022 XML to Java objects |
| `signature/` | Security | SHA256+RSA digital signature sign/verify |
| `producer/` | JMS Producer | Builds envelope, signs, sends to outward queues |
| `entity/` | Database | 3 MySQL tables for transactions, registrations, audit |
| `repository/` | Data Access | Spring Data JPA queries |
| `client/` | HTTP Client | Forwards messages to downstream Core Banking service |
| `dto/` | Data Models | All ISO 20022 message structures + REST request/response |
| `enums/` | Constants | 28+ message types, 20+ rejection codes, status codes |

---

## REST APIs

### Payment Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/v1/payments/credit` | Send a Direct Credit payment (pacs.008) to MPCSS |
| `GET` | `/api/v1/payments/{messageId}` | Get payment status by message ID |
| `GET` | `/api/v1/payments?status=SENT` | List payments by status |
| `GET` | `/api/v1/payments/account/{accountId}?from=&to=` | Payment history for an account |

### Registration Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/v1/registrations` | Submit registration (CUSTOMER_OPEN/CLOSE, ACCOUNT_OPEN/CLOSE, etc.) |
| `GET` | `/api/v1/registrations/{messageId}` | Get registration status |
| `GET` | `/api/v1/registrations/mobile/{number}` | List registrations by mobile number |
| `GET` | `/api/v1/registrations?status=PENDING` | List registrations by status |

### Enquiry Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/v1/enquiry/name-verification` | Verify customer name (before payment) |
| `POST` | `/api/v1/enquiry/default-account` | Check default account for mobile number |
| `POST` | `/api/v1/enquiry/payment` | Payment enquiry (check status via MPCSS) |
| `POST` | `/api/v1/enquiry/heartbeat` | Send heartbeat to MPCSS |
| `POST` | `/api/v1/enquiry/participants` | Pull participants list (MPCSSv2) |

### Example: Initiate Payment

```bash
curl -X POST http://localhost:8080/api/v1/payments/credit \
  -H "Content-Type: application/json" \
  -d '{
    "debtorAccount": "OM12BANK0000001234567890",
    "debtorName": "Ahmed Al-Balushi",
    "creditorIdentifier": "96898533444",
    "creditorIdentifierType": "MOBILE",
    "amount": 50.000,
    "currency": "OMR",
    "description": "Payment for services"
  }'
```

Response:
```json
{
  "success": true,
  "message": "Payment submitted to MPCSS",
  "data": {
    "messageId": "BNK1000000000001",
    "status": "SENT",
    "amount": 50.000,
    "currency": "OMR"
  }
}
```

### Example: Register Customer

```bash
curl -X POST http://localhost:8080/api/v1/registrations \
  -H "Content-Type: application/json" \
  -d '{
    "registrationType": "CUSTOMER_OPEN",
    "customerIdType": "1",
    "customerIdNumber": "12345678",
    "idIssuingCountry": "OM",
    "firstName": "Ahmed",
    "lastName": "Al-Balushi",
    "mobileNumber": "96898533444"
  }'
```

### Example: Name Verification

```bash
curl -X POST http://localhost:8080/api/v1/enquiry/name-verification \
  -H "Content-Type: application/json" \
  -d '{
    "identifier": "96898533444",
    "identifierType": "MOBILE"
  }'
```

---

## What's Ready

| Component | Status |
|-----------|--------|
| Gradle build (Spring Boot 3.3 + Artemis + JPA + MySQL) | ✅ Complete |
| 10 JMS Listeners for all inward queues | ✅ Complete |
| ISO 20022 XML Parser (pacs.008, pacs.002, cstmrreg.*) | ✅ Complete |
| Digital Signature (SHA256withRSA sign + verify) | ✅ Complete |
| XML Builders (pacs.008, pacs.002, cstmrreg.01-08) | ✅ Complete |
| MySQL Persistence (3 tables) | ✅ Complete |
| 12 REST API Endpoints | ✅ Complete |
| Core Banking Client (REST forwarding) | ✅ Complete |
| MPCSS Message Envelope (type, format, date, signature, content) | ✅ Complete |
| Global Exception Handler + Rejection Codes | ✅ Complete |
| 28+ Message Types mapped (MPCSSv1 + v2) | ✅ Complete |
| Unit Tests (parser + message ID) | ✅ Complete |

---

## What You Need to Configure

### 1. ActiveMQ Artemis Connection

In `application.yml`, update the broker connection provided by CBO:

```yaml
spring:
  artemis:
    broker-url: tcp://YOUR_ARTEMIS_HOST:61616   # ← CBO ActiveMQ broker address
    user: YOUR_USERNAME                          # ← Provided by CBO
    password: YOUR_PASSWORD                      # ← Provided by CBO
```

Or via environment variables:
```bash
export ARTEMIS_BROKER_URL=tcp://mpcss-broker.cbo.gov.om:61616
export ARTEMIS_USER=your_user
export ARTEMIS_PASSWORD=your_password
```

---

### 2. MySQL Database

```yaml
spring:
  datasource:
    url: jdbc:mysql://YOUR_HOST:3306/mpcss_consumer
    username: YOUR_DB_USER
    password: YOUR_DB_PASSWORD
```

Or via environment variables:
```bash
export MYSQL_URL=jdbc:mysql://db-server:3306/mpcss_consumer
export MYSQL_USER=mpcss_app
export MYSQL_PASSWORD=secure_password
```

Tables are **auto-created** by JPA (`ddl-auto: update`):
- `mpcss_transactions` — all payment transactions (outward + inward)
- `customer_registrations` — registration records
- `inward_messages` — audit log of all messages received from MPCSS

---

### 3. Participant Identity

Your bank's identity in the MPCSS system:

```yaml
mpcss:
  participant:
    short-name: nbo          # ← Your bank's short name (used in queue names)
    numeric-code: NBO1       # ← 4-char code (prefix for MsgId generation)
    bic: NBOKOMRUXXX         # ← Your bank's BIC code
```

Or via environment variables:
```bash
export MPCSS_PARTICIPANT_SHORT_NAME=nbo
export MPCSS_PARTICIPANT_CODE=NBO1
export MPCSS_PARTICIPANT_BIC=NBOKOMRUXXX
```

> This determines your queue names: `mpc.nbo.payment.outward`, `mpc.nbo.reply.inward`, etc.

---

### 4. Digital Certificates (from CBO)

Place your PKCS12 keystore files in:

```
src/main/resources/keystore/
├── mpcss-keystore.p12         ← Your private key (for signing outward messages & MQ auth)
└── mpcss-truststore.p12       ← CBO's certificate chain (for verifying inward messages & broker trust)
```

Then update the config:
```yaml
mpcss:
  signature:
    enabled: true
    keystore-type: PKCS12
    keystore-path: classpath:keystore/mpcss-keystore.p12
    keystore-password: YOUR_KEYSTORE_PASSWORD
    key-alias: YOUR_KEY_ALIAS
    key-password: YOUR_KEY_PASSWORD
    truststore-type: PKCS12
    truststore-path: classpath:keystore/mpcss-truststore.p12
    truststore-password: YOUR_TRUSTSTORE_PASSWORD
```

Or via environment variables:
```bash
export MPCSS_KEYSTORE_PASSWORD=real_password
export MPCSS_KEY_ALIAS=my-bank-cert
export MPCSS_KEY_PASSWORD=real_password
export MPCSS_TRUSTSTORE_PASSWORD=real_password
```

> **Development tip**: Set `MPCSS_SIGNATURE_ENABLED=false` to skip signature validation during development.

---

### 5. Core Banking Service URL

This service forwards all inward MPCSS messages to your Core Banking system via REST:

```yaml
mpcss:
  core-banking:
    base-url: http://YOUR_CORE_BANKING:8081/api/v1
    timeout-seconds: 30
```

Or via environment variables:
```bash
export CORE_BANKING_URL=http://core-banking:8081/api/v1
export CORE_BANKING_TIMEOUT=30
```

**Your Core Banking service must expose these endpoints** (or you can change the paths in `CoreBankingClient.java`):

| Method | Path | Called When |
|--------|------|------------|
| `POST` | `/payments/credit` | We receive an inward credit transfer (pacs.008) — need to credit our customer |
| `POST` | `/payments/status` | We receive a payment status report (pacs.002) — need to update payment status |
| `POST` | `/registration/response` | We receive a registration response (cstmrreg.10) |
| `POST` | `/customers/name-verified` | We receive a name verification result (cstmrreg.21) |
| `POST` | `/accounts/credit` | Credit processing request to the account |
| `POST` | `/reports/inward` | We receive a report (NCP / Reconciliation / Transaction) |

> **If your Core Banking API paths are different**, just update them in:
> `src/main/java/om/gov/cbo/mpcss/client/CoreBankingClient.java`

---

### 6. Quick Reference — All Configuration

| Setting | File | Env Variable |
|---------|------|--------------|
| Artemis broker URL | `application.yml` | `ARTEMIS_BROKER_URL` |
| Artemis username | `application.yml` | `ARTEMIS_USER` |
| Artemis password | `application.yml` | `ARTEMIS_PASSWORD` |
| MySQL URL | `application.yml` | `MYSQL_URL` |
| MySQL user | `application.yml` | `MYSQL_USER` |
| MySQL password | `application.yml` | `MYSQL_PASSWORD` |
| Participant short name | `application.yml` | `MPCSS_PARTICIPANT_SHORT_NAME` |
| Participant code (4-char) | `application.yml` | `MPCSS_PARTICIPANT_CODE` |
| Participant BIC | `application.yml` | `MPCSS_PARTICIPANT_BIC` |
| Keystore path | `application.yml` | `MPCSS_KEYSTORE_PATH` |
| Keystore password | `application.yml` | `MPCSS_KEYSTORE_PASSWORD` |
| Key alias | `application.yml` | `MPCSS_KEY_ALIAS` |
| Key password | `application.yml` | `MPCSS_KEY_PASSWORD` |
| Truststore path | `application.yml` | `MPCSS_TRUSTSTORE_PATH` |
| Truststore password | `application.yml` | `MPCSS_TRUSTSTORE_PASSWORD` |
| Signature enabled | `application.yml` | `MPCSS_SIGNATURE_ENABLED` |
| Core Banking URL | `application.yml` | `CORE_BANKING_URL` |
| Core Banking timeout | `application.yml` | `CORE_BANKING_TIMEOUT` |

