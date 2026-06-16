# Wallet & Ledger API

A Spring Boot service for digital wallets with deposits and **atomic, concurrency-safe
internal transfers**. The focus is strict financial data integrity: every balance change
is transactional, race conditions are prevented with pessimistic locking, and all money
movements are recorded in an immutable double-entry ledger.

---

## Features

- Create wallets (per user, single currency) with an initial balance of `0.00`
- Deposit / top-up
- Internal account-to-account transfers — atomic and race-safe
- Transaction history — newest-first, optionally filtered by type
- Standardized error responses (`{ "errorCode", "message" }`)
- Interactive API docs (Swagger UI)

## Tech Stack

Java 21 · Spring Boot 3.5 · Spring Web · Spring Data JPA · H2 (in-memory) ·
Bean Validation · Lombok · JUnit 5 · Mockito · Maven (wrapper) · Docker.

All monetary values use `BigDecimal` (precision 19, scale 2) — never floating point.

---

## Architecture

Strictly layered, with DTOs at the boundary so JPA entities never leave the service layer:

```
Controller  ->  Service  ->  Repository  ->  Entity (H2)
```

There is intentionally **no facade layer** — in a domain this focused it would only add
pass-through indirection.

### Package layout

```
com.hesap.wallet
├── controller/                 REST endpoints (AccountController, TransferController)
├── service/                    AccountService, DepositService, TransactionHistoryService
│   └── transfer/               AbstractTransferService (template), InternalTransferService
│       └── validation/         TransferValidationStrategy + validators
├── repository/                 AccountRepository, TransactionRepository
├── entity/                     Account, Transaction
├── enums/                      Currency, TransactionType, TransactionStatus
├── dto/request|response/       request bodies & API views
├── factory/                    TransactionFactory
└── exception/                  domain exceptions + GlobalExceptionHandler
```

### Design patterns (GoF)

| Pattern | Type | Where | Purpose |
|---|---|---|---|
| **Factory** | Creational | `TransactionFactory` | Centralizes construction of ledger entries (`DEPOSIT`, linked `TRANSFER_OUT`/`TRANSFER_IN`) — one place owns type, status, balance snapshot, and linkage. |
| **Template Method** | Behavioral | `AbstractTransferService` | Defines the immutable transfer sequence; subclasses fill step hooks but can never reorder them. |
| **Strategy** | Behavioral | `TransferValidationStrategy` (+ validators) | Pluggable validation rules injected as a list. New rules (fees, limits, cross-currency policy) are added as new beans with no engine changes (Open/Closed Principle). |

---

## Data Integrity & Concurrency

The case study's central concern is correct behavior when multiple deposits/transfers hit
the same account at once. This is handled with **pessimistic write locking** plus
transactional atomicity.

### Pessimistic locking

`AccountRepository.findByIdForUpdate` is annotated with `@Lock(LockModeType.PESSIMISTIC_WRITE)`,
which makes Hibernate issue `SELECT ... FOR UPDATE`:

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("select a from Account a where a.id = :id")
Optional<Account> findByIdForUpdate(@Param("id") Long id);
```

- The lock is a **row-level write lock held until the surrounding transaction commits or
  rolls back**. A second transaction trying to lock the same account blocks until the first
  finishes, so concurrent balance mutations are **serialized** — eliminating lost updates.
- **Both deposits and transfers** load accounts through this method.

### Transfer engine ordering (Template Method)

`AbstractTransferService.transfer(...)` runs a fixed, single-transaction sequence:

1. **Validate** — stateless request checks (positive amount, distinct accounts).
2. **Acquire locks** — lock both accounts via `findByIdForUpdate`, **in ascending account-id
   order**. Consistent ordering means concurrent `A → B` and `B → A` transfers can never
   deadlock.
3. **Update balances** — the **sufficient-funds and currency checks run here, under the
   lock**, immediately before the debit/credit. The funds check and the debit are therefore
   a single race-free unit (no time-of-check/time-of-use gap).
4. **Write ledger** — the linked `TRANSFER_OUT` / `TRANSFER_IN` entries (shared reference).

### Atomicity

The whole sequence is `@Transactional`, so **any** failure rolls back every balance change
*and* every ledger entry — the system is never left in a partial state.

### Proven by tests

- `DepositConcurrencyTest` — 400 concurrent deposits sum exactly (no lost updates).
- `TransferConcurrencyTest` — 50 racing transfers against a limited balance: exactly the
  fundable number succeed, the account never goes negative, total money is conserved; and
  simultaneous bidirectional transfers complete without deadlock.
- `TransferAtomicityTest` — a forced mid-transfer failure rolls back both balances and the
  already-written ledger leg.

---

## Getting Started

### Option A — Docker (no local JDK/Maven required)

```bash
docker compose up --build
```

The API starts on **http://localhost:8080**. Stop with `Ctrl+C` (or `docker compose down`).

### Option B — Local (requires JDK 21)

```bash
./mvnw spring-boot:run
```

> The Maven wrapper (`./mvnw`) downloads the correct Maven version automatically; only a
> JDK 21 is required. If your default `java` is a different version, point `JAVA_HOME` at a
> JDK 21 before running.

### Build & test

```bash
./mvnw clean install      # compiles, runs all tests, packages the jar
```

---

## API Reference

Base path: `/api/v1`. All request/response bodies are JSON in **snake_case**.

### Create account
```bash
curl -X POST http://localhost:8080/api/v1/accounts \
  -H "Content-Type: application/json" \
  -d '{"user_id": 42, "currency": "TRY"}'
```
`201 Created`
```json
{ "account_id": 1, "user_id": 42, "currency": "TRY", "balance": 0.00, "created_at": "2026-06-16T20:22:25.270681Z" }
```

### Deposit
```bash
curl -X POST http://localhost:8080/api/v1/accounts/1/deposit \
  -H "Content-Type: application/json" \
  -d '{"amount": 50.00}'
```
`200 OK`
```json
{ "transaction_id": 1, "account_id": 1, "new_balance": 50.00, "status": "SUCCESS" }
```

### Transfer
```bash
curl -X POST http://localhost:8080/api/v1/transfers \
  -H "Content-Type: application/json" \
  -d '{"sender_account_id": 1, "receiver_account_id": 2, "amount": 30.00}'
```
`201 Created`
```json
{ "transaction_id": 3, "status": "SUCCESS", "timestamp": "2026-06-16T20:25:00.000000Z" }
```

### Transaction history
```bash
# all transactions, newest first
curl http://localhost:8080/api/v1/accounts/1/transactions

# filter by type: DEPOSIT | TRANSFER_IN | TRANSFER_OUT
curl "http://localhost:8080/api/v1/accounts/1/transactions?transaction_type=DEPOSIT"
```
`200 OK`
```json
[
  { "transaction_id": 3, "type": "TRANSFER_OUT", "amount": 30.00, "balance_after": 120.00, "status": "SUCCESS", "related_account_id": 2, "created_at": "..." },
  { "transaction_id": 2, "type": "DEPOSIT", "amount": 50.00, "balance_after": 150.00, "status": "SUCCESS", "created_at": "..." }
]
```
(`related_account_id` is present only for transfer legs.)

### Error model

Every error returns the same envelope:
```json
{ "errorCode": "INSUFFICIENT_BALANCE", "message": "Insufficient balance in account: 1" }
```

| errorCode | HTTP | When |
|---|---|---|
| `ACCOUNT_NOT_FOUND` | 404 | Unknown account id |
| `INSUFFICIENT_BALANCE` | 422 | Sender cannot cover the transfer |
| `INVALID_TRANSFER` | 400 | Same sender/receiver, currency mismatch, non-positive amount |
| `VALIDATION_ERROR` | 400 | Request body fails bean validation |
| `INVALID_PARAMETER` | 400 | Bad path/query value (e.g. unknown `transaction_type`) |
| `MALFORMED_REQUEST` | 400 | Missing/unparseable body (e.g. unknown currency) |
| `INTERNAL_ERROR` | 500 | Unexpected server error |

---

## API Docs & Database Console

- **Swagger UI:** http://localhost:8080/swagger-ui.html
- **OpenAPI JSON:** http://localhost:8080/v3/api-docs
- **H2 console:** http://localhost:8080/h2-console — JDBC URL `jdbc:h2:mem:walletdb`, user `sa`, no password.

> H2 is in-memory; data resets on restart.

---

## Testing

`./mvnw clean install` runs the full suite: service unit tests (Mockito), the validation
strategies, web-layer error mapping (`@WebMvcTest` + MockMvc), and the integration tests
covering concurrency, atomicity, and history ordering/filtering against a real H2 instance.
