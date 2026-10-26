# Design notes

Most of this I wrote myself (wallets, p2p, reversal, seed/reset, errors). The trickier bits — ordered pessimistic locks, idempotency table shape, bank latency/timeout simulation — I used AI for and then rewrote/trimmed until I understood it. Postman collection I started myself; AI cleaned it up (folders, error cases, baseUrl).

## Decisions

- Money: long santim, no doubles.
- available = balance - reserved (reserved unused until payouts).
- Isolation: default Spring/Postgres READ COMMITTED. Correctness comes from SELECT ... FOR UPDATE on wallets, not SERIALIZABLE.
- Idempotency: claim the key with an insert first (status 0 = in progress). Winner does the work then fills status + body. Loser either replays the finished response or gets 409 REQUEST_IN_PROGRESS. Body hash mismatch → 409 IDEMPOTENCY_CONFLICT. Keep 24h.
- Two instances: same jar, PORT=8081/8082, shared Postgres. No in-memory locks for money paths.
- Fake bank: still colocated for now. Will split to :9090 later.

## Q1 — Every wait

Wallet row lock (FOR UPDATE): waits until the other tx commits/rolls back. Bound by Postgres lock_timeout (default none — risk). Hikari borrow: max 2s then fail. Tomcat: up to 50 request threads; beyond that queue. Bank POST sleep 2–6s (or 60s hang) — blocks the request thread today, which is bad for payouts later; for the stub it's intentional. Idempotency find/save: normal DB round trip. No Redis/queue waits.

## Q2 — Database restart mid-payout

Accept → reserve + PENDING in one tx → commit → async bank call. If Postgres dies after reserve, row + reserved are still there after restart. @Scheduled recovery (both instances) finds PENDING, submits if needed, else GETs bank. SUCCEEDED: balance-=amt, reserved-=amt. FAILED: reserved-=amt only. Both finalize paths no-op if status already final (row lock). Bank also POSTs callbacks (sometimes 2–3x same event_id); we store event_id so effect runs once.

## Q3 — What I left out

- Bank as separate process: callbacks, configurable rates, total-paid endpoint.
- E2E S1–S5, B2C (5.6), statement (5.7). Optional APIs I'll either skip or sketch here if time runs out — statement needs stable cursor under concurrent writes; B2C needs per-row independence without locking M001 for 60s.
