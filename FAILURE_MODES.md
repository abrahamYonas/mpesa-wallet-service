# Failure modes

Wrote these as I went. Most of the wallet/transfer stuff I handled myself. Concurrent idempotency and the bank edge cases I leaned on AI.

## 5.1 P2P transfer

- [x] Two requests debit the same wallet at once → overdraft. Prevented: pessimistic lock on both wallets, ordered by id so no deadlock.
- [x] Debit commits but credit doesn't → money vanishes. Prevented: one @Transactional, both updates in same tx.
- [x] Same Idempotency-Key sent twice with same body → double transfer. Prevented: store key + response, replay. (still weak if both hit before either saves — not fully handled for cross-instance race)
- [x] Same key, different body → silent wrong transfer. Prevented: hash body, 409 IDEMPOTENCY_CONFLICT.
- [x] Transfer from frozen wallet. Prevented: both wallets must be ACTIVE.
- [x] Same wallet both sides creates money loop / nonsense. Prevented: 422 SAME_WALLET.
- [x] Concurrent identical keys on 8081 and 8082 at the same time. Prevented: insert key first (claim). Loser gets replay or 409 REQUEST_IN_PROGRESS.

## 5.2 Reversal

- [x] Reverse twice → money created. Prevented: status REVERSED + 409 ALREADY_REVERSED.
- [x] Reverse while another transfer drains to_wallet → overdraft. Prevented: lock both wallets, check available.
- [x] Reverse into a frozen from_wallet (original sender). Spec says credit is always ok. Allowed: only debit side must be ACTIVE.
- [ ] Concurrent reverse of same transfer on both instances. mostly handled by status check under lock of wallets, but transfer row itself isn't locked for update — could be tighter

## 5.3 Wallet get / freeze

- [x] GET shows negative available. Prevented: available = balance - reserved, never write negative.
- [x] Freeze returns but a transfer still debits after. Prevented: transfer checks ACTIVE under row lock; freeze also locks the row. In-flight either finishes before freeze or fails WALLET_NOT_ACTIVE.
- [ ] Freeze under heavy load taking >2s. not measured / not handled specially

## 5.4 Payouts

- [x] Reserve never released if bank hangs. Prevented: recovery job polls bank and fails/succeeds; reserved released on FAIL, balance+reserved on SUCCESS.
- [x] Double finalize. Prevented: markSucceeded/markFailed only if still PENDING + row lock.
- [ ] Report FAILED but bank still paid. partly handled by polling bank status — callbacks not built yet
- [x] Holding request thread on bank call. Prevented: return 202 first, bank call on @Async after commit.
- [x] Instance killed after reserve. Prevented: PENDING in DB, other instance recovery picks it up.

## 5.5 Bank callbacks — not built yet

- [ ] Duplicate event_id applies twice
- [ ] Callback before submit returns
- [ ] SUCCEEDED after we already marked FAILED

## Fake bank (current stub)

- [x] Same payout_id paid twice. Prevented: idempotent on payout_id in DB.
- [ ] Callbacks / retries / configurable rates. not handled (bank is still inside the app, no callbacks out)

## Infra

- [x] Thread / connection blowup under load (basic). Tomcat max 50, Hikari 10, timeout 2000ms.
- [ ] No Redis etc — anything JVM-local is invisible to the other instance. Knew that going in; locks are DB locks.
