# StayFile Backend API

RESTful API backend for **StayFile** - Multi-Tenant Real Estate Broker & PG Management Platform built with Spring Boot 3.4.11, Java 21 LTS, and PostgreSQL.

---

## 🚀 Prerequisites

- **Java 21 LTS**
- **PostgreSQL 16** (or Docker)
- **Maven 3.9+** (or included `./mvnw`)

---

## 🗄️ Database Setup

1. **Create the PostgreSQL database:**
   ```bash
   psql -U postgres -c "CREATE DATABASE stayfile;"
   ```

2. **Initialize Schema & RLS Policies:**
   ```bash
   psql -U postgres -d stayfile -f ../database/init-scripts/01-init.sql
   ```

---

## ⚙️ Configuration

Application configuration files are located under `src/main/resources/`:
- `application.properties`: Common baseline configurations (server port, Jackson formatting, actuator).
- `application-dev.properties`: Development settings with safe fallback secrets for local work.
- `application-prod.properties`: Production profile requiring environment-injected secrets.

### Key Environment Variables
| Variable | Description | Dev Default |
|---|---|---|
| `SERVER_PORT` | HTTP port | `8080` |
| `DB_HOST` | Database host | `localhost` |
| `DB_PORT` | Database port | `5433` |
| `DB_NAME` | Database name | `stayfile` |
| `DB_USERNAME` | PostgreSQL username | `postgres` |
| `DB_PASSWORD` | PostgreSQL password | `postgres` |
| `JWT_SECRET` | Base64 secret for JWT HMAC signing | Dev fallback provided |
| `PII_SECRET_KEY` | AES-256 Base64 key for encrypting PII | Dev fallback provided |

---

## 🏃 Running the Application

### Using Maven Wrapper
```bash
# Run with dev profile
./mvnw spring-boot:run

# Run tests
./mvnw test

# Package JAR
./mvnw clean package -DskipTests
```

---

## 🛡️ Security & Architecture

- **Package Root**: `com.bhartiyasaas.stayfile`
- **Authentication**: JWT Bearer token authentication via `JwtAuthenticationFilter`.
- **Multi-Tenancy**: `TenantContextFilter` injects `X-Organization-Id` into `SecurityContext` and sets PostgreSQL RLS context (`SET LOCAL app.current_organization_id`).
- **PII Protection**: `PiiEncryptionService` secures sensitive identities and bank details with AES-GCM-256.
- **PDF Generation**: `InvoiceServiceImpl` & `ReceiptServiceImpl` render downloadable PDF documents with OpenPDF.

---

© 2026 **StayFile Platform** by BhartiyaSaaS Software Labs.
