# Mini Bank

[![ci](https://github.com/azmain2704-ship-it/mini-bank/actions/workflows/ci.yml/badge.svg)](https://github.com/azmain2704-ship-it/mini-bank/actions/workflows/ci.yml)

The behind-the-scenes part of a bank, with no screens: a REST API that opens accounts, moves money between them and keeps a full history. I wanted to build the bit of a bank that has to be right, so most of the work went into the rules around a transfer, not the endpoints themselves.

Java 21, Spring Boot, PostgreSQL, Flyway and Docker. Tested with JUnit, and with Postman by hand.

**Try it live: [mini-bank-4x45.onrender.com/swagger-ui.html](https://mini-bank-4x45.onrender.com/swagger-ui.html)** (running on Render's free tier with a Neon PostgreSQL database, so the first request after a quiet spell takes about a minute while the server wakes up).

## What it does

- **Accounts** with a balance. You can open one with a starting balance and pay money in.
- **Transfers** from one account to another.
- **Rules:** you can't send more than you have, and you can't send zero, a negative amount, fractions of a penny or money to the same account.
- **History:** every transfer gets a row in the history table, including refused ones. A refused transfer is still something that happened, and a bank would want a record of it.
- **Fraud flags:** a transfer of £5,000 or more, or a 5th transfer out of the same account inside a minute, gets flagged for review. Flags don't block the transfer, the same way a real bank lets the payment through and has someone look at it later. The limits live in `application.yml`.
- **Idempotency keys:** if a client sends the same transfer twice with the same `Idempotency-Key` header (say the connection dropped and it retried), the money only moves once and the second call gets the original result back. If the same key comes back with a different amount or account, that's almost certainly a bug in the client, so it gets a 422 and nothing moves.

## The parts I had to think hardest about

**Two transfers at the same moment.** If two requests both read "balance £100" and both send £100, the account ends up at -£100. Each transfer locks both account rows (`SELECT ... FOR UPDATE`) before checking the balance, so the second request waits until the first has finished. There's a test that fires 20 £10 transfers at a £100 account at the same instant: exactly 10 go through and the balance lands on £0.

**Deadlocks.** If transfer 1 goes A→B and locks A, while transfer 2 goes B→A and locks B, each waits for the other forever. So the code always locks the lower account id first, whichever direction the money is going. Another test sends 40 transfers both ways at once and checks they all finish and no money appears or disappears.

**Money as whole pence.** Balances are stored as `long` pence and never as doubles (in floating point, `0.1 + 0.2` isn't `0.3`). The API takes and returns pounds like `12.50`. Anything with more than 2 decimal places is refused rather than rounded.

**All or nothing.** Taking money from one account, adding it to the other and writing the history row happen in a single database transaction. If anything fails halfway, none of it is saved.

**The database checks too.** The tables have `CHECK` constraints (balance >= 0, amount > 0, from ≠ to). Even if the Java code had a bug, PostgreSQL would refuse to save an impossible state.

## API

| Method | Path | What it does |
|---|---|---|
| POST | `/api/accounts` | Open an account: `{"ownerName": "Alice", "openingBalance": 100.00}` |
| GET | `/api/accounts` / `/api/accounts/{id}` | List accounts / get one |
| POST | `/api/accounts/{id}/deposits` | Pay money in: `{"amount": 25.00}` |
| POST | `/api/transfers` | Move money: `{"fromAccountId": 1, "toAccountId": 2, "amount": 12.50}` (optional `Idempotency-Key` header) |
| GET | `/api/transfers?accountId=1` | History, newest first |
| GET | `/api/accounts/{id}/transfers` | One account's history |
| GET | `/api/fraud-flags` | Everything that's been flagged |

Transfers return **201** when the money moved, **422** when a rule refused it (still saved in the history) or an `Idempotency-Key` was reused for a different transfer (not saved), **400** for bad input and **404** for an unknown account. Errors come back as standard [problem details](https://www.rfc-editor.org/rfc/rfc7807) JSON with a readable `detail`.

Full interactive docs (Swagger UI) are at `/swagger-ui.html` when it's running.

## Running it

**With Docker** (app + PostgreSQL):

```bash
docker compose up --build
```

**Without installing anything** (Windows): double-click `run_bank.bat`. If you don't have Java 21, it downloads a copy for this project. It runs the bank with a small built-in H2 database and opens Swagger UI in the browser.

**With Java 21 installed:**

```bash
mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=local
```

(`mvnw.cmd` downloads Maven the first time. If you already have Maven, plain `mvn` works too.)

To try it in Postman, import `postman/mini-bank.postman_collection.json` and run the requests in order. They open two accounts, make a transfer, try an overdraft, send a negative amount and set off a fraud flag, and each one checks its own result.

## Tests

`mvn verify` (or `mvnw.cmd verify`) runs 26 tests:

- **BankApiTest** goes through the real API and database for every rule: overdrafts, zero, negative and sub-penny amounts, same account, unknown accounts, history including refused transfers, both fraud rules (with the edge cases just under the limits), idempotent retries and a key reused for a different transfer.
- **ConcurrencyTest** covers the simultaneous-transfer and deadlock cases above.
- **MoneyTest** covers the pounds/pence conversion.

GitHub Actions runs all of them on every push, then builds the Docker image, starts it with a real PostgreSQL database and runs `scripts/smoke-test.sh` against it.

## How the code is laid out

```
src/main/java/com/minibank/
  account/   Account entity, repository (with the row lock), service, controller
  transfer/  Transfer entity, the transfer logic, history
  fraud/     The two fraud rules and the flags endpoint
  common/    Money conversion and error handling
src/main/resources/db/migration/   Flyway SQL that creates the tables
```

## What I'd add next

Login, so people can only move money out of their own accounts. Currencies other than pounds. A proper double-entry ledger, where every transfer writes a debit row and a credit row and balances are worked out from those. And a way for someone to mark a fraud flag as reviewed.
