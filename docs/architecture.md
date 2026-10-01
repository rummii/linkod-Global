# Architecture decisions and blueprint corrections

## First milestone

Use a modular monolith: HTTP transport → marketplace service → SQLite persistence.
Payment and Meta integration boundaries remain independent of dispatch. Keep all external calls
outside database transactions. Keep project work separate from the existing Maria project.

## Corrections to the supplied blueprint

1. Verify webhook HMAC over raw bytes before parsing. Reject malformed digest lengths safely.
2. Persist inbound events before acknowledging. Deduplicate by the actual `leadgen_id`.
3. Treat a lead as an inquiry, not a fully priced confirmed booking. The original code calculated
   enrichment but ignored it and expected fields Meta does not supply on a lead notification.
4. Facebook Login IDs and Messenger page-scoped IDs are different identities. Do not merge users
   based on an assumed PSID or unverified telephone number. Design explicit account linking.
5. Persist individual offers and deadlines; process expiry after a restart. Restrict acceptance
   to the actual offer recipient, current deadline and eligible provider. A rejected offer must
   not cancel somebody else's dispatch timer.
6. Snapshot catalogue price and currency on the server. A customer never supplies a final price.
7. Track service, authorization, capture, transfer and bank payout separately. Creating a
   PaymentIntent does not authorize money. An authorization must be confirmed and has an expiry.
   A transfer to a connected balance does not prove that money reached a provider's bank.
8. Stripe does not provide escrow accounts. Use accurate authorization/delayed-transfer wording.
   Confirm supported platform/provider countries and currencies before choosing the gateway.
9. Live payment implementation needs operation-level idempotency keys, durable retry records,
   verified webhook reconciliation and refund/transfer-reversal handling. A database transaction
   cannot roll back an external capture or transfer.
10. Add a completion-proof and dispute policy before enabling financial settlement.

## Deployment path

Local proof of concept: Node 24, SQLite WAL, one process, persistent local database.
Single-server pilot: persistent disk, backup/restore checks, TLS ingress, real identity adapter,
notification worker, managed secret storage and configured payment sandbox.
Autoscaling deployment: PostgreSQL/PostGIS, durable task queue, distributed rate limits,
Socket.io Redis adapter or an alternative notification transport. Container-local SQLite is not
a durable multi-instance Cloud Run database.

## References checked when preparing this framework

- https://nodejs.org/download/release/latest-v24.x/docs/api/sqlite.html
- https://docs.stripe.com/payments/place-a-hold-on-a-payment-method
- https://docs.stripe.com/connect/manual-payouts
- https://docs.stripe.com/connect/separate-charges-and-transfers

The initial payment adapter makes no external calls. Provider/country feasibility remains a
business setup decision; the reference links are not a claim that Stripe supports this platform.
