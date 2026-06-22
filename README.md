# 📊 GitHub PR Analyzer

An event-driven system that ingests GitHub Pull Request webhook events, persists them asynchronously through RabbitMQ, exposes a JWT-secured analytics API, and layers an LLM service on top to turn raw metrics into natural language insights. The whole stack runs behind an nginx reverse proxy and is deployed on AWS EC2.

![Java](https://img.shields.io/badge/Java-25-orange?style=flat-square&logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.0.6-brightgreen?style=flat-square&logo=springboot)
![Python](https://img.shields.io/badge/Python-3.13-blue?style=flat-square&logo=python)
![FastAPI](https://img.shields.io/badge/FastAPI-009688?style=flat-square&logo=fastapi)
![RabbitMQ](https://img.shields.io/badge/RabbitMQ-3-FF6600?style=flat-square&logo=rabbitmq)
![MongoDB](https://img.shields.io/badge/MongoDB-7-47A248?style=flat-square&logo=mongodb)
![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?style=flat-square&logo=docker)
![nginx](https://img.shields.io/badge/nginx-reverse_proxy-009639?style=flat-square&logo=nginx)
![AWS](https://img.shields.io/badge/AWS-EC2-FF9900?style=flat-square&logo=amazonaws)
![JWT](https://img.shields.io/badge/Auth-OAuth2%2FJWT-blueviolet?style=flat-square)
![License](https://img.shields.io/badge/License-MIT-yellow?style=flat-square)

---

## 🌐 Live Demo

Deployed on an AWS EC2 `t3.small` instance, fronted by nginx:

**Base URL:** `http://pr-analyzer-marco.duckdns.org`

| Path prefix | Routed to | Service |
|---|---|---|
| `/api/` | `githubpranalyzer:8080` | Java API (auth, webhook, analytics) |
| `/insights/` | `github-pr-insights:8000` | Python LLM service |

> The JWT signing keys are generated in memory at startup, so a server restart invalidates any previously issued token. Request a fresh one from `/api/auth/token`.

---

## 🧭 Why I Built This

The HelpDesk API handles tickets created by people. This project handles what feeds those tickets automatically: when a pull request is opened, merged, or closed on GitHub, the system captures the event, processes it asynchronously, and makes it available for analysis.

The goal was to move past request/response thinking and work with a real messaging pipeline: a webhook receiver that publishes to a Topic Exchange, a consumer that persists to MongoDB, retry with exponential backoff, and a Dead Letter Queue for failures that exhaust every attempt.

The analytics layer is built on MongoDB aggregation pipelines, which handle flexible time-range queries and grouping across a large volume of events more naturally than a relational schema would. On top of that, a separate Python service consumes the analytics API and uses an LLM to describe the data in plain language, so the deliberate Java + Python split is part of the design, not an accident.

---

## 🚀 Tech Stack

| Layer | Technology |
|---|---|
| API language | Java 25 |
| API framework | Spring Boot 4.0.6 |
| Messaging | Spring AMQP · RabbitMQ (Topic Exchange, DLQ, retry) |
| Security | Spring Security · OAuth2 Resource Server · JWT (RSA) · HMAC-SHA256 |
| Persistence | Spring Data MongoDB · MongoTemplate (aggregation pipelines) |
| AI service | Python 3.13 · FastAPI · Groq (LLaMA 3.3 70B) |
| Reverse proxy | nginx |
| Infrastructure | Docker Compose · AWS EC2 (`t3.small`) · DuckDNS |
| Testing | JUnit · Mockito · WebMvcTest · SpringBootTest · Awaitility · Flapdoodle (embedded MongoDB) |
| Coverage | JaCoCo |

---

## 🏛️ Architecture

```
                              Internet
                                 │
                                 ▼
                    ┌────────────────────────┐
                    │  nginx (reverse proxy) │  :80
                    │  /api/      → :8080     │
                    │  /insights/ → :8000     │
                    └───────────┬────────────┘
              ┌─────────────────┴──────────────────┐
              ▼                                      ▼
   ┌─────────────────────┐               ┌──────────────────────┐
   │   Java API (8080)   │◄──────────────│  Python svc (8000)   │
   │   Spring Boot 4     │  validate +   │  FastAPI + Groq      │
   └─────────────────────┘  fetch data   └──────────────────────┘

GitHub Webhook
      │  POST /api/webhook/notify
      ▼
HmacSignatureFilter (HMAC-SHA256 validation)
      │
      ▼
PrEventProducer ──► pr-exchange (TopicExchange)
                         │ routing key: pr.<action>
                         ▼
                    pr-events (queue, x-dead-letter-exchange set)
                         │
                         ▼
                  PrEventConsumer
                         │ idempotency check (deliveryId)
                         │ retry: 3x exponential backoff (1s → 2s → 4s)
                         │ on exhaustion → reject (requeue=false)
                         │         └──► pr-dead-exchange → pr-events-dlq
                         ▼
                      MongoDB (pr_events)
                         │
                         ▼
              GET /api/analytics/** (JWT required)
                         │
                         ▼
              github-pr-insights (Python)
              LLM-powered natural language analysis
```

### Package Structure (Java)

```text
controller/
services/
repositories/
entities/
dtos/
  analytics/
  messaging/
config/
```

---

## 🔐 Authentication & Authorization

**Mechanism:** JWT issued via `POST /auth/token`. The Resource Server validates the token on every request to `/analytics/**`. The webhook endpoint is public but protected by HMAC-SHA256 signature validation.

**Token lifetime:** 86400 seconds (24h).

> ⚠️ Keys are generated in memory at startup. Tokens are invalidated after a server restart.

### Getting a token (local)

```bash
curl -X POST http://localhost:8080/auth/token \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```

Response:
```json
{ "token": "<jwt>" }
```

Use it on analytics requests:
```
Authorization: Bearer <token>
```

### Webhook signature validation

GitHub signs every webhook delivery with HMAC-SHA256. `HmacSignatureFilter` validates the signature before the request reaches the controller:

```
X-Hub-Signature-256: sha256=<hmac-sha256-of-body>
X-GitHub-Delivery:   <delivery-id>
```

An invalid or missing signature returns `401 Unauthorized`.

---

## 🔌 Endpoints

In production every Java route is reached under the `/api/` prefix (stripped by nginx before it reaches Spring), and the Python route under `/insights/`. Locally you can hit the services directly on their ports.

### Java API

| Method | Local | Production |
|---|---|---|
| `POST` | `/auth/token` | `/api/auth/token` |
| `POST` | `/webhook/notify` | `/api/webhook/notify` |
| `GET` | `/analytics/summary` | `/api/analytics/summary` |
| `GET` | `/analytics/authormetrics` | `/api/analytics/authormetrics` |
| `GET` | `/analytics/repositorymetrics` | `/api/analytics/repositorymetrics` |

**Analytics query params (all endpoints):**

| Param | Type | Example |
|---|---|---|
| `from` | `LocalDateTime` | `2026-01-01T00:00:00` |
| `to` | `LocalDateTime` | `2026-12-31T23:59:59` |

#### Summary response
```json
{ "total": 120, "merged": 85, "opened": 20, "closed": 15 }
```

#### Author / repository metrics response
```json
[
  { "author": "tonicostmarco", "total": 42, "merged": 30, "opened": 8, "closed": 4 }
]
```

### Python service

| Method | Local | Production |
|---|---|---|
| `GET` | `/insights/` | `/insights/` |

**Query params:** `date_from`, `date_to` (both `LocalDateTime` strings).
**Header:** `Authorization: Bearer <token>`.

```json
{
  "status": "ok",
  "result": "There is 1 repository with 1 pull request in total. It was opened and merged by tonicostmarco."
}
```

---

## 📨 Messaging

### Topic Exchange routing

The producer publishes to `pr-exchange` with routing key `pr.<action>`, so future consumers can subscribe to specific event types without changing the producer.

| Routing key | Event |
|---|---|
| `pr.opened` | PR opened |
| `pr.closed` | PR closed |
| `pr.merged` | PR merged |
| `pr.*` | All PR events (current consumer) |

### Retry + Dead Letter Queue

`pr-events` is declared with `x-dead-letter-exchange = pr-dead-exchange`. Consumer failures trigger automatic retry with exponential backoff; once attempts are exhausted, the message is rejected with `requeue=false` and dead-letters to `pr-events-dlq`:

```
attempt 1 → fail → wait 1s
attempt 2 → fail → wait 2s
attempt 3 → fail → reject (requeue=false) → pr-events-dlq
```

Messages in `pr-events-dlq` are kept for manual inspection or reprocessing.

```properties
spring.rabbitmq.listener.simple.retry.enabled=true
spring.rabbitmq.listener.simple.retry.max-attempts=3
spring.rabbitmq.listener.simple.retry.initial-interval=1000
spring.rabbitmq.listener.simple.retry.multiplier=2.0
spring.rabbitmq.listener.simple.retry.max-interval=10000
```

### Idempotency

The consumer checks `deliveryId` before saving. GitHub guarantees at-least-once delivery, so duplicate events are silently ignored.

---

## ⚠️ Error Handling

| Code | Meaning |
|---|---|
| `400 Bad Request` | Missing required header (`X-GitHub-Delivery`) |
| `401 Unauthorized` | Invalid or missing HMAC signature · Invalid or missing JWT |
| `403 Forbidden` | Authenticated but not authorized |

---

## ⚙️ Environment Variables

The orchestration `docker-compose.yml` reads one secret from a root `.env` file; the rest are set inline in the compose definition.

| Variable | Used by | Default | Description |
|---|---|---|---|
| `GROQ_API_KEY` | Python | none (required) | Groq API key, read from root `.env` |
| `GITHUB_WEBHOOK_SECRET` | Java | `meu-secret-local` | HMAC-SHA256 webhook secret |
| `ANALYTICS_BASE_URL` | Python | `http://githubpranalyzer:8080` | Java API base (set by compose) |
| `ANALYTICS_USERNAME` / `ANALYTICS_PASSWORD` | Python | `admin` / `admin123` | Service credentials (set by compose) |
| `SPRING_RABBITMQ_*` / `MONGODB_HOST` | Java | infra hostnames | Set by compose for the container network |

### `.env.example`

```env
# Required: the Python service will not start an analysis without it
GROQ_API_KEY=your_groq_api_key_here

# Recommended for any non-local deployment (see note below)
# GITHUB_WEBHOOK_SECRET=a-strong-random-secret
```

> Get a Groq key at [console.groq.com](https://console.groq.com) → API Keys.

---

## 🛠️ Running Locally

The entire stack (MongoDB, RabbitMQ, Java API, Python service, nginx) runs from a single Compose file. No separate `mvnw spring-boot:run` step is needed; the Java app is containerized.

### Prerequisites

| Tool | Minimum |
|---|---|
| Docker + Docker Compose | any recent version |
| A Groq API key | for the insights service |

### Setup

```bash
git clone https://github.com/tonicostmarco/githubpranalyzer
cd githubpranalyzer

# create the root .env with your Groq key
cp .env.example .env
# edit .env and set GROQ_API_KEY

docker compose up -d
```

Compose brings the services up in dependency order (see the healthcheck section below). Once everything is healthy:

| Service | Direct | Through nginx |
|---|---|---|
| Java API | `http://localhost:8080` | `http://localhost/api/` |
| Python service | `http://localhost:8000` | `http://localhost/insights/` |
| RabbitMQ management UI | `http://localhost:15672` (`guest` / `guest`) | — |

---

## 🧪 Example Requests

The examples below target the deployed URL. Swap `http://pr-analyzer-marco.duckdns.org/api` for `http://localhost:8080` to run them locally (and drop `/api` since nginx is the one adding it).

### 1. Issue a token

```bash
curl -X POST http://pr-analyzer-marco.duckdns.org/api/auth/token \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```

### 2. Send a signed webhook

The signature must be computed over the **exact** bytes of the request body. `printf '%s'` avoids the trailing newline that `echo` would add and that would break the HMAC.

```bash
BODY='{"action":"opened","number":1,"pull_request":{"title":"Fix bug","state":"open","merged":false,"user":{"login":"tonicostmarco"},"created_at":"2026-01-01T00:00:00Z","merged_at":"2026-01-01T01:00:00Z"},"repository":{"full_name":"tonicostmarco/github-pr-analyzer"}}'

# must match the deployed GITHUB_WEBHOOK_SECRET
SECRET="meu-secret-local"

SIG=$(printf '%s' "$BODY" | openssl dgst -sha256 -hmac "$SECRET" | awk '{print $2}')

curl -X POST http://pr-analyzer-marco.duckdns.org/api/webhook/notify \
  -H "Content-Type: application/json" \
  -H "X-GitHub-Delivery: local-test-001" \
  -H "X-Hub-Signature-256: sha256=$SIG" \
  -d "$BODY"
```

### 3. Query analytics (token required)

```bash
TOKEN=$(curl -s -X POST http://pr-analyzer-marco.duckdns.org/api/auth/token \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' | jq -r '.token')

curl "http://pr-analyzer-marco.duckdns.org/api/analytics/summary?from=2026-01-01T00:00:00&to=2026-12-31T23:59:59" \
  -H "Authorization: Bearer $TOKEN"
```

> No `jq`? Replace the extraction with: `| sed 's/.*"token":"\([^"]*\)".*/\1/'`

### 4. Generate an insight (token required)

```bash
curl "http://pr-analyzer-marco.duckdns.org/insights/?date_from=2026-01-01T00:00:00&date_to=2026-12-31T23:59:59" \
  -H "Authorization: Bearer $TOKEN"
```

---

## 🧪 Tests

```bash
./mvnw test
```

| Test class | Type | What it covers |
|---|---|---|
| `PrEventConsumerIT` | Integration (`@SpringBootTest`) | Consumer saves to MongoDB · idempotency via `deliveryId` |
| `PrEventConsumerDlqIT` | Integration (`@SpringBootTest`) | Retry exhaustion → message reaches `pr-events-dlq` |
| `PrEventControllerIT` | `@WebMvcTest` | HTTP contract · HMAC validation · missing header returns 400 |
| `GithubpranalyzerApplicationTests` | Context | Application context loads under the `test` profile |

Tests use embedded MongoDB (Flapdoodle) and a real RabbitMQ via Docker. Async assertions use Awaitility.

---

## 🎯 Technical Decisions

| Decision | Reason |
|---|---|
| Topic Exchange over Direct Exchange | Routing key `pr.<action>` lets future consumers subscribe to specific event types without touching the producer |
| MongoDB over PostgreSQL for events | Aggregation pipelines handle flexible time-range grouping across high event volumes more naturally than SQL joins |
| DLQ over infinite requeue | Failed messages are isolated in `pr-events-dlq` for inspection instead of looping and blocking throughput |
| Idempotent consumer via `deliveryId` | GitHub delivers at-least-once; checking before insert prevents duplicate documents |
| HMAC filter with replayable wrapper | The body is read once for signature validation, then wrapped in a `ByteArrayInputStream`-backed request so the controller can still deserialize it |
| `Instant` over `OffsetDateTime` in `PrEvent` | MongoDB's BSON driver has no native codec for `OffsetDateTime`; `Instant` stores UTC milliseconds, which is enough for event timestamps |
| nginx reverse proxy in front of both services | A single public entrypoint and port; path-based routing (`/api`, `/insights`) keeps the two services behind one host without exposing their internal ports |
| Auth-endpoint healthcheck for the Java container | Hitting `/auth/token` proves the app is up, the JWT encoder is wired, and security is functional, not just that a port is open |

---

## ⚠️ Known Limitations

- JWT keys are generated in memory at startup, so tokens are invalidated after a restart.
- Analytics endpoints do not paginate; large time ranges may return unbounded lists.
- Demo credentials (`admin` / `admin123`) and the default webhook secret are intended for a portfolio demo, not production.

---

## 🗺️ Roadmap

- [x] Python + LLM analysis layer via [github-pr-insights](https://github.com/tonicostmarco/github-pr-insights)
- [x] Deploy to AWS EC2 (nginx reverse proxy, DuckDNS)
- [ ] Persistent JWT key storage (restart-safe tokens)
- [ ] Pagination for analytics endpoints
- [ ] Inject the webhook secret via environment in the deployed compose
- [ ] GitHub App integration (replace the manual webhook secret with installation tokens)

---

## 🐍 AI Analytics Layer

[github-pr-insights](https://github.com/tonicostmarco/github-pr-insights) is a companion Python service that consumes this API and uses an LLM (Groq, LLaMA 3.3 70B) to generate natural language insights about pull request data.

**Tech stack:** Python 3.13 · FastAPI · Groq · requests · python-dotenv

**Flow:**

1. The client authenticates via `/auth/token` and receives a JWT.
2. The client calls `/insights/` with the JWT in the `Authorization` header.
3. The Python service validates the token against the Java API. If validation fails, the analysis is denied.
4. With a valid token, the service fetches summary, author, and repository metrics.
5. The data is sent to Groq, which returns a natural language summary.
6. The insight is returned to the client.

Full setup and usage in the [github-pr-insights README](https://github.com/tonicostmarco/github-pr-insights).

---

## 📄 License

Licensed under the [MIT License](LICENSE).
