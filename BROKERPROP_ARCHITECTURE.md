# 🏡 BrokerProp - Real Estate Broker & PG Management Platform

> **A custom white-label web platform empowering real estate brokers, property owners, and PG managers to showcase properties, generate legal rent agreements, automate rent collection, issue receipts, and manage tenant intimation.**

---

## 🌟 Executive Summary

**BrokerProp** is an enterprise-grade multi-tenant SaaS platform designed specifically for the Indian real estate market. It bridges the gap between property showcase websites, WhatsApp conversational commerce, legal compliance (e-Stamping & Aadhaar e-Sign), utility bill payments (BBPS), and automated financial management for PG co-living spaces and full apartment rentals.

---

## 🏗️ System Architecture

```
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                                   BROKERPROP ARCHITECTURE                              │
├────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                        │
│     ┌────────────────────────────────────┐            ┌──────────────────────────┐     │
│     │   Next.js 14 (App Router)          │            │  WhatsApp Business Bot   │     │
│     │   White-Label Owner Portal         │            │  (Meta Cloud API/Interakt)│     │
│     └─────────────────┬──────────────────┘            └────────────┬─────────────┘     │
│                       │                                            │                   │
│                       └─────────────────────┬──────────────────────┘                   │
│                                             │ REST API (JSON)                          │
│                                             ▼                                          │
│                       ┌──────────────────────────────────────────┐                     │
│                       │    Java 21 LTS + Spring Boot 3 Backend   │                     │
│                       │    • Multi-Tenant RBAC Security          │                     │
│                       │    • Server-Side OpenPDF Engine          │                     │
│                       │    • Webhook Listener & Integration Log  │                     │
│                       └─────────────────────┬────────────────────┘                     │
│                                             │                                          │
│          ┌──────────────────────────────────┼──────────────────────────────────┐       │
│          │ JDBC / JPA                       │ REST API Clients                 │       │
│          ▼                                  ▼                                  ▼       │
│  ┌───────────────┐               ┌──────────────────────┐             ┌────────────────┐
│  │ PostgreSQL DB │               │   Third-Party APIs   │             │Supabase Storage│
│  │ (Supabase/RDS)│               ├──────────────────────┤             ├────────────────┤
│  │ • 16 Tables   │               │ • Surepass (KYC)     │             │ • ID Proofs    │
│  │ • RLS Security│               │ • Digio (e-Sign)     │             │ • Agreements   │
│  │ • Triggers    │               │ • Razorpay (Payments)│             │ • PDF Receipts │
│  │ • Org Indexes │               │ • Decentro (BBPS)    │             │ • Property Img │
│  └───────────────┘               └──────────────────────┘             └────────────────┘
│                                                                                        │
└────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## ✨ Key Platform Modules & Features

### 1. 🏢 White-Label Website & RBAC Portal
- **Custom Subdomains / Domains**: Each broker/owner receives their own branded portal (e.g. `sunshinepg.brokerprop.com`).
- **Role-Based Access Control (RBAC)**:
  - 👑 `SUPER_ADMIN`: Platform owner
  - 🏠 `OWNER_ADMIN`: Property Owner / Agency Admin
  - 🧑‍💼 `PROPERTY_MANAGER`: Site/Branch Manager
  - 🛠️ `STAFF_ASSISTANT`: Front-desk & Maintenance Staff
  - 👤 `TENANT`: PG Resident / Flat Rentee
  - 👁️ `PUBLIC_GUEST`: Showcase visitor
- **Company Legal Branding**: Logo, primary theme color, GSTIN, RERA registration, PAN, registered office address, and owner digital signature upload.

### 2. 🏠 Dual Inventory Management (PG Beds vs. Full Flats)
- **PG / Co-Living Hierarchy**: Building ➔ Floor ➔ Room ➔ Bed Slot (e.g., Room 101 - Bed A).
- **Full Flat Rentals**: Whole apartment units (2BHK, 3BHK, Villa).
- **Automated Occupancy Sync**: PostgreSQL triggers automatically switch unit statuses (`AVAILABLE` ➔ `OCCUPIED`) when a lease becomes `ACTIVE`.

### 3. 🪪 Identity Verification & KYC Vault
- **Aadhaar OKYC & OTP API**: Surepass / Digio integration for instant identity verification.
- **PAN Verification API**: Verifies tenant PAN against Income Tax database.
- **Compliance Storage**: Unencrypted `id_proof_last4` for UI display with AES-256 encrypted full document ID storage.

### 4. 📜 Legal Digital Rent Agreement (E-Stamping & E-Sign)
- **State E-Stamp Procurement**: Auto-procures state e-Stamp papers (₹100/₹500 value).
- **Aadhaar OTP E-Sign**: Both Landlord and Tenant e-sign via Aadhaar OTP (legally binding under IT Act, 2000).
- **Multi-Template Support**: Supports 11-Month PG agreements, Residential Leave & License, and Commercial leases.

### 5. 💳 Financial Management, Receipts & BBPS Utilities
- **0% UPI Rent Collection**: Razorpay / Cashfree 1-click online payment links with direct owner bank payouts.
- **Org-Scoped Receipts**: Auto-generated payment receipts (e.g., `REC-2026-000001`) scoped per organization.
- **BBPS Utility Payments**: Direct electricity & water bill fetching and payment via Bharat BillPay (Decentro).

### 6. 📱 WhatsApp Business & CRM Lead Funnel
- **Delivery Audit Logs**: Every outbound/inbound WhatsApp message, agreement PDF delivery, and receipt notification logged in `whatsapp_logs`.
- **CRM Lead Pipeline**: Tracks prospective tenant enquiries (`NEW`, `CONTACTED`, `VISITED`, `CONVERTED`, `LOST`) captured via WhatsApp, Website, 99acres, or NoBroker.

---

## 🛠️ Finalized Tech Stack & Tools

| Component | Technology | Rationale / Purpose |
| :--- | :--- | :--- |
| **Backend Engine** | **Java 21 LTS + Spring Boot 3+** | Virtual Threads for high-concurrency webhooks, strong typing, financial transactions & audit logs |
| **Database** | **PostgreSQL (Supabase / AWS RDS)** | 16 Normalized Tables, Row Level Security (RLS), Automated Triggers, Org Composite Indexes |
| **Frontend** | **Next.js 14 (App Router) + Tailwind CSS** | Server-side rendering, fast white-label portals, mobile-responsive dashboard |
| **PDF Generation** | **Server-side OpenPDF / Java Engine** | Tamper-proof server timestamped PDFs for court proof (`@react-pdf` for frontend preview) |
| **File Storage** | **Supabase Storage Buckets** | S3-compatible object storage with RLS policies for tenant document privacy |
| **Location Services**| **Leaflet.js + OpenStreetMap** | 100% Free interactive property map location picker |

---

## 🔌 Finalized Third-Party APIs

- 🪪 **Surepass / Digio**: Aadhaar OKYC & PAN Verification API
- 📜 **Digio / Leegality**: State E-Stamping & Aadhaar OTP E-Sign API
- 💳 **Razorpay / Cashfree**: Rent & Deposit Payment Gateway (0% UPI fee)
- ⚡ **Decentro**: BBPS Utility Bill Payment API (Electricity & Water)
- 📱 **Meta WhatsApp Cloud API**: WhatsApp Business Conversational Commerce (via Interakt)

---

## 📂 Database & Data Model Files

The database architecture is 100% frozen and documented:

1. 📜 **[`database/schema.sql`](file:///c:/Users/Wissen/Desktop/tech%20stack%20training/realstate-ajent-app/database/schema.sql)**  
   *Single standalone master PostgreSQL initialization script containing all 16 tables, enums, triggers, composite indexes, and RLS policies.*

2. 📘 **[`database/DATA_MODEL.md`](file:///c:/Users/Wissen/Desktop/tech%20stack%20training/realstate-ajent-app/database/DATA_MODEL.md)**  
   *Comprehensive technical reference documentation detailing all table schemas, field attributes, constraints, and realistic sample data.*

---

## 🚀 Database Quick Start

To initialize the PostgreSQL / Supabase database in 1 click:

1. Open Supabase **SQL Editor** or your PostgreSQL client (DBeaver / pgAdmin / psql).
2. Copy the contents of [`database/schema.sql`](file:///c:/Users/Wissen/Desktop/tech%20stack%20training/realstate-ajent-app/database/schema.sql).
3. Execute the script.

All 16 tables, custom types, automated unit-lease status triggers, receipt sequence generators, and multi-tenant indexes will be created automatically.

---

## 🔒 Security & Data Isolation
- **Multi-Tenancy Isolation**: Enforced across every table using `organization_id` and `owner_id`.
- **Row Level Security (RLS)**: PostgreSQL policies isolate tenant and owner data.
- **Audit Logging**: `integration_logs`, `whatsapp_logs`, and `esign_transactions` record every transaction timestamp and API payload.

---

© 2026 **BrokerProp Platform**. All rights reserved.
