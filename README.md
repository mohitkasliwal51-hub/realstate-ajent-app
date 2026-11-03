# StayFile - Real Estate & Rental Property OS

> **Enterprise-grade multi-tenant platform designed for property owners, PG/co-living managers, and real estate brokerages to streamline listings, tenant KYC, legal rent agreements (e-Stamping & Aadhaar e-Sign), rent collection, and automated financial accounting.**

---

## 🌟 Overview

**StayFile** empowers property owners, PG operators, and brokerages across India to automate operations from lease onboarding to exit settlement:
- **Multi-Tenant RBAC**: Strict workspace isolation with organization-level Row Level Security (RLS).
- **Lease & Rent Agreements**: Automated digital rent agreements with stamp duty calculation and Aadhaar OTP e-Sign integration.
- **Tenant Management & KYC**: Identity verification (Aadhaar/PAN/OVD via Surepass/Digio) and police verification data.
- **Billing & Accounting**: Automated invoice generation, rent collection tracking, split payouts for landlords and brokerages, and PDF receipt rendering.
- **Dual Inventory Engine**: Supports single/shared room PG beds, full apartments, villas, and commercial real estate.

---

## 🏗️ Architecture

```
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                                    STAYFILE ARCHITECTURE                               │
├────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                        │
│     ┌────────────────────────────────────┐            ┌──────────────────────────┐     │
│     │   Next.js 15 (App Router)          │            │  WhatsApp Business API   │     │
│     │   Frontend Portal & Landing Page   │            │  (Meta Cloud API)        │     │
│     └─────────────────┬──────────────────┘            └────────────┬─────────────┘     │
│                       │                                            │                   │
│                       └─────────────────────┬──────────────────────┘                   │
│                                             │ REST API (JSON)                          │
│                                             ▼                                          │
│                       ┌──────────────────────────────────────────┐                     │
│                       │    Java 21 LTS + Spring Boot 3.4 Backend │                     │
│                       │    • Multi-Tenant RBAC Security          │                     │
│                       │    • OpenPDF Invoice & Receipt Engine    │                     │
│                       │    • AES-256 PII Vault & Auditing        │                     │
│                       └─────────────────────┬────────────────────┘                     │
│                                             │                                          │
│          ┌──────────────────────────────────┼──────────────────────────────────┐       │
│          │ JDBC / JPA (RLS Context)         │ Third-Party API Integrations     │       │
│          ▼                                  ▼                                  ▼       │
│  ┌───────────────────────┐       ┌──────────────────────┐             ┌────────────────┐
│  │     PostgreSQL 16     │       │ External Gateways    │             │ Supabase / S3  │
│  │ • 21 Tables & 25 Enums│       ├──────────────────────┤             ├────────────────┤
│  │ • Row-Level Security  │       │ • Surepass / Digio   │             │ • KYC Proofs   │
│  │ • Composite Multi-Org │       │ • Cashfree / Razorpay│             │ • PDF Receipts │
│  └───────────────────────┘       └──────────────────────┘             └────────────────┘
│                                                                                        │
└────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 🚀 Tech Stack

### Backend
- **Framework**: Spring Boot 3.4.11
- **Language**: Java 21 LTS
- **Security**: Spring Security 6 with JWT (HS256/HS384) + AES-GCM-256 for PII encryption
- **Database Access**: Spring Data JPA / Hibernate 6 with PostgreSQL Dialect
- **Document Generation**: OpenPDF 2.0.3

### Frontend
- **Framework**: Next.js 15.1.6 (App Router)
- **Language**: TypeScript 5.7 & React 19
- **Styling**: Tailwind CSS 3.4 & Lucide Icons
- **HTTP Client**: Axios with JWT Interceptor

### Infrastructure & Database
- **Database**: PostgreSQL 16 (Port 5433 for dev, with schema in `database/init-scripts/01-init.sql`)
- **Containerization**: Docker Compose (`docker-compose.yml` for dev, `docker-compose.prod.yml` for prod)

---

## 🛠️ Quick Start

### 1. Prerequisites
- **Java 21 LTS**
- **Node.js 20+** & npm / pnpm
- **PostgreSQL 16** (or Docker)

### 2. Run with Docker Compose
```bash
# Build and run containers
./docker.sh build
./docker.sh up

# Access Services:
# Frontend: http://localhost:3000
# Backend API: http://localhost:8080
# Database: localhost:5433
```

### 3. Run Locally (Development Mode)

#### A. Database
Ensure PostgreSQL is running on port `5433` (or configure via `DB_PORT`) and execute the initial schema:
```bash
psql -U postgres -d stayfile -f database/init-scripts/01-init.sql
```

#### B. Backend
```bash
cd backend
./mvnw spring-boot:run
```

#### C. Frontend
```bash
cd frontend
npm install
npm run dev
```

---

## 📂 Project Structure

```
realstate-ajent-app/
├── backend/                  # Spring Boot 3.4 API backend (Java 21)
│   ├── src/main/java/com/bhartiyasaas/stayfile/
│   │   ├── config/           # Security, Web & Jackson configuration
│   │   ├── controller/       # REST API controllers
│   │   ├── dto/              # Request / Response DTOs
│   │   ├── entity/           # JPA entities & PostgreSQL enums
│   │   ├── repository/       # Spring Data JPA repositories
│   │   ├── security/         # Tenant filter, JWT provider & encryption
│   │   └── service/          # Business logic & PDF generation
│   └── src/main/resources/   # application.properties (dev/prod profiles)
├── frontend/                 # Next.js 15 App Router frontend
│   ├── app/                  # Routes: (auth), (portal), properties, leases, etc.
│   ├── components/           # UI components, modals, sidebar, navbar
│   └── lib/api/              # Axios API clients for backend endpoints
├── database/                 # Database documentation & init scripts
│   ├── DATA_MODEL.md         # Schema design & relational data model
│   └── init-scripts/         # 01-init.sql (21 tables, 25 enums, RLS policies)
├── landing-page/             # Standalone marketing landing page
├── docker-compose.yml        # Multi-container dev orchestration
├── docker-compose.prod.yml   # Production deployment compose spec
└── docker.sh                 # Unified Docker lifecycle management script
```

---

## 🔒 Security & Tenant Isolation

- **Tenant Boundary**: Every request resolves the tenant identity from the JWT and establishes a tenancy context.
- **PII Protection**: Sensitive fields (Aadhaar, PAN, bank account numbers) are encrypted using AES-GCM-256 before database storage.
- **Audit Trails**: Security actions, webhook invocations, and electronic signing flows maintain immutable logs.

---

© 2026 **StayFile Platform** by BhartiyaSaaS Software Labs. All rights reserved.
