# Centralized Logging System

## Quick Start

### 1. Start Infrastructure
```bash
docker-compose up -d
```

### 2. Verify Services
| Service | URL | Credentials |
|---------|-----|-------------|
| Grafana | http://localhost:3000 | admin / admin |
| Loki    | http://localhost:3100/ready | N/A |

### 3. Start Spring Boot API
```bash
cd api/api
mvn spring-boot:run
```

Logs will automatically flow to Loki.

---

## Architecture

```
┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│   Android    │     │  Spring API  │     │ Admin Panel  │
│   App        │     │  (port 8080) │     │  (port 5173) │
└──────┬───────┘     └──────┬───────┘     └──────┬───────┘
       │                    │                    │
       │ POST /api/logs     │ Loki Appender      │ POST /api/logs
       └────────────────────┼────────────────────┘
                            ▼
               ┌────────────────────────┐
               │   Loki (port 3100)     │
               └────────────┬───────────┘
                            ▼
               ┌────────────────────────┐
               │  Grafana (port 3000)   │
               └────────────────────────┘
```

---

## Grafana Queries (LogQL)

### View All Logs
```logql
{app="civic-connect-api"} | json
```

### Filter by Source
```logql
{app="civic-connect-api"} | json | source="android"
{app="civic-connect-api"} | json | source="admin-panel"
{app="civic-connect-api"} | json | source="api"
```

### Filter by Level
```logql
{app="civic-connect-api", level="ERROR"} | json
{app="civic-connect-api", level="WARN"} | json
```

### Trace by Correlation ID
```logql
{app="civic-connect-api"} | json | correlationId="your-correlation-id-here"
```

### Find Crashes
```logql
{app="civic-connect-api"} | json | message=~".*CRASH.*"
```

### Filter by User
```logql
{app="civic-connect-api"} | json | userId="123"
```

---

## Correlation IDs

Every request gets a unique `X-Correlation-ID` header that traces across:
- Android app requests
- Spring Boot API processing
- React Admin Panel calls

Use the correlation ID in Grafana to see the full request journey.

---

## Key Files

### Spring Boot
| File | Purpose |
|------|---------|
| `logback-spring.xml` | Loki appender config |
| `CorrelationIdFilter.java` | Generates/extracts correlation IDs |
| `RequestLoggingInterceptor.java` | Logs request/response timing |
| `LogIngestionController.java` | Receives logs from Android/React |

### Android
| File | Purpose |
|------|---------|
| `AppLogger.kt` | Main logging interface |
| `RemoteLogService.kt` | Batches and sends logs |
| `CorrelationIdInterceptor.kt` | Adds correlation ID to requests |

### React Admin Panel
| File | Purpose |
|------|---------|
| `loggingService.ts` | Centralized logging |
| `ErrorBoundary.tsx` | Catches React crashes |
| `globalErrorHandler.ts` | Catches JS errors |

---

## Troubleshooting

### No logs in Grafana?
1. Check Loki: `curl http://localhost:3100/ready`
2. Restart Spring Boot to reload logback config
3. Check Docker: `docker-compose logs loki`

### Android logs not appearing?
1. Ensure `AppLogger.initialize()` is called in Application class
2. Check API connectivity to Spring Boot
3. Logs are batched - wait 10 seconds or trigger buffer flush

### React Admin logs not appearing?
1. Check browser console for errors
2. Verify API_URL in loggingService.ts
3. Logs are batched - wait 5 seconds or refresh page

---

## Retention

Logs are retained for **30 days** (configured in `loki-config.yml`).
