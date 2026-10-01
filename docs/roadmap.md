# Implementation roadmap

## Milestone 1 — Backend foundation (this commit)

Catalogue snapshots, idempotent bookings, provider matching, durable offers, access boundaries,
job transitions, Meta inbox, tests and container/CI scaffolding.

## Milestone 2 — Identity and usable workflows

Verified customer/provider/admin login, provider onboarding and approval, customer booking UI,
provider offers and GPS UI, superadmin operations UI, pricing management, completion evidence,
pagination and an OpenAPI contract. Choose a real identity provider before public deployment.

## Milestone 3 — Meta and notifications

Graph API lead retrieval, form mapping, explicit account linking, address collection and geocoding,
durable worker leases/retries/dead-letter handling, Messenger consent and messaging policy,
Socket.io authentication and rooms, push notifications and a notification outbox.

## Milestone 4 — Payments and accounting

Confirm legal platform location and supported provider markets, choose gateway, implement test-mode
authorization/capture/transfer, verified webhook state reconciliation, idempotent retry recovery,
authorization-expiry policy, accounting ledger, refunds, disputes and payout visibility.
Verify behavior when a capture succeeds but a transfer or local write fails before enabling live money.

## Milestone 5 — Pilot and production operations

Managed database/queue, observability, secrets, backups, retention controls, accessibility checks,
load/race tests, payment sandbox end-to-end tests and a limited provider pilot.

Decisions still needed: initial service city/country, service categories, pricing and commission,
provider approval requirements, proof-of-service policy, payment business entity and bank setup.
Sample Manila coordinates and PHP prices are development fixtures, not approved commercial policy.
