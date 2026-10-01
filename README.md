# Linkod Global

**Mobile customer demo included:** see [START-HERE.md](START-HERE.md). Run `npm run demo` and open http://127.0.0.1:3000. The customer app now opens with map-based location selection; provider/admin views remain at `/workspace`.


An on-demand service marketplace connecting customers with approved nearby providers.
Initial backend framework, version 0.1.0. Not a finished marketplace.

## Run locally

Requires Node.js 24.19+ in the 24.x line. No third-party dependencies are required for this foundation.
The built-in `node:sqlite` API is experimental in this Node version; it is isolated behind `src/db/`.

```sh
cp .env.example .env
npm run seed
```

Set `DEMO_AUTH=true` in `.env`, then:

```sh
npm start
npm test
```

API: `http://127.0.0.1:3000`. Demo accounts use `Authorization: Bearer demo-customer`,
`demo-provider`, `demo-provider-2`, or `demo-admin`. These identities work only when demo
authentication is explicitly enabled, and are prohibited in production. Seed prices are sample PHP prices.
Provider GPS expires after ten minutes; refresh it through the location endpoint to keep matching.

## Implemented

- SQLite WAL database, constraints, transaction wrapper and initial schema version.
- Server-controlled catalogue pricing, integer minor units and currency snapshots.
- Idempotent booking creation with payload-conflict detection.
- Geographic matching with provider approval, skill, fresh GPS, radius and active-job checks.
- Durable sequential offers, two-minute expiry, rejection and restart recovery.
- Atomic acceptance by the offered provider and legal, ownership-checked job transitions.
- Role-scoped job access, masked offer summaries and append-only application audit history.
- Raw-body Meta signature verification and deduplicated durable lead inbox before acknowledgment.
- Request-size limits, basic single-process rate limiting, generic internal errors and request IDs.
- Payment interface that fails closed, Docker packaging and GitHub Actions tests.

## API

| Method | Path | Access | Purpose |
|---|---|---|---|
| GET | `/health` | Public | Health and version |
| GET | `/api/categories` | Public | Service catalogue |
| POST | `/api/jobs` | Customer | Book with `Idempotency-Key` |
| GET | `/api/jobs` | Signed in | Up to 100 visible jobs |
| GET | `/api/jobs/:id` | Owner, assigned provider, admin | Job details |
| PATCH | `/api/jobs/:id` | Authorized actor | Advance or cancel job |
| PATCH | `/api/providers/me/location` | Provider | GPS and availability |
| GET | `/api/providers/me/offers` | Provider | Current offers |
| POST | `/api/offers/:id/accept` | Offered provider | Accept assignment |
| POST | `/api/offers/:id/reject` | Offered provider | Reject offer |
| GET/POST | `/api/webhooks/meta` | Verified Meta | Subscription or lead inbox |
| GET | `/api/admin/webhook-inbox` | Admin | Inspect ingestion metadata |
| POST | `/api/jobs/:id/payment` | Job access required | Returns 503 until gateway exists |

Create a booking:

```sh
curl http://127.0.0.1:3000/api/jobs \
  -H 'Authorization: Bearer demo-customer' \
  -H 'Idempotency-Key: example-booking-1' \
  -H 'Content-Type: application/json' \
  -d '{"category_id":1,"address":"Demo address, Manila","lat":14.5995,"lng":120.9842}'
```

Refresh provider GPS:

```sh
curl -X PATCH http://127.0.0.1:3000/api/providers/me/location \
  -H 'Authorization: Bearer demo-provider' \
  -H 'Content-Type: application/json' \
  -d '{"lat":14.5995,"lng":120.9842,"active":true}'
```

Job flow: `pending_dispatch → dispatched → accepted → in_transit → in_progress → completed`.
Customer/admin cancellation is allowed before work starts. Exhausted dispatch becomes `expired`.
Service completion is independent of payment capture and payout.

## Boundaries and next steps

A customer/provider/admin demo UI is included in `public/`. Facebook OAuth, Messenger, Socket.io, photo uploads,
gateway integration, admin onboarding, dispute handling and notifications remain to be built.
The HTTP boundary uses Node's native server to make this first commit runnable without network
dependency installation; Express/Socket.io can wrap the services in the next milestone.
The Meta inbox deliberately does not dispatch raw leads: a worker must retrieve lead data, map the
form to a category, collect missing address/consent, geocode and request a confirmed booking first.
Inbox schema includes retries/leases but no processor is wired in this milestone.

Production API routes require an injected verified `authenticate(req)` adapter. The packaged
server has no production identity provider yet and keeps protected routes closed. Do not enable
demo access for a deployment. Durable SQLite needs a single instance and persistent disk;
switch to PostgreSQL before running multiple Cloud Run instances.

See [architecture decisions](docs/architecture.md) and [implementation roadmap](docs/roadmap.md).
