PRAGMA foreign_keys=ON;
PRAGMA journal_mode=WAL;
PRAGMA busy_timeout=5000;
CREATE TABLE IF NOT EXISTS schema_migrations (version INTEGER PRIMARY KEY);
INSERT OR IGNORE INTO schema_migrations VALUES(1);
CREATE TABLE IF NOT EXISTS users (
 id INTEGER PRIMARY KEY, role TEXT NOT NULL CHECK(role IN ('customer','provider','admin')),
 name TEXT NOT NULL
);
CREATE TABLE IF NOT EXISTS categories (
 id INTEGER PRIMARY KEY, slug TEXT NOT NULL UNIQUE, name TEXT NOT NULL,
 base_price_minor INTEGER NOT NULL CHECK(base_price_minor>0),
 currency TEXT NOT NULL CHECK(length(currency)=3), enabled INTEGER NOT NULL DEFAULT 1 CHECK(enabled IN (0,1))
);
CREATE TABLE IF NOT EXISTS providers (
 id INTEGER PRIMARY KEY, user_id INTEGER NOT NULL UNIQUE REFERENCES users(id),
 active INTEGER NOT NULL DEFAULT 0 CHECK(active IN (0,1)),
 approved INTEGER NOT NULL DEFAULT 0 CHECK(approved IN (0,1)),
 lat REAL CHECK(lat BETWEEN -90 AND 90), lng REAL CHECK(lng BETWEEN -180 AND 180),
 radius_km REAL NOT NULL DEFAULT 15 CHECK(radius_km>0), last_seen INTEGER
);
CREATE INDEX IF NOT EXISTS providers_geo ON providers(active,approved,lat,lng);
CREATE TABLE IF NOT EXISTS provider_skills (
 provider_id INTEGER NOT NULL REFERENCES providers(id), category_id INTEGER NOT NULL REFERENCES categories(id),
 PRIMARY KEY(provider_id,category_id)
);
CREATE TABLE IF NOT EXISTS jobs (
 id TEXT PRIMARY KEY, customer_id INTEGER NOT NULL REFERENCES users(id),
 category_id INTEGER NOT NULL REFERENCES categories(id), address TEXT NOT NULL, description TEXT NOT NULL,
 lat REAL NOT NULL CHECK(lat BETWEEN -90 AND 90), lng REAL NOT NULL CHECK(lng BETWEEN -180 AND 180),
 status TEXT NOT NULL DEFAULT 'pending_dispatch' CHECK(status IN ('pending_dispatch','dispatched','accepted','in_transit','in_progress','completed','cancelled','expired')),
 provider_id INTEGER REFERENCES providers(id), price_minor INTEGER NOT NULL CHECK(price_minor>0), currency TEXT NOT NULL,
 request_key TEXT NOT NULL, request_hash TEXT NOT NULL, created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL,
 UNIQUE(customer_id,request_key)
);
CREATE INDEX IF NOT EXISTS jobs_customer ON jobs(customer_id,created_at);
CREATE INDEX IF NOT EXISTS jobs_provider ON jobs(provider_id,status);
CREATE TABLE IF NOT EXISTS offers (
 id TEXT PRIMARY KEY, job_id TEXT NOT NULL REFERENCES jobs(id), provider_id INTEGER NOT NULL REFERENCES providers(id),
 status TEXT NOT NULL CHECK(status IN ('pending','accepted','rejected','expired','cancelled')),
 expires_at INTEGER NOT NULL, UNIQUE(job_id,provider_id)
);
CREATE UNIQUE INDEX IF NOT EXISTS one_pending_offer_per_job ON offers(job_id) WHERE status='pending';
CREATE INDEX IF NOT EXISTS offers_due ON offers(status,expires_at);
CREATE TABLE IF NOT EXISTS audit_events (
 id INTEGER PRIMARY KEY, job_id TEXT NOT NULL REFERENCES jobs(id), actor_id INTEGER REFERENCES users(id),
 event TEXT NOT NULL, created_at INTEGER NOT NULL
);
CREATE TABLE IF NOT EXISTS webhook_inbox (
 id INTEGER PRIMARY KEY, source TEXT NOT NULL, external_id TEXT NOT NULL,
 payload TEXT NOT NULL, status TEXT NOT NULL DEFAULT 'pending' CHECK(status IN ('pending','processing','done','failed')),
 attempts INTEGER NOT NULL DEFAULT 0, available_at INTEGER NOT NULL, received_at INTEGER NOT NULL,
 UNIQUE(source,external_id)
);
CREATE TABLE IF NOT EXISTS payments (
 job_id TEXT PRIMARY KEY REFERENCES jobs(id), gateway TEXT NOT NULL, external_id TEXT UNIQUE,
 status TEXT NOT NULL CHECK(status IN ('requires_payment','authorized','captured','transferred','cancelled','refunded','failed')),
 amount_minor INTEGER NOT NULL CHECK(amount_minor>0), currency TEXT NOT NULL, capture_before INTEGER,
 updated_at INTEGER NOT NULL
);
