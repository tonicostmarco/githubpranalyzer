# 📊 GitHub PR Analyzer

An event-driven system that ingests GitHub Pull Request webhook events, persists them asynchronously via RabbitMQ, and exposes a REST analytics API secured with JWT authentication.

![Java](https://img.shields.io/badge/Java-25-orange?style=flat-square&logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.0.6-brightgreen?style=flat-square&logo=springboot)
![RabbitMQ](https://img.shields.io/badge/RabbitMQ-3-FF6600?style=flat-square&logo=rabbitmq)
![MongoDB](https://img.shields.io/badge/MongoDB-7-47A248?style=flat-square&logo=mongodb)
![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?style=flat-square&logo=docker)
![JWT](https://img.shields.io/badge/Auth-OAuth2%2FJWT-blueviolet?style=flat-square)
![License](https://img.shields.io/badge/License-MIT-yellow?style=flat-square)

---

## 🧭 Why I Built This

The HelpDesk API handles tickets created by people. This project handles what feeds those tickets automatically — when a pull request is opened, merged, or closed on GitHub, the system captures that event, processes it asynchronously, and makes it available for analysis.

The goal was to move beyond request/response thinking and work with a real messaging pipeline: a webhook receiver that publishes to a Topic Exchange, a consumer that persists to MongoDB, retry logic with exponential backoff, and a Dead Letter Queue for failures that exhaust all attempts.

The analytics layer was built on top of MongoDB aggregation pipelines, which are better suited for flexible time-range queries and grouping across a large volume of events than a relational schema would be.

---

## 🚀 Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 25 |
| Framework | Spring Boot 4.0.6 |
| Messaging | Spring AMQP · RabbitMQ (Topic Exchange, DLQ, retry) |
| Security | Spring Security · OAuth2 Resource Server · JWT · HMAC-SHA256 |
| Persistence | Spring Data MongoDB · MongoTemplate (aggregation pipelines) |
| Infrastructure | Docker Compose (RabbitMQ + MongoDB) |
| Testing | JUnit 6 · Mockito · WebMvcTest · SpringBootTest · Awaitility · Flapdoodle (embedded MongoDB) |
| Coverage | JaCoCo |

---

## 🏛️ Architecture

```
GitHub Webhook
      │
      ▼
POST /webhook/notify
      │  HmacSignatureFilter (HMAC-SHA256 validation)
      ▼
PrEventProducer ──► pr-exchange (TopicExchange)
                         │ routing key: pr.<action>
                         ▼
                    pr-events (queue)
                         │
                         ▼
                  PrEventConsumer
                         │ idempotency check (deliveryId)
                         │ retry: 3x with exponential backoff (1s → 2s → 4s)
                         │ on exhaustion: basicNack requeue=false
                         │         └──► pr-dead-exchange → pr-events-dlq
                         ▼
                      MongoDB
                    (pr_events)
                         │
                         ▼
              GET /analytics/** (JWT required)
```

### Package Structure

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

### Getting a Token

```bash
curl -X POST http://localhost:8080/auth/token \
  -H "Content-Type: application/json" \
  -d '{"username": "admin", "password": "admin123"}'
```

Response:
```json
{
  "token": "<jwt>"
}
```

Use the token in analytics requests:
```
Authorization: Bearer <token>
```

### Webhook Signature Validation

GitHub signs every webhook delivery with HMAC-SHA256. The `HmacSignatureFilter` validates the signature before the request reaches the controller.

```
X-Hub-Signature-256: sha256=<hmac-sha256-of-body>
X-GitHub-Delivery: <delivery-id>
```

If the signature is invalid: `401 Unauthorized`.

---

## 🔌 Endpoints

### Authentication

#### Issue access token

```http
POST /auth/token
```

```json
{
  "username": "admin",
  "password": "admin123"
}
```

---

### Webhook

#### Receive GitHub PR event

```http
POST /webhook/notify
```

**Required headers:**
```
X-GitHub-Delivery: <delivery-id>
X-Hub-Signature-256: sha256=<hmac-sha256-of-body>
Content-Type: application/json
```

**Body:** standard GitHub `pull_request` webhook payload.

Returns `200 OK` after publishing to the exchange. Processing is asynchronous.

---

### Analytics

All analytics endpoints require `Authorization: Bearer <token>`.

**Query params (all endpoints):**

| Param | Type | Example |
|---|---|---|
| `from` | `LocalDateTime` | `2026-01-01T00:00:00` |
| `to` | `LocalDateTime` | `2026-12-31T23:59:59` |

#### Summary

```http
GET /analytics/summary?from=...&to=...
```

```json
{
  "total": 120,
  "merged": 85,
  "opened": 20,
  "closed": 15
}
```

#### Metrics by author

```http
GET /analytics/authormetrics?from=...&to=...
```

```json
[
  {
    "author": "tonicostmarco",
    "total": 42,
    "merged": 30,
    "opened": 8,
    "closed": 4
  }
]
```

#### Metrics by repository

```http
GET /analytics/repositorymetrics?from=...&to=...
```

```json
[
  {
    "repository": "tonicostmarco/github-pr-analyzer",
    "total": 42,
    "merged": 30,
    "opened": 8,
    "closed": 4
  }
]
```

---

## 📨 Messaging

### Topic Exchange routing

The producer publishes to `pr-exchange` with routing key `pr.<action>`. This allows future consumers to subscribe to specific event types without changes to the producer.

| Routing key | Event |
|---|---|
| `pr.opened` | PR opened |
| `pr.closed` | PR closed |
| `pr.merged` | PR merged |
| `pr.*` | All PR events (current consumer) |

### Retry + Dead Letter Queue

Consumer failures trigger automatic retry with exponential backoff before the message is sent to the DLQ:

```
attempt 1 → fail → wait 1s
attempt 2 → fail → wait 2s
attempt 3 → fail → basicNack requeue=false → pr-events-dlq
```

Messages in `pr-events-dlq` are kept for manual inspection or reprocessing.

Configuration in `application.properties`:
```properties
spring.rabbitmq.listener.simple.retry.enabled=true
spring.rabbitmq.listener.simple.retry.max-attempts=3
spring.rabbitmq.listener.simple.retry.initial-interval=1000
spring.rabbitmq.listener.simple.retry.multiplier=2.0
spring.rabbitmq.listener.simple.retry.max-interval=10000
```

### Idempotency

The consumer checks `deliveryId` before saving. GitHub guarantees at-least-once delivery — duplicate events are silently ignored.

---

## ⚠️ Error Handling

| Code | Meaning |
|---|---|
| `400 Bad Request` | Missing required header (`X-GitHub-Delivery`) |
| `401 Unauthorized` | Invalid or missing HMAC signature · Invalid or missing JWT |
| `403 Forbidden` | Authenticated but not authorized |

---

## ⚙️ Environment Variables

| Variable | Default | Description |
|---|---|---|
| `GITHUB_WEBHOOK_SECRET` | `meu-secret-local` | Secret used for HMAC-SHA256 webhook signature validation |

---

## 🛠️ Running Locally

### Prerequisites

| Tool | Minimum Version |
|---|---|
| Java | 25 |
| Maven | 3.9.x |
| Docker + Docker Compose | any recent version |

### Setup

```bash
git clone https://github.com/tonicostmarco/github-pr-analyzer
cd github-pr-analyzer

# Start RabbitMQ + MongoDB
docker compose up -d

# Run application
./mvnw spring-boot:run
```

RabbitMQ management UI: [http://localhost:15672](http://localhost:15672) — `guest` / `guest`

### Simulating a webhook locally

```bash
BODY='{"action":"opened","number":1,"pull_request":{"title":"Fix bug","state":"open","merged":false,"user":{"login":"tonicostmarco"},"created_at":"2026-01-01T00:00:00Z","merged_at":"2026-01-01T01:00:00Z"},"repository":{"full_name":"tonicostmarco/github-pr-analyzer"}}'

SECRET="meu-secret-local"

SIG=$(echo -n "$BODY" | openssl dgst -sha256 -hmac "$SECRET" | awk '{print "sha256=" $2}')

curl -X POST http://localhost:8080/webhook/notify \
  -H "Content-Type: application/json" \
  -H "X-GitHub-Delivery: local-test-001" \
  -H "X-Hub-Signature-256: $SIG" \
  -d "$BODY"
```

---

## 🧪 Tests

```bash
./mvnw test
```

| Test class | Type | What it covers |
|---|---|---|
| `PrEventConsumerIT` | Integration (`@SpringBootTest`) | Consumer saves to MongoDB · idempotency check via `deliveryId` |
| `PrEventConsumerDlqIT` | Integration (`@SpringBootTest`) | Retry exhaustion → message reaches `pr-events-dlq` |
| `PrEventControllerIT` | `@WebMvcTest` | HTTP contract · HMAC validation · missing header returns 400 |
| `GithubpranalyzerApplicationTests` | Context | Application context loads without errors |

Tests use embedded MongoDB (Flapdoodle) and real RabbitMQ via Docker. Async assertions use Awaitility.

**Notable test cases:**

| Test | Type | Validates |
|---|---|---|
| `PrEventConsumerIT.consume` | Integration | Message published to `pr-exchange` is consumed and persisted to MongoDB with correct `deliveryId` |
| `PrEventConsumerDlqIT.shouldSendToDlq_whenConsumerFailsAllRetries` | Integration | After 3 failed attempts with exponential backoff, message lands in `pr-events-dlq` |
| `PrEventControllerIT.shouldReturn200AndCallProducer_whenValidRequest` | WebMvcTest | Valid HMAC signature + valid payload → 200 OK, producer called with correct `deliveryId` |
| `PrEventControllerIT.shouldReturn401_whenInvalidSignature` | WebMvcTest | Invalid HMAC signature → 401 Unauthorized |
| `PrEventControllerIT.shouldReturn400_whenMissingDeliveryHeader` | WebMvcTest | Missing `X-GitHub-Delivery` header → 400 Bad Request |

---

## 🎯 Technical Decisions

| Decision | Reason |
|---|---|
| Topic Exchange over Direct Exchange | Routing key `pr.<action>` allows future consumers to subscribe to specific event types (e.g. only `pr.merged`) without changes to the producer |
| MongoDB over PostgreSQL for events | Aggregation pipelines handle flexible time-range grouping across high event volumes more naturally than SQL; no relational joins needed |
| DLQ over infinite requeue | Failed messages are isolated in `pr-events-dlq` for inspection instead of looping indefinitely and blocking queue throughput |
| Idempotent consumer via `deliveryId` | GitHub guarantees at-least-once delivery. Checking before insert prevents duplicate documents at the application level |
| HMAC-SHA256 filter with replayable wrapper | Body is read once for signature validation, then wrapped in a custom `HttpServletRequestWrapper` with `ByteArrayInputStream` so the controller can still deserialize the payload |
| `Instant` over `OffsetDateTime` in `PrEvent` | MongoDB's BSON driver has no native codec for `OffsetDateTime`. `Instant` is natively supported and stores UTC milliseconds, which is sufficient for event timestamps |

---

## ⚠️ Known Limitations

- JWT keys are generated in memory at startup. Tokens are invalidated after a server restart.
- Analytics endpoints do not paginate results. Large time ranges may return unbounded lists.

---

## 🗺️ Roadmap

- [ ] Persistent JWT key storage (restart-safe tokens)
- [ ] Pagination for analytics endpoints
- [ ] GitHub App integration (replace manual webhook secret with installation tokens)
- [ ] Deploy to AWS EC2
- [ ] Python + LLM — automated PR analysis and data visualization layer consuming the analytics API

---

## 📄 License

This project is licensed under the [MIT License](LICENSE).
