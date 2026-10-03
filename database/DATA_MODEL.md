# StayFile Database Data Model

This document describes the schema implemented by
[`database/init-scripts/01-init.sql`](init-scripts/01-init.sql). The SQL script is
the source of truth. This model contains 21 tables and 25 PostgreSQL enum types.

## Database Conventions

- IDs use `UUID` with `gen_random_uuid()` defaults.
- Timestamps use `TIMESTAMPTZ` and default to `NOW()`.
- Monetary values use `DECIMAL(10,2)`.
- Organization-owned records contain `organization_id`, except landlords, which
  use `managing_organization_id`.
- Important parent-child relationships use composite foreign keys containing the
  organization ID to prevent cross-organization references.
- The SQL enables RLS. Application connections must set
  `app.current_organization_id` before querying organization-owned data.
- The initialization script is intended for a fresh database. It is not a
  versioned, rerunnable migration because enum, trigger, policy, and constraint
  statements are not all guarded with existence checks.
- There is no migration tool (Flyway/Liquibase) and Hibernate runs with
  `ddl-auto=none`. Docker only runs `init-scripts/` when the data volume is first
  created. After editing the init script on a dev machine, recreate the volume:
  `docker compose down -v && docker compose up -d`. Once real data exists, add
  idempotent scripts under `database/migrations/` instead.

## Enum Types

| Type | Values |
| --- | --- |
| `user_role` | `SUPER_ADMIN`, `ADMIN`, `PROPERTY_MANAGER`, `AGENT`, `TENANT`, `PUBLIC_GUEST` |
| `organization_type` | `OWNER`, `BROKERAGE`, `HYBRID` |
| `owner_type` | `INDIVIDUAL`, `COMPANY`, `TRUST`, `PARTNERSHIP` |
| `property_type` | `PG`, `FLAT`, `COMMERCIAL`, `HOSTEL` |
| `sharing_type` | `SINGLE`, `DOUBLE`, `TRIPLE`, `FOUR_SHARING`, `FULL_FLAT`, `COMMERCIAL_SPACE` |
| `unit_status` | `AVAILABLE`, `OCCUPIED`, `RESERVED`, `MAINTENANCE`, `DISABLED` |
| `lease_status` | `DRAFT`, `PENDING_ESIGN`, `ACTIVE`, `EXPIRED`, `TERMINATED`, `CANCELLED` |
| `brokerage_fee_type` | `NONE`, `ONE_TIME`, `RECURRING_PERCENTAGE`, `RECURRING_FIXED` |
| `maintenance_fee_type` | `NONE`, `MONTHLY`, `ANNUAL_ONE_TIME` |
| `invoice_type` | `MOVE_IN`, `MONTHLY_RENT`, `UTILITY_ONLY`, `MAINTENANCE_ONLY`, `FINAL_SETTLEMENT` |
| `invoice_status` | `DRAFT`, `UNPAID`, `PARTIAL`, `PAID`, `OVERDUE`, `CANCELLED` |
| `charge_type` | `RENT`, `SECURITY_DEPOSIT`, `ONE_TIME_BROKERAGE`, `RECURRING_COMMISSION`, `MAINTENANCE_FEE`, `AGREEMENT_FEE`, `ELECTRICITY_BILL`, `WATER_BILL`, `LATE_FEE`, `TOKEN_BOOKING`, `OTHER` |
| `receipt_type` | `SECURITY_DEPOSIT`, `RENT_PAYMENT`, `UTILITY_BILL`, `MAINTENANCE`, `BROKERAGE_FEE`, `AGREEMENT_FEE`, `TOKEN_BOOKING`, `OTHER` |
| `payment_mode` | `UPI`, `NET_BANKING`, `CREDIT_CARD`, `DEBIT_CARD`, `CASH`, `CHEQUE` |
| `payout_status` | `PENDING`, `PROCESSING`, `SETTLED`, `FAILED` |
| `verification_status` | `NOT_STARTED`, `PENDING`, `VERIFIED`, `REJECTED` |
| `whatsapp_direction` | `INBOUND`, `OUTBOUND` |
| `whatsapp_msg_type` | `TEXT`, `TEMPLATE`, `DOCUMENT`, `IMAGE`, `INTERACTIVE`, `LOCATION` |
| `whatsapp_status` | `SENT`, `DELIVERED`, `READ`, `FAILED` |
| `bbps_status` | `PENDING`, `SUCCESS`, `FAILED` |
| `lead_source` | `WHATSAPP`, `WEBSITE`, `NINETYNINE_ACRES`, `NOBROKER`, `MAGICBRICKS`, `DIRECT` |
| `lead_status` | `NEW`, `CONTACTED`, `VISITED`, `CONVERTED`, `LOST` |
| `ticket_status` | `OPEN`, `IN_PROGRESS`, `RESOLVED`, `CLOSED` |
| `ticket_priority` | `LOW`, `MEDIUM`, `HIGH`, `URGENT` |
| `ticket_category` | `PLUMBING`, `ELECTRICAL`, `APPLIANCE`, `CARPENTRY`, `CLEANING`, `PEST_CONTROL`, `INTERNET`, `OTHER` |

## Core Organization Tables

### `organizations`

Columns: `id`, `name`, `slug`, `organization_type`, `is_active`, `metadata`,
`created_at`, `updated_at`.

`slug` is unique. `organization_type` defaults to `OWNER`.

### `profiles`

Columns: `id`, `organization_id`, `email`, `password_hash`, `phone`, `full_name`,
`avatar_url`, `role`, `permissions`, `is_active`, `metadata`, `created_at`,
`updated_at`.

`organization_id` references `organizations`. `(id, organization_id)` is unique,
and `(organization_id, email)` has a unique index.

### `branding_settings`

Columns: `id`, `organization_id`, `legal_business_name`, `trade_name`,
`owner_pan`, `owner_gstin`, `rera_number`, `registered_office_address`,
`contact_phone`, `contact_email`, `agency_logo_url`, `primary_color`,
`secondary_color`, `signature_url`, `owner_upi_id`, `bank_account_number`,
`bank_ifsc_code`, `bank_name`, `account_holder_name`, `metadata`, `created_at`,
`updated_at`.

There is one branding record per organization.

### `landlords`

Columns: `id`, `managing_organization_id`, `profile_id`, `owner_type`,
`legal_name`, `email`, `phone`, `pan`, `gstin`, `address`,
`bank_account_number`, `bank_ifsc_code`, `bank_name`, `account_holder_name`,
`owner_upi_id`, `is_active`, `metadata`, `created_at`, `updated_at`.

`managing_organization_id` references `organizations`. `profile_id` is an
optional profile link. `(id, managing_organization_id)` is unique.

## Property and Lease Tables

### `properties`

Columns: `id`, `organization_id`, `landlord_id`, `name`, `type`, `address`,
`city`, `state`, `pincode`, `landmark`, `latitude`, `longitude`, `amenities`,
`rules`, `images`, `description`, `is_active`, `metadata`, `created_at`,
`updated_at`.

Coordinates are range-checked. `(id, organization_id)` is unique. If a landlord
is assigned, the composite `(landlord_id, organization_id)` relationship must
match the landlord's managing organization.

### `units`

Columns: `id`, `organization_id`, `property_id`, `parent_unit_id`, `unit_number`,
`floor_number`, `sharing_type`, `monthly_rent`, `security_deposit`, `status`,
`current_lease_id`, `amenities`, `notes`, `metadata`, `created_at`, `updated_at`.

`parent_unit_id` supports room-to-bed hierarchy. `(property_id, unit_number)`
is unique. Rent and deposit cannot be negative. `property_id`, `parent_unit_id`,
and `current_lease_id` are foreign-key relationships; property and parent-unit
links are organization-scoped.

### `tenants`

Columns: `id`, `organization_id`, `user_id`, `full_name`, `email`, `phone`,
`permanent_address`, `emergency_contact_name`, `emergency_contact_phone`,
`emergency_contact_relation`, `id_proof_type`, `id_proof_last4`,
`id_proof_number`, `id_proof_document_url`, `kyc_status`, `metadata`,
`created_at`, `updated_at`.

The encrypted identity value is stored in `id_proof_number`. `(id,
organization_id)` is unique. `user_id` is unique when present (one tenant
record per tenant login), which powers tenant self-service lookups.

### `agreement_templates`

Columns: `id`, `organization_id`, `title`, `template_type`, `content_template`,
`is_default`, `metadata`, `created_at`, `updated_at`.

`content_template` stores the Markdown/HTML template. `(id, organization_id)`
is unique.

### `leases`

Columns: `id`, `organization_id`, `unit_id`, `tenant_id`, `landlord_id`,
`created_by`, `agreement_template_id`, `start_date`, `end_date`, `monthly_rent`,
`security_deposit`, `rent_due_day`, `notice_period_days`,
`lock_in_period_months`, `brokerage_fee_type`, `brokerage_amount`,
`maintenance_fee_type`, `maintenance_fee_amount`, `agreement_fee_amount`,
`custom_clauses`, `terms_and_conditions`, `status`, `agreement_pdf_url`,
`is_esign_completed`, `esign_transaction_id`, `e_stamp_number`, `metadata`,
`created_at`, `updated_at`.

Constraints include valid date order, rent/deposit/fee non-negativity, and a
rent due day from 1 through 31. Unit, tenant, landlord, creator, and agreement
template references are organization-scoped. `(id, organization_id)` is unique.

## Billing and Utility Tables

### `meter_readings`

Columns: `id`, `organization_id`, `unit_id`, `meter_type`, `previous_reading`,
`current_reading`, `units_consumed`, `rate_per_unit`, `total_charge`,
`reading_date`, `is_billed`, `notes`, `created_at`, `updated_at`.

`units_consumed` and `total_charge` are stored generated columns. `meter_type`
must be `ELECTRICITY`, `WATER`, or `GAS`. Current readings cannot be lower than
previous readings, and rates cannot be negative.

### `invoices`

Columns: `id`, `organization_id`, `invoice_number`, `lease_id`, `tenant_id`,
`unit_id`, `invoice_type`, `billing_period_start`, `billing_period_end`,
`due_date`, `subtotal_amount`, `tax_amount`, `discount_amount`, `total_amount`,
`paid_amount`, `balance_due`, `status`, `invoice_pdf_url`, `notes`, `metadata`,
`created_at`, `updated_at`.

`balance_due` is a stored generated column equal to `total_amount - paid_amount`.
Amounts cannot be negative. `(id, organization_id)` is unique.

### `invoice_line_items`

Columns: `id`, `invoice_id`, `organization_id`, `charge_type`, `description`,
`quantity`, `unit_price`, `amount`, `created_at`.

Line items support rent, deposit, brokerage, recurring commission, maintenance,
agreement, electricity, water, late fee, token, and other charges. The invoice
and organization relationship is enforced with a composite foreign key.

### `receipts`

Columns: `id`, `receipt_number`, `organization_id`, `invoice_id`, `lease_id`,
`tenant_id`, `receipt_type`, `amount`, `payment_mode`, `transaction_reference`,
`payment_date`, `notes`, `pdf_url`, `metadata`, `created_at`, `updated_at`.

`invoice_id` is nullable for advance/token receipts. Amount must be greater than
zero. Receipt, invoice, lease, tenant, and organization relationships are
organization-scoped. `(id, organization_id)` is unique.

### `landlord_payouts`

Columns: `id`, `payout_number`, `organization_id`, `landlord_id`, `property_id`,
`total_collected`, `commission_amount`, `deductions_amount`,
`net_payout_amount`, `payout_mode`, `payout_status`, `utr_number`, `payout_date`,
`notes`, `statement_pdf_url`, `created_at`, `updated_at`.

Collected, commission, deductions, and net payout amounts cannot be negative.
Landlord and optional property references are organization-scoped.

## Compliance and Integration Tables

### `bbps_transactions`

Columns: `id`, `organization_id`, `property_id`, `utility_type`, `biller_id`,
`consumer_number`, `bill_amount`, `due_date`, `status`, `transaction_ref`,
`created_at`, `updated_at`.

### `police_verifications`

Columns: `id`, `organization_id`, `lease_id`, `tenant_id`, `landlord_id`,
`police_station_name`, `jurisdiction_district`, `status`, `submission_date`,
`application_reference_no`, `verification_pdf_url`, `metadata`, `created_at`,
`updated_at`.

Lease, tenant, and optional landlord references are organization-scoped.

### `esign_transactions`

Columns: `id`, `organization_id`, `lease_id`, `provider`, `transaction_id`,
`stamp_paper_number`, `stamp_amount`, `status`, `signed_pdf_url`,
`audit_trail_json`, `created_at`, `updated_at`.

### `whatsapp_logs`

Columns: `id`, `organization_id`, `tenant_id`, `wamid`, `phone_number`,
`direction`, `type`, `message_body`, `template_name`, `status`, `error_details`,
`created_at`, `updated_at`.

The tenant link is optional and organization-scoped when present.

### `property_leads`

Columns: `id`, `organization_id`, `property_id`, `unit_id`, `assigned_to`, `name`,
`phone`, `email`, `source`, `status`, `notes`, `follow_up_date`, `metadata`,
`created_at`, `updated_at`.

Property, optional unit, and optional assigned profile references are
organization-scoped.

### `maintenance_tickets`

Columns: `id`, `organization_id`, `unit_id`, `tenant_id`, `assigned_to`, `title`,
`description`, `category`, `priority`, `status`, `images`, `created_at`,
`updated_at`.

`category`, `priority` and `status` use the `ticket_category`,
`ticket_priority` and `ticket_status` enums (defaults `PLUMBING`, `MEDIUM`,
`OPEN`). Unit, tenant, and optional assigned profile references are
organization-scoped.

### `integration_logs`

Columns: `id`, `organization_id`, `provider`, `endpoint`, `request_payload`,
`response_payload`, `status_code`, `execution_time_ms`, `error_message`,
`created_at`, `updated_at`.

The organization link is optional for platform-level integration events.

## Triggers and Generated Values

### `update_updated_at_column`

Before-update triggers maintain `updated_at` on all 21 tables.

### `sync_unit_lease_status`

An after-insert or after-update-of-`status, unit_id` trigger on `leases`:

- Marks the unit `OCCUPIED` and sets `current_lease_id` for an active lease.
- Releases the old unit when an active lease changes units.
- Marks the unit `AVAILABLE` when a lease becomes `EXPIRED`, `TERMINATED`, or
  `CANCELLED`.

### `sync_invoice_payment_status`

An after-insert/update/delete trigger on `receipts`:

- Sums receipts linked to the invoice into `paid_amount`.
- Sets `PAID`, `PARTIAL`, `UNPAID`, or `OVERDUE` based on payment and due date.
- Preserves `DRAFT` and `CANCELLED` invoices.
- Recalculates both the old and new invoice when a receipt is reassigned.

## Indexes and Uniqueness

- Unique profile email per organization.
- Unique receipt, invoice, and payout numbers per organization.
- One active lease per unit.
- One `MONTHLY_RENT` invoice per lease and billing-period start.
- Unique tenant phone per organization.
- Unique tenant `user_id` (partial index, ignores NULL).
- Organization/status and tenant/status lookup indexes for leases and invoices.
- Unit, meter, receipt, payout, WhatsApp, lead, ticket, and integration lookup
  indexes.
- Overdue lookup index for invoices with status `UNPAID` or `PARTIAL`.

## Row-Level Security

RLS is enabled on all 21 tables. Public read policies expose active properties
and available units. Organization policies use
`public.current_organization_id()`, which reads the PostgreSQL setting
`app.current_organization_id`.

The setting must be applied on the same database connection used by the query.
The Docker development configuration currently connects as the PostgreSQL
superuser; PostgreSQL superusers bypass RLS. Production must use a dedicated
non-superuser application role and configure the organization context per
transaction.

## Business Scope

The schema supports:

- Owner, brokerage, and hybrid organization types.
- Client landlords and landlord payouts.
- PG rooms with child beds.
- Lease brokerage and maintenance fee terms.
- Move-in and recurring invoice line items.
- Electricity, water, and gas meter readings.
- Receipts, partial payments, invoice balances, and overdue status.
- BBPS, police verification, e-sign, WhatsApp, lead, maintenance, and
  integration audit records.

The tables record BBPS, e-sign, KYC, and WhatsApp integration state; external
provider API execution remains an application/integration responsibility.

## Owner / Broker / Hybrid Model

The same tables serve all three organization types. The key is
`properties.landlord_id`:

| Property kind | `landlord_id` | Lessor & bank details come from |
| --- | --- | --- |
| Self-owned | `NULL` | `branding_settings` (legal name, PAN, GSTIN, bank, UPI) |
| Managed (on behalf of a landlord) | set | `landlords` row |

`organizations.organization_type` decides which kinds of property are allowed:

| Organization type | Self-owned | Managed |
| --- | --- | --- |
| `OWNER` | yes (only) | no |
| `BROKERAGE` | no | yes (only, landlord required) |
| `HYBRID` | yes | yes (per property) |

Derived rules (enforced by the backend `OrganizationPolicyService`, not by SQL,
because they span tables):

- Landlord management and landlord payouts are not available to `OWNER`
  organizations.
- A lease's `landlord_id` always equals its property's `landlord_id`.
- Brokerage fees (`brokerage_fee_type <> 'NONE'`), brokerage/commission invoice
  lines and `BROKERAGE_FEE` receipts are only allowed on managed properties.
- Changing `organization_type` is validated against existing data (e.g. an org
  with managed properties cannot become `OWNER`).
