# 🏡 StayFile - Database Data Model Documentation (LOCKED & FINALIZED v4)

This document provides a comprehensive technical reference for the **StayFile** PostgreSQL multi-tenant database schema. It details all 19 tables, custom enums, data types, integrity constraints, automated DB triggers, composite indexes, and realistic sample data for both **Direct Property Owners** and **Real Estate Brokerage Agencies**.

---

## 📑 Table of Contents

1. [Enums & Custom Data Types](#1-enums--custom-data-types)
2. [1. organizations](#1-organizations)
3. [2. profiles](#2-profiles)
4. [3. branding_settings](#3-branding_settings)
5. [4. landlords](#4-landlords)
6. [5. properties](#5-properties)
7. [6. units](#6-units)
8. [7. tenants](#7-tenants)
9. [8. agreement_templates](#8-agreement_templates)
10. [9. leases](#9-leases)
11. [10. meter_readings](#10-meter_readings)
12. [11. invoices](#11-invoices)
13. [12. invoice_line_items](#12-invoice_line_items)
14. [13. receipts](#13-receipts)
15. [14. landlord_payouts](#14-landlord_payouts)
16. [15. bbps_transactions](#15-bbps_transactions)
17. [16. police_verifications](#16-police_verifications)
18. [17. esign_transactions](#17-esign_transactions)
19. [18. whatsapp_logs](#18-whatsapp_logs)
20. [19. property_leads](#19-property_leads)
21. [20. maintenance_tickets](#20-maintenance_tickets)
22. [21. integration_logs](#21-integration_logs)
23. [Automated DB Triggers & Indexes](#automated-db-triggers--indexes)

---

## 1. Enums & Custom Data Types

| Enum Name | Allowed Values | Description |
| :--- | :--- | :--- |
| `user_role` | `SUPER_ADMIN`, `ADMIN`, `PROPERTY_MANAGER`, `AGENT`, `TENANT`, `PUBLIC_GUEST` | Permission levels in system RBAC |
| `organization_type` | `OWNER`, `BROKERAGE`, `HYBRID` | Account type (Direct Landlord vs Broker Agency) |
| `property_type` | `PG`, `FLAT`, `COMMERCIAL`, `HOSTEL` | Property classification |
| `sharing_type` | `SINGLE`, `DOUBLE`, `TRIPLE`, `FOUR_SHARING`, `FULL_FLAT`, `COMMERCIAL_SPACE` | Occupancy / sharing configuration |
| `unit_status` | `AVAILABLE`, `OCCUPIED`, `RESERVED`, `MAINTENANCE`, `DISABLED` | Inventory availability state |
| `lease_status` | `DRAFT`, `PENDING_ESIGN`, `ACTIVE`, `EXPIRED`, `TERMINATED`, `CANCELLED` | Agreement lifecycle state |
| `brokerage_fee_type` | `NONE`, `ONE_TIME`, `RECURRING_PERCENTAGE`, `RECURRING_FIXED` | Brokerage fee structure |
| `maintenance_fee_type` | `NONE`, `MONTHLY`, `ANNUAL_ONE_TIME` | PG & property maintenance fee model |
| `invoice_type` | `MOVE_IN`, `MONTHLY_RENT`, `UTILITY_ONLY`, `MAINTENANCE_ONLY`, `FINAL_SETTLEMENT` | Category of invoice statement |
| `invoice_status` | `DRAFT`, `UNPAID`, `PARTIAL`, `PAID`, `OVERDUE`, `CANCELLED` | Invoice payment status |
| `charge_type` | `RENT`, `SECURITY_DEPOSIT`, `ONE_TIME_BROKERAGE`, `RECURRING_COMMISSION`, `MAINTENANCE_FEE`, `AGREEMENT_FEE`, `ELECTRICITY_BILL`, `WATER_BILL`, `LATE_FEE`, `TOKEN_BOOKING`, `OTHER` | Line item charge classification |
| `receipt_type` | `SECURITY_DEPOSIT`, `RENT_PAYMENT`, `UTILITY_BILL`, `MAINTENANCE`, `BROKERAGE_FEE`, `AGREEMENT_FEE`, `TOKEN_BOOKING`, `OTHER` | Financial payment receipt purpose |
| `payment_mode` | `UPI`, `NET_BANKING`, `CREDIT_CARD`, `DEBIT_CARD`, `CASH`, `CHEQUE` | Payment instrument used |
| `payout_status` | `PENDING`, `PROCESSING`, `SETTLED`, `FAILED` | State of landlord payout transfer |

---

## 1. `organizations`
Stores top-level SaaS business accounts. Supports both Direct Landlords and Brokerage Agencies.

| Field | Data Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | `UUID` | `PRIMARY KEY` | Organization Identifier |
| `name` | `TEXT` | `NOT NULL` | Operating Agency / Owner Business Name |
| `slug` | `TEXT` | `UNIQUE`, `NOT NULL` | Subdomain slug e.g. `sunshine-properties` |
| `organization_type` | `organization_type` | `DEFAULT 'OWNER'` | `OWNER` vs `BROKERAGE` vs `HYBRID` |
| `is_active` | `BOOLEAN` | `DEFAULT TRUE` | Account active state |

---

## 2. `profiles`
User logins belonging to an organization.

| Field | Data Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | `UUID` | `PRIMARY KEY` | User Unique ID |
| `organization_id` | `UUID` | `FOREIGN KEY` ➔ `organizations` | Parent organization |
| `email` | `TEXT` | `NOT NULL` | User Email |
| `full_name` | `TEXT` | `NOT NULL` | User Name |
| `role` | `user_role` | `DEFAULT 'ADMIN'` | `ADMIN`, `PROPERTY_MANAGER`, `AGENT`, `TENANT` |

---

## 3. `landlords`
Legal property owners. For Brokers, stores client landlords with full bank payout details.

| Field | Data Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | `UUID` | `PRIMARY KEY` | Landlord Identifier |
| `managing_organization_id` | `UUID` | `FOREIGN KEY` ➔ `organizations` | Managing Agency / Owner Org |
| `legal_name` | `TEXT` | `NOT NULL` | Legal Name for Agreements |
| `phone` | `TEXT` | `NOT NULL` | Contact Mobile Number |
| `pan` | `TEXT` | - | PAN for Tax / TDS Compliance |
| `bank_account_number` | `TEXT` | - | Landlord Payout Bank Account |
| `bank_ifsc_code` | `TEXT` | - | Bank IFSC Code |
| `owner_upi_id` | `TEXT` | - | Direct Payout UPI VPA |

---

## 4. `properties`
Properties (Buildings, PG Complexes, Flats) managed by an organization.

| Field | Data Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | `UUID` | `PRIMARY KEY` | Property Identifier |
| `organization_id` | `UUID` | `FOREIGN KEY` ➔ `organizations` | Managing Organization |
| `landlord_id` | `UUID` | `FOREIGN KEY` ➔ `landlords` | Legal Property Owner |
| `name` | `TEXT` | `NOT NULL` | Property Name e.g. "Sunshine PG" |
| `type` | `property_type` | `DEFAULT 'PG'` | PG vs Flat vs Commercial |

---

## 5. `units`
PG Beds & Flat Units. Supports PG Room-Bed hierarchy using `parent_unit_id`.

| Field | Data Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | `UUID` | `PRIMARY KEY` | Unit / Bed Identifier |
| `property_id` | `UUID` | `FOREIGN KEY` ➔ `properties` | Parent Property |
| `parent_unit_id` | `UUID` | `FOREIGN KEY` ➔ `units` | Nullable parent for PG Room ➔ Bed Slot |
| `unit_number` | `TEXT` | `NOT NULL` | e.g. "Flat 402" or "Room 101 - Bed A" |
| `monthly_rent` | `DECIMAL(10,2)`| `NOT NULL` | Base Rent Amount |
| `status` | `unit_status` | `DEFAULT 'AVAILABLE'`| Availability State |

---

## 6. `leases`
Tenancy contracts with fee terms (`brokerage_fee_type`, `maintenance_fee_type`, `agreement_fee_amount`).

| Field | Data Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | `UUID` | `PRIMARY KEY` | Lease Contract ID |
| `organization_id` | `UUID` | `FOREIGN KEY` ➔ `organizations` | Parent Org |
| `unit_id` | `UUID` | `FOREIGN KEY` ➔ `units` | Assigned Unit / Bed |
| `tenant_id` | `UUID` | `FOREIGN KEY` ➔ `tenants` | Renter |
| `landlord_id` | `UUID` | `FOREIGN KEY` ➔ `landlords` | Legal Landlord signing e-Sign |
| `created_by` | `UUID` | `FOREIGN KEY` ➔ `profiles` | Staff/Broker user who created lease |
| `brokerage_fee_type` | `brokerage_fee_type` | `DEFAULT 'NONE'` | `NONE`, `ONE_TIME`, `RECURRING_PERCENTAGE` |
| `brokerage_amount` | `DECIMAL(10,2)`| `DEFAULT 0.00` | Upfront fee or % |
| `maintenance_fee_type`| `maintenance_fee_type`| `DEFAULT 'NONE'` | `NONE`, `MONTHLY`, `ANNUAL_ONE_TIME` |

---

## 7. `meter_readings` [NEW]
Tracks room sub-meter electricity & water readings for automated utility line-item billing.

| Field | Data Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | `UUID` | `PRIMARY KEY` | Meter Reading ID |
| `unit_id` | `UUID` | `FOREIGN KEY` ➔ `units` | Target Unit / PG Room |
| `meter_type` | `TEXT` | `DEFAULT 'ELECTRICITY'` | ELECTRICITY / WATER |
| `previous_reading` | `DECIMAL(10,2)`| `NOT NULL` | Previous meter index |
| `current_reading` | `DECIMAL(10,2)`| `NOT NULL` | Current meter index |
| `units_consumed` | `DECIMAL(10,2)`| `GENERATED ALWAYS` | `current_reading - previous_reading` |
| `total_charge` | `DECIMAL(10,2)`| `GENERATED ALWAYS` | `units_consumed * rate_per_unit` |

---

## 8. `invoices` & `invoice_line_items` [NEW]
Sub-ledger for monthly rent statements, move-in invoices, line-item breakdowns, and balance due tracking.

| Table | Column | Description |
| :--- | :--- | :--- |
| **`invoices`** | `invoice_number` | Org-Scoped Number e.g. `INV-2026-000001` |
| | `invoice_type` | `MOVE_IN` vs `MONTHLY_RENT` |
| | `total_amount` | Sum of all line items |
| | `paid_amount` | Auto-updated by trigger from `receipts` |
| | `balance_due` | `GENERATED ALWAYS` (`total_amount - paid_amount`) |
| **`invoice_line_items`**| `charge_type` | `RENT`, `SECURITY_DEPOSIT`, `ONE_TIME_BROKERAGE`, `ELECTRICITY_BILL`, `MAINTENANCE_FEE`, `AGREEMENT_FEE` |

---

## 9. `landlord_payouts` [NEW]
Broker settlement ledger for paying landlords net rent collected minus agency commission.

| Field | Data Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `payout_number` | `TEXT` | `NOT NULL` | Org-Scoped Payout No. e.g. `PAY-2026-000001` |
| `landlord_id` | `UUID` | `FOREIGN KEY` ➔ `landlords` | Client Landlord |
| `total_collected` | `DECIMAL(10,2)`| `NOT NULL` | Total Tenant Rent Collected |
| `commission_amount`| `DECIMAL(10,2)`| `DEFAULT 0.00` | Agency Commission Fee Deducted |
| `net_payout_amount`| `DECIMAL(10,2)`| `NOT NULL` | Net Bank Amount Transferred to Landlord |
| `utr_number` | `TEXT` | - | Bank Transfer Reference UTR |
| `payout_status` | `payout_status` | `DEFAULT 'PENDING'` | `PENDING`, `SETTLED`, `FAILED` |

---

## Automated DB Triggers & Indexes

1. **`trg_sync_unit_lease_status`**:
   - Automatically marks `units.status = 'OCCUPIED'` and populates `units.current_lease_id` when a lease becomes `ACTIVE`.
   - Frees unit back to `AVAILABLE` when lease expires or is cancelled.
2. **`trg_sync_invoice_payment_status`**:
   - Automatically recalculates `invoices.paid_amount`, computes `balance_due`, and updates invoice status (`UNPAID` ➔ `PARTIAL` ➔ `PAID`) whenever a payment `receipt` is inserted, updated, or **deleted**.
   - Handles receipt re-assignment across invoices cleanly.
3. **Multi-Tenant Composite & Unique Indexes**:
   - `idx_unique_monthly_rent_invoice`: Partial unique index on `(lease_id, billing_period_start)` for `MONTHLY_RENT` invoices to prevent duplicate monthly rent billing.
   - `idx_invoices_overdue_lookup`: Partial index on `(organization_id, due_date)` WHERE `status = 'UNPAID'` for high-speed overdue invoice identification.
   - `idx_leases_org_status`, `idx_landlords_org`, `idx_invoices_org_status`, `idx_payouts_landlord`, `idx_meter_readings_unit`.
4. **Row Level Security (RLS) Policies**:
   - Organization-authenticated policies on `properties`, `units`, `leases`, `invoices`, `receipts`, and `landlord_payouts` enforcing database-level tenant isolation via `app.current_organization_id`.

