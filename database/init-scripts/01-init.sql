-- =============================================================================
-- StayFile - Enterprise Multi-Tenant Data Model & Database Schema
-- Multi-Tenant PostgreSQL Master Script
-- Features: Owner & Brokerage Dual Support, PG Bed Hierarchy, Invoice Sub-Ledger,
--           Sub-Meter Readings, Landlord Payouts, Automated Triggers, Encrypted KYC,
--           WhatsApp & BBPS Utilities, Property Leads CRM.
-- =============================================================================

-- Enable required Postgres extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Set Timezone to UTC
SET timezone = 'UTC';

-- -----------------------------------------------------------------------------
-- 1. ENUM TYPES (Extensible System Enums)
-- -----------------------------------------------------------------------------

CREATE TYPE user_role AS ENUM (
    'SUPER_ADMIN',        -- StayFile Platform Administrator
    'ADMIN',              -- Organization / Agency Administrator
    'PROPERTY_MANAGER',   -- Site / Branch / PG Manager
    'AGENT',              -- Brokerage Field Agent / Staff Assistant
    'TENANT',             -- PG Resident / Flat Rentee
    'PUBLIC_GUEST'        -- Unauthenticated Showcase Visitor
);

CREATE TYPE organization_type AS ENUM (
    'OWNER',              -- Direct property owner, family, or landlord company
    'BROKERAGE',          -- Broker or real-estate agency managing client properties
    'HYBRID'              -- Owner who also manages properties for other external landlords
);

CREATE TYPE owner_type AS ENUM (
    'INDIVIDUAL',
    'COMPANY',
    'TRUST',
    'PARTNERSHIP'
);

CREATE TYPE property_type AS ENUM (
    'PG',                 -- Paying Guest / Co-Living Space
    'FLAT',               -- Apartment / Villa / Independent House
    'COMMERCIAL',         -- Office / Shop / Warehouse
    'HOSTEL'              -- Student Hostel / Dormitory
);

CREATE TYPE sharing_type AS ENUM (
    'SINGLE',             -- Single Room / Independent Flat
    'DOUBLE',             -- 2 Sharing
    'TRIPLE',             -- 3 Sharing
    'FOUR_SHARING',       -- 4 Sharing
    'FULL_FLAT',          -- Whole Apartment
    'COMMERCIAL_SPACE'    -- Office / Shop / Warehouse unit
);

CREATE TYPE unit_status AS ENUM (
    'AVAILABLE',          -- Ready for rent
    'OCCUPIED',           -- Currently rented out
    'RESERVED',           -- Token amount received / Booking pending
    'MAINTENANCE',        -- Under repair / Cleaning
    'DISABLED'            -- Out of service
);

CREATE TYPE lease_status AS ENUM (
    'DRAFT',              -- Agreement being drafted
    'PENDING_ESIGN',      -- Sent for Aadhaar e-Sign OTP
    'ACTIVE',             -- Active tenancy
    'EXPIRED',            -- Term completed
    'TERMINATED',         -- Ended before lock-in
    'CANCELLED'           -- Cancelled prior to move-in
);

CREATE TYPE brokerage_fee_type AS ENUM (
    'NONE',                   -- No brokerage fee (Direct Owner)
    'ONE_TIME',               -- One-time upfront brokerage at move-in
    'RECURRING_PERCENTAGE',   -- Monthly recurring management fee (% of rent)
    'RECURRING_FIXED'         -- Monthly recurring fixed fee (₹ amount)
);

CREATE TYPE maintenance_fee_type AS ENUM (
    'NONE',                   -- No maintenance fee
    'MONTHLY',                -- Recurring monthly maintenance charge
    'ANNUAL_ONE_TIME'         -- One-time annual PG maintenance charge at move-in
);

CREATE TYPE invoice_type AS ENUM (
    'MOVE_IN',                -- Move-in Invoice (Rent + Deposit + Brokerage + Agreement Fee)
    'MONTHLY_RENT',           -- Recurring Monthly Rent & Utilities Invoice
    'UTILITY_ONLY',           -- Standalone Utility Bill Invoice
    'MAINTENANCE_ONLY',       -- Maintenance / Repair Invoice
    'FINAL_SETTLEMENT'        -- Move-out / Security Deposit Refund Invoice
);

CREATE TYPE invoice_status AS ENUM (
    'DRAFT',
    'UNPAID',
    'PARTIAL',
    'PAID',
    'OVERDUE',
    'CANCELLED'
);

CREATE TYPE charge_type AS ENUM (
    'RENT',                   -- Base Monthly Rent
    'SECURITY_DEPOSIT',       -- Move-in Refundable Security Deposit
    'ONE_TIME_BROKERAGE',     -- Upfront Brokerage Fee
    'RECURRING_COMMISSION',   -- Monthly Agency Management Fee
    'MAINTENANCE_FEE',       -- Maintenance Charge (Monthly or Annual)
    'AGREEMENT_FEE',         -- E-Stamp Paper & Aadhaar e-Sign Processing Fee
    'ELECTRICITY_BILL',       -- Sub-meter Electricity Charge
    'WATER_BILL',             -- Water Charge
    'LATE_FEE',               -- Overdue Payment Fine
    'TOKEN_BOOKING',          -- Advance Booking Token Amount
    'OTHER'                   -- Miscellaneous Charge
);

CREATE TYPE receipt_type AS ENUM (
    'SECURITY_DEPOSIT',
    'RENT_PAYMENT',
    'UTILITY_BILL',
    'MAINTENANCE',
    'BROKERAGE_FEE',
    'AGREEMENT_FEE',
    'TOKEN_BOOKING',
    'OTHER'
);

CREATE TYPE payment_mode AS ENUM (
    'UPI',                -- Google Pay / PhonePe / Paytm / BHIM
    'NET_BANKING',        -- Internet banking
    'CREDIT_CARD',        -- Credit card
    'DEBIT_CARD',         -- Debit card
    'CASH',               -- Physical Cash
    'CHEQUE'              -- Bank Cheque
);

CREATE TYPE payout_status AS ENUM (
    'PENDING',
    'PROCESSING',
    'SETTLED',
    'FAILED'
);

CREATE TYPE verification_status AS ENUM (
    'NOT_STARTED',
    'PENDING',
    'VERIFIED',
    'REJECTED'
);

CREATE TYPE whatsapp_direction AS ENUM (
    'INBOUND',
    'OUTBOUND'
);

CREATE TYPE whatsapp_msg_type AS ENUM (
    'TEXT',
    'TEMPLATE',
    'DOCUMENT',
    'IMAGE',
    'INTERACTIVE',
    'LOCATION'
);

CREATE TYPE whatsapp_status AS ENUM (
    'SENT',
    'DELIVERED',
    'READ',
    'FAILED'
);

CREATE TYPE bbps_status AS ENUM (
    'PENDING',
    'SUCCESS',
    'FAILED'
);

CREATE TYPE lead_source AS ENUM (
    'WHATSAPP',
    'WEBSITE',
    'NINETYNINE_ACRES',
    'NOBROKER',
    'MAGICBRICKS',
    'DIRECT'
);

CREATE TYPE lead_status AS ENUM (
    'NEW',
    'CONTACTED',
    'VISITED',
    'CONVERTED',
    'LOST'
);

-- -----------------------------------------------------------------------------
-- 2. CORE MULTI-TENANT, USER & LANDLORD TABLES
-- -----------------------------------------------------------------------------

-- Organizations (SaaS Multi-Tenant Accounts: Direct Owner or Brokerage Agency)
CREATE TABLE IF NOT EXISTS public.organizations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name TEXT NOT NULL,                             -- Agency or Owner Business Legal Name
    slug TEXT UNIQUE NOT NULL,                       -- URL-friendly slug e.g. "sunshine-properties"
    organization_type organization_type NOT NULL DEFAULT 'OWNER',
    is_active BOOLEAN DEFAULT TRUE,
    metadata JSONB DEFAULT '{}'::jsonb,              -- Custom org settings
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Profiles (User Accounts mapped to Auth User ID)
CREATE TABLE IF NOT EXISTS public.profiles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),  -- Links to Auth Provider User ID
    organization_id UUID NOT NULL REFERENCES public.organizations(id) ON DELETE CASCADE,
    email TEXT NOT NULL,
    password_hash TEXT NOT NULL,                     -- BCrypt hashed password
    phone TEXT,
    full_name TEXT NOT NULL,
    avatar_url TEXT,
    role user_role NOT NULL DEFAULT 'ADMIN',
    permissions JSONB DEFAULT '[]'::jsonb,           -- Fine-grained RBAC permissions
    is_active BOOLEAN DEFAULT TRUE,
    metadata JSONB DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    CONSTRAINT uq_profiles_id_organization UNIQUE (id, organization_id)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_profiles_organization_email
    ON public.profiles (organization_id, email);

-- Custom Branding & Default Organization Payout Settings
CREATE TABLE IF NOT EXISTS public.branding_settings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID UNIQUE NOT NULL REFERENCES public.organizations(id) ON DELETE CASCADE,
    legal_business_name TEXT NOT NULL,              -- Business Legal Name for Agreements
    trade_name TEXT,                                 -- Display Brand Name
    owner_pan TEXT,                                  -- PAN Card Number
    owner_gstin TEXT,                                -- GSTIN Number
    rera_number TEXT,                                -- RERA Registration Number
    registered_office_address TEXT,                  -- Official Address
    contact_phone TEXT,
    contact_email TEXT,
    agency_logo_url TEXT,                            -- Brand Logo PNG/SVG URL
    primary_color TEXT DEFAULT '#2563eb',            -- Primary Brand Hex Color
    secondary_color TEXT DEFAULT '#1e293b',          -- Secondary Hex Color
    signature_url TEXT,                              -- Digital Signature Image URL for PDFs
    
    -- Default Org Bank Payout Details
    owner_upi_id TEXT,                               -- e.g. "sunshine@icici"
    bank_account_number TEXT,
    bank_ifsc_code TEXT,
    bank_name TEXT,
    account_holder_name TEXT,
    
    metadata JSONB DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Landlords (Legal Property Owners)
-- Supports both Direct Landlords and Brokerage Client Landlords with complete Payout details.
CREATE TABLE IF NOT EXISTS public.landlords (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    managing_organization_id UUID NOT NULL REFERENCES public.organizations(id) ON DELETE CASCADE,
    profile_id UUID REFERENCES public.profiles(id) ON DELETE SET NULL, -- Optional portal login
    owner_type owner_type NOT NULL DEFAULT 'INDIVIDUAL',
    legal_name TEXT NOT NULL,                       -- Landlord Full Legal Name (for Agreements)
    email TEXT,
    phone TEXT NOT NULL,
    pan TEXT,                                        -- PAN for Tax Compliance & TDS
    gstin TEXT,
    address TEXT,
    
    -- Landlord Direct Payout Banking Details
    bank_account_number TEXT,
    bank_ifsc_code TEXT,
    bank_name TEXT,
    account_holder_name TEXT,
    owner_upi_id TEXT,                               -- Direct Payout UPI VPA
    
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    CONSTRAINT uq_landlords_id_organization UNIQUE (id, managing_organization_id)
);

-- -----------------------------------------------------------------------------
-- 3. PROPERTY & INVENTORY MANAGEMENT TABLES
-- -----------------------------------------------------------------------------

-- Properties (Building / Complex / Apartment / Villa)
CREATE TABLE IF NOT EXISTS public.properties (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES public.organizations(id) ON DELETE CASCADE,
    landlord_id UUID REFERENCES public.landlords(id) ON DELETE RESTRICT, -- Legal Landlord Owner
    name TEXT NOT NULL,                              -- e.g. "Sunshine Heights" or "GreenStays PG"
    type property_type NOT NULL DEFAULT 'PG',
    address TEXT NOT NULL,
    city TEXT NOT NULL,
    state TEXT NOT NULL,
    pincode VARCHAR(10) NOT NULL,
    landmark TEXT,
    latitude DECIMAL(10, 8),
    longitude DECIMAL(11, 8),
    amenities JSONB DEFAULT '[]'::jsonb,             -- ["WiFi", "AC", "Power Backup", "Food"]
    rules TEXT[] DEFAULT '{}',                       -- House Rules
    images TEXT[] DEFAULT '{}',                      -- Image URLs array
    description TEXT,
    is_active BOOLEAN DEFAULT TRUE,
    metadata JSONB DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    CONSTRAINT uq_properties_id_organization UNIQUE (id, organization_id),
    CONSTRAINT ck_property_coordinates CHECK (latitude IS NULL OR latitude BETWEEN -90 AND 90),
    CONSTRAINT ck_property_longitude CHECK (longitude IS NULL OR longitude BETWEEN -180 AND 180)
);

-- Foreign Key to Landlords table for property ownership
ALTER TABLE public.properties
    ADD CONSTRAINT fk_properties_landlord_organization
    FOREIGN KEY (landlord_id, organization_id)
    REFERENCES public.landlords (id, managing_organization_id);

-- Units / Rooms / Bed Slots (Supports PG Room Bed hierarchy via parent_unit_id)
CREATE TABLE IF NOT EXISTS public.units (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES public.organizations(id) ON DELETE CASCADE,
    property_id UUID NOT NULL REFERENCES public.properties(id) ON DELETE CASCADE,
    parent_unit_id UUID REFERENCES public.units(id) ON DELETE CASCADE, -- Nullable parent for PG Room -> Bed Slot
    unit_number TEXT NOT NULL,                       -- e.g. "Flat 402" or "Room 101 - Bed A"
    floor_number INTEGER DEFAULT 0,
    sharing_type sharing_type NOT NULL DEFAULT 'FULL_FLAT',
    monthly_rent DECIMAL(10, 2) NOT NULL CHECK (monthly_rent >= 0),
    security_deposit DECIMAL(10, 2) NOT NULL CHECK (security_deposit >= 0),
    status unit_status NOT NULL DEFAULT 'AVAILABLE',
    current_lease_id UUID,                           -- FK added below for O(1) active lease lookup
    amenities JSONB DEFAULT '[]'::jsonb,
    notes TEXT,
    metadata JSONB DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    CONSTRAINT uq_units_id_organization UNIQUE (id, organization_id),
    CONSTRAINT uq_property_unit_number UNIQUE (property_id, unit_number)
);

-- -----------------------------------------------------------------------------
-- 4. TENANTS, AGREEMENTS, LEASES & UTILITY READINGS
-- -----------------------------------------------------------------------------

-- Tenants (Renters & PG Residents)
CREATE TABLE IF NOT EXISTS public.tenants (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES public.organizations(id) ON DELETE CASCADE,
    user_id UUID REFERENCES public.profiles(id) ON DELETE SET NULL, -- Optional linked tenant login
    full_name TEXT NOT NULL,
    email TEXT,
    phone TEXT NOT NULL,
    permanent_address TEXT NOT NULL,
    emergency_contact_name TEXT,
    emergency_contact_phone TEXT,
    emergency_contact_relation TEXT,
    
    -- Identity Proofs (Compliance & AES-256 Encrypted)
    id_proof_type TEXT DEFAULT 'Aadhaar',
    id_proof_last4 VARCHAR(20),                      -- Masked display value ("XXXX-XXXX-9012")
    id_proof_number TEXT,                            -- AES-256 encrypted string
    id_proof_document_url TEXT,
    kyc_status verification_status NOT NULL DEFAULT 'PENDING',
    
    metadata JSONB DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    CONSTRAINT uq_tenants_id_organization UNIQUE (id, organization_id)
);

-- Agreement Templates (Multi-Template Master)
CREATE TABLE IF NOT EXISTS public.agreement_templates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES public.organizations(id) ON DELETE CASCADE,
    title TEXT NOT NULL,                             -- e.g. "Standard PG 11-Month Agreement"
    template_type TEXT NOT NULL DEFAULT 'PG_11_MONTH',
    content_template TEXT NOT NULL,                  -- Markdown/HTML template with Mustache placeholders
    is_default BOOLEAN DEFAULT FALSE,
    metadata JSONB DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    CONSTRAINT uq_agreement_templates_id_organization UNIQUE (id, organization_id)
);

-- Leases / Tenancy Agreements
CREATE TABLE IF NOT EXISTS public.leases (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES public.organizations(id) ON DELETE CASCADE,
    unit_id UUID NOT NULL REFERENCES public.units(id) ON DELETE RESTRICT,
    tenant_id UUID NOT NULL REFERENCES public.tenants(id) ON DELETE RESTRICT,
    landlord_id UUID REFERENCES public.landlords(id) ON DELETE RESTRICT, -- Legal Landlord signing agreement
    created_by UUID NOT NULL REFERENCES public.profiles(id) ON DELETE RESTRICT, -- Staff/Broker user who created lease
    agreement_template_id UUID REFERENCES public.agreement_templates(id) ON DELETE SET NULL,
    
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    monthly_rent DECIMAL(10, 2) NOT NULL CHECK (monthly_rent >= 0),
    security_deposit DECIMAL(10, 2) NOT NULL CHECK (security_deposit >= 0),
    rent_due_day INTEGER NOT NULL DEFAULT 5 CHECK (rent_due_day BETWEEN 1 AND 31),
    notice_period_days INTEGER DEFAULT 30 CHECK (notice_period_days >= 0),
    lock_in_period_months INTEGER DEFAULT 6 CHECK (lock_in_period_months >= 0),
    
    -- Flexible Brokerage & Fee Configuration Terms
    brokerage_fee_type brokerage_fee_type NOT NULL DEFAULT 'NONE',
    brokerage_amount DECIMAL(10, 2) DEFAULT 0.00 CHECK (brokerage_amount >= 0), -- Amount or %
    maintenance_fee_type maintenance_fee_type NOT NULL DEFAULT 'NONE',
    maintenance_fee_amount DECIMAL(10, 2) DEFAULT 0.00 CHECK (maintenance_fee_amount >= 0),
    agreement_fee_amount DECIMAL(10, 2) DEFAULT 0.00 CHECK (agreement_fee_amount >= 0),
    
    custom_clauses TEXT,
    terms_and_conditions TEXT,
    
    status lease_status NOT NULL DEFAULT 'DRAFT',
    agreement_pdf_url TEXT,
    is_esign_completed BOOLEAN DEFAULT FALSE,
    esign_transaction_id TEXT,
    e_stamp_number TEXT,
    
    metadata JSONB DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    CONSTRAINT uq_leases_id_organization UNIQUE (id, organization_id),
    CONSTRAINT ck_lease_dates CHECK (end_date >= start_date)
);

-- Foreign Keys for units.current_lease_id and leases composite constraints
ALTER TABLE public.units 
    ADD CONSTRAINT fk_units_current_lease 
    FOREIGN KEY (current_lease_id) REFERENCES public.leases(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_units_property_organization
        FOREIGN KEY (property_id, organization_id)
        REFERENCES public.properties (id, organization_id);

ALTER TABLE public.leases
    ADD CONSTRAINT fk_leases_unit_organization
        FOREIGN KEY (unit_id, organization_id)
        REFERENCES public.units (id, organization_id),
    ADD CONSTRAINT fk_leases_tenant_organization
        FOREIGN KEY (tenant_id, organization_id)
        REFERENCES public.tenants (id, organization_id),
    ADD CONSTRAINT fk_leases_landlord_organization
        FOREIGN KEY (landlord_id, organization_id)
        REFERENCES public.landlords (id, managing_organization_id),
    ADD CONSTRAINT fk_leases_creator_organization
        FOREIGN KEY (created_by, organization_id)
        REFERENCES public.profiles (id, organization_id),
    ADD CONSTRAINT fk_leases_template_organization
        FOREIGN KEY (agreement_template_id, organization_id)
        REFERENCES public.agreement_templates (id, organization_id);

-- Sub-Meter Utilities Reading Tracker (Electricity & Water Sub-Meters)
CREATE TABLE IF NOT EXISTS public.meter_readings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES public.organizations(id) ON DELETE CASCADE,
    unit_id UUID NOT NULL REFERENCES public.units(id) ON DELETE CASCADE,
    meter_type TEXT NOT NULL DEFAULT 'ELECTRICITY', -- ELECTRICITY / WATER / GAS
    previous_reading DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    current_reading DECIMAL(10, 2) NOT NULL,
    units_consumed DECIMAL(10, 2) GENERATED ALWAYS AS (current_reading - previous_reading) STORED,
    rate_per_unit DECIMAL(10, 2) NOT NULL DEFAULT 10.00,
    total_charge DECIMAL(10, 2) GENERATED ALWAYS AS ((current_reading - previous_reading) * rate_per_unit) STORED,
    reading_date DATE NOT NULL DEFAULT CURRENT_DATE,
    is_billed BOOLEAN DEFAULT FALSE,
    notes TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    CONSTRAINT ck_meter_readings_value CHECK (current_reading >= previous_reading),
    CONSTRAINT ck_meter_readings_type CHECK (meter_type IN ('ELECTRICITY', 'WATER', 'GAS')),
    CONSTRAINT ck_meter_readings_rate CHECK (rate_per_unit >= 0)
);

-- -----------------------------------------------------------------------------
-- 5. INVOICES, RECEPTS, PAYOUTS & POLICE VERIFICATION
-- -----------------------------------------------------------------------------

-- Monthly Invoices Ledger (Header Table)
CREATE TABLE IF NOT EXISTS public.invoices (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES public.organizations(id) ON DELETE CASCADE,
    invoice_number TEXT NOT NULL,                    -- Scoped per Organization (e.g. INV-2026-000001)
    lease_id UUID NOT NULL REFERENCES public.leases(id) ON DELETE RESTRICT,
    tenant_id UUID NOT NULL REFERENCES public.tenants(id) ON DELETE RESTRICT,
    unit_id UUID NOT NULL REFERENCES public.units(id) ON DELETE RESTRICT,
    
    invoice_type invoice_type NOT NULL DEFAULT 'MONTHLY_RENT',
    billing_period_start DATE NOT NULL,
    billing_period_end DATE NOT NULL,
    due_date DATE NOT NULL,
    
    subtotal_amount DECIMAL(10, 2) NOT NULL DEFAULT 0.00 CHECK (subtotal_amount >= 0),
    tax_amount DECIMAL(10, 2) DEFAULT 0.00 CHECK (tax_amount >= 0),
    discount_amount DECIMAL(10, 2) DEFAULT 0.00 CHECK (discount_amount >= 0),
    total_amount DECIMAL(10, 2) NOT NULL CHECK (total_amount >= 0),
    paid_amount DECIMAL(10, 2) NOT NULL DEFAULT 0.00 CHECK (paid_amount >= 0),
    balance_due DECIMAL(10, 2) GENERATED ALWAYS AS (total_amount - paid_amount) STORED,
    
    status invoice_status NOT NULL DEFAULT 'UNPAID',
    invoice_pdf_url TEXT,
    notes TEXT,
    metadata JSONB DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    CONSTRAINT uq_invoices_id_organization UNIQUE (id, organization_id)
);

-- Invoice Line Items (Breakdown of Rent, Deposit, Brokerage, Utilities, Fines)
CREATE TABLE IF NOT EXISTS public.invoice_line_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    invoice_id UUID NOT NULL REFERENCES public.invoices(id) ON DELETE CASCADE,
    organization_id UUID NOT NULL REFERENCES public.organizations(id) ON DELETE CASCADE,
    charge_type charge_type NOT NULL DEFAULT 'RENT',
    description TEXT NOT NULL,                      -- e.g. "Monthly Rent for Oct 2026", "Sub-meter Electricity (150 units)"
    quantity DECIMAL(10, 2) DEFAULT 1.00,
    unit_price DECIMAL(10, 2) NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Receipts (Payment Transactions linked to Invoice & Lease)
CREATE TABLE IF NOT EXISTS public.receipts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    receipt_number TEXT NOT NULL,                    -- Scoped per Organization (e.g. REC-2026-000001)
    organization_id UUID NOT NULL REFERENCES public.organizations(id) ON DELETE CASCADE,
    invoice_id UUID REFERENCES public.invoices(id) ON DELETE RESTRICT, -- Nullable for advance token deposits
    lease_id UUID NOT NULL REFERENCES public.leases(id) ON DELETE RESTRICT,
    tenant_id UUID NOT NULL REFERENCES public.tenants(id) ON DELETE RESTRICT,
    
    receipt_type receipt_type NOT NULL DEFAULT 'RENT_PAYMENT',
    amount DECIMAL(10, 2) NOT NULL CHECK (amount > 0),
    payment_mode payment_mode NOT NULL DEFAULT 'UPI',
    transaction_reference TEXT,                      -- UTR / Gateway Transaction ID
    payment_date DATE NOT NULL DEFAULT CURRENT_DATE,
    notes TEXT,
    pdf_url TEXT,
    
    metadata JSONB DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    CONSTRAINT uq_receipts_id_organization UNIQUE (id, organization_id)
);

-- Landlord Payout Settlements (Broker Agency -> Client Landlord Payout Ledger)
CREATE TABLE IF NOT EXISTS public.landlord_payouts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    payout_number TEXT NOT NULL,                     -- Scoped per Org (e.g. PAY-2026-000001)
    organization_id UUID NOT NULL REFERENCES public.organizations(id) ON DELETE CASCADE,
    landlord_id UUID NOT NULL REFERENCES public.landlords(id) ON DELETE RESTRICT,
    property_id UUID REFERENCES public.properties(id) ON DELETE SET NULL,
    
    total_collected DECIMAL(10, 2) NOT NULL CHECK (total_collected >= 0),
    commission_amount DECIMAL(10, 2) DEFAULT 0.00 CHECK (commission_amount >= 0),
    deductions_amount DECIMAL(10, 2) DEFAULT 0.00 CHECK (deductions_amount >= 0), -- Maintenance/Repair deductions
    net_payout_amount DECIMAL(10, 2) NOT NULL CHECK (net_payout_amount >= 0),
    
    payout_mode payment_mode NOT NULL DEFAULT 'NET_BANKING',
    payout_status payout_status NOT NULL DEFAULT 'PENDING',
    utr_number TEXT,                                 -- Bank Transfer Reference UTR
    payout_date DATE,
    notes TEXT,
    statement_pdf_url TEXT,
    
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- BBPS Utility Bill Transactions (Electricity, Water via Decentro / BBPS)
CREATE TABLE IF NOT EXISTS public.bbps_transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES public.organizations(id) ON DELETE CASCADE,
    property_id UUID NOT NULL REFERENCES public.properties(id) ON DELETE CASCADE,
    utility_type TEXT NOT NULL,                      -- ELECTRICITY / WATER
    biller_id TEXT NOT NULL,
    consumer_number TEXT NOT NULL,
    bill_amount DECIMAL(10, 2) NOT NULL,
    due_date DATE,
    status bbps_status NOT NULL DEFAULT 'PENDING',
    transaction_ref TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Police Verification & Tenant Intimation Tracking
CREATE TABLE IF NOT EXISTS public.police_verifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES public.organizations(id) ON DELETE CASCADE,
    lease_id UUID NOT NULL REFERENCES public.leases(id) ON DELETE CASCADE,
    tenant_id UUID NOT NULL REFERENCES public.tenants(id) ON DELETE CASCADE,
    landlord_id UUID REFERENCES public.landlords(id) ON DELETE RESTRICT,
    
    police_station_name TEXT NOT NULL,
    jurisdiction_district TEXT NOT NULL,
    status verification_status NOT NULL DEFAULT 'NOT_STARTED',
    submission_date DATE,
    application_reference_no TEXT,
    verification_pdf_url TEXT,
    
    metadata JSONB DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- E-Sign & E-Stamping API Transactions Log
CREATE TABLE IF NOT EXISTS public.esign_transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES public.organizations(id) ON DELETE CASCADE,
    lease_id UUID NOT NULL REFERENCES public.leases(id) ON DELETE CASCADE,
    provider TEXT NOT NULL,                          -- "Digio", "Leegality", "NeSL"
    transaction_id TEXT NOT NULL,                    -- Provider reference token
    stamp_paper_number TEXT,                         -- State e-Stamp number
    stamp_amount DECIMAL(10, 2),
    status TEXT NOT NULL,                            -- PENDING, SIGNED, FAILED
    signed_pdf_url TEXT,
    audit_trail_json JSONB DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- -----------------------------------------------------------------------------
-- 6. WHATSAPP LOGS, PROPERTY LEADS, MAINTENANCE & AUDIT LOGS
-- -----------------------------------------------------------------------------

-- WhatsApp Communication & Delivery Audit Logs
CREATE TABLE IF NOT EXISTS public.whatsapp_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES public.organizations(id) ON DELETE CASCADE,
    tenant_id UUID REFERENCES public.tenants(id) ON DELETE SET NULL,
    
    wamid TEXT,                                      -- Meta WhatsApp Message ID
    phone_number TEXT NOT NULL,
    direction whatsapp_direction NOT NULL DEFAULT 'OUTBOUND',
    type whatsapp_msg_type NOT NULL DEFAULT 'TEXT',
    message_body TEXT,
    template_name TEXT,
    status whatsapp_status NOT NULL DEFAULT 'SENT',
    
    error_details JSONB DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Property Leads & CRM Enquiries (WhatsApp / Website / 99acres / NoBroker)
CREATE TABLE IF NOT EXISTS public.property_leads (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES public.organizations(id) ON DELETE CASCADE,
    property_id UUID NOT NULL REFERENCES public.properties(id) ON DELETE CASCADE,
    unit_id UUID REFERENCES public.units(id) ON DELETE SET NULL,
    assigned_to UUID REFERENCES public.profiles(id) ON DELETE SET NULL, -- Assigned Agent Profile
    
    name TEXT NOT NULL,
    phone TEXT NOT NULL,
    email TEXT,
    source lead_source NOT NULL DEFAULT 'WHATSAPP',
    status lead_status NOT NULL DEFAULT 'NEW',
    notes TEXT,
    follow_up_date TIMESTAMPTZ,
    
    metadata JSONB DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Maintenance Complaints / Service Tickets
CREATE TABLE IF NOT EXISTS public.maintenance_tickets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES public.organizations(id) ON DELETE CASCADE,
    unit_id UUID NOT NULL REFERENCES public.units(id) ON DELETE CASCADE,
    tenant_id UUID NOT NULL REFERENCES public.tenants(id) ON DELETE CASCADE,
    assigned_to UUID REFERENCES public.profiles(id) ON DELETE SET NULL,
    
    title TEXT NOT NULL,                             -- e.g. "Geyser not heating"
    description TEXT,
    category TEXT DEFAULT 'PLUMBING',
    priority TEXT DEFAULT 'MEDIUM',
    status TEXT DEFAULT 'OPEN',
    images TEXT[] DEFAULT '{}',
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Third-Party API Integration Audit Logs
CREATE TABLE IF NOT EXISTS public.integration_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID REFERENCES public.organizations(id) ON DELETE CASCADE,
    provider TEXT NOT NULL,                          -- "SUREPASS", "RAZORPAY", "WHATSAPP", "DIGIO", "BBPS"
    endpoint TEXT NOT NULL,
    request_payload JSONB DEFAULT '{}'::jsonb,
    response_payload JSONB DEFAULT '{}'::jsonb,
    status_code INTEGER,
    execution_time_ms INTEGER,
    error_message TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Enforce organization consistency across dependent records.
ALTER TABLE public.units
    ADD CONSTRAINT fk_units_parent_organization
    FOREIGN KEY (parent_unit_id, organization_id)
    REFERENCES public.units (id, organization_id);

ALTER TABLE public.meter_readings
    ADD CONSTRAINT fk_meter_readings_unit_organization
    FOREIGN KEY (unit_id, organization_id)
    REFERENCES public.units (id, organization_id);

ALTER TABLE public.invoices
    ADD CONSTRAINT fk_invoices_lease_organization
    FOREIGN KEY (lease_id, organization_id)
    REFERENCES public.leases (id, organization_id),
    ADD CONSTRAINT fk_invoices_tenant_organization
    FOREIGN KEY (tenant_id, organization_id)
    REFERENCES public.tenants (id, organization_id),
    ADD CONSTRAINT fk_invoices_unit_organization
    FOREIGN KEY (unit_id, organization_id)
    REFERENCES public.units (id, organization_id);

ALTER TABLE public.invoice_line_items
    ADD CONSTRAINT fk_invoice_line_items_invoice_organization
    FOREIGN KEY (invoice_id, organization_id)
    REFERENCES public.invoices (id, organization_id);

ALTER TABLE public.receipts
    ADD CONSTRAINT fk_receipts_invoice_organization
    FOREIGN KEY (invoice_id, organization_id)
    REFERENCES public.invoices (id, organization_id),
    ADD CONSTRAINT fk_receipts_lease_organization
    FOREIGN KEY (lease_id, organization_id)
    REFERENCES public.leases (id, organization_id),
    ADD CONSTRAINT fk_receipts_tenant_organization
    FOREIGN KEY (tenant_id, organization_id)
    REFERENCES public.tenants (id, organization_id);

ALTER TABLE public.landlord_payouts
    ADD CONSTRAINT fk_payouts_landlord_organization
    FOREIGN KEY (landlord_id, organization_id)
    REFERENCES public.landlords (id, managing_organization_id),
    ADD CONSTRAINT fk_payouts_property_organization
    FOREIGN KEY (property_id, organization_id)
    REFERENCES public.properties (id, organization_id);

ALTER TABLE public.police_verifications
    ADD CONSTRAINT fk_police_verifications_lease_organization
    FOREIGN KEY (lease_id, organization_id)
    REFERENCES public.leases (id, organization_id),
    ADD CONSTRAINT fk_police_verifications_tenant_organization
    FOREIGN KEY (tenant_id, organization_id)
    REFERENCES public.tenants (id, organization_id),
    ADD CONSTRAINT fk_police_verifications_landlord_organization
    FOREIGN KEY (landlord_id, organization_id)
    REFERENCES public.landlords (id, managing_organization_id);

ALTER TABLE public.esign_transactions
    ADD CONSTRAINT fk_esign_transactions_lease_organization
    FOREIGN KEY (lease_id, organization_id)
    REFERENCES public.leases (id, organization_id);

ALTER TABLE public.whatsapp_logs
    ADD CONSTRAINT fk_whatsapp_logs_tenant_organization
    FOREIGN KEY (tenant_id, organization_id)
    REFERENCES public.tenants (id, organization_id);

ALTER TABLE public.property_leads
    ADD CONSTRAINT fk_property_leads_property_organization
    FOREIGN KEY (property_id, organization_id)
    REFERENCES public.properties (id, organization_id),
    ADD CONSTRAINT fk_property_leads_unit_organization
    FOREIGN KEY (unit_id, organization_id)
    REFERENCES public.units (id, organization_id),
    ADD CONSTRAINT fk_property_leads_assignee_organization
    FOREIGN KEY (assigned_to, organization_id)
    REFERENCES public.profiles (id, organization_id);

ALTER TABLE public.maintenance_tickets
    ADD CONSTRAINT fk_maintenance_tickets_unit_organization
    FOREIGN KEY (unit_id, organization_id)
    REFERENCES public.units (id, organization_id),
    ADD CONSTRAINT fk_maintenance_tickets_tenant_organization
    FOREIGN KEY (tenant_id, organization_id)
    REFERENCES public.tenants (id, organization_id),
    ADD CONSTRAINT fk_maintenance_tickets_assignee_organization
    FOREIGN KEY (assigned_to, organization_id)
    REFERENCES public.profiles (id, organization_id);

-- -----------------------------------------------------------------------------
-- 7. AUTOMATED TRIGGERS FOR LEASE-UNIT STATUS & INVOICE PAYMENTS
-- -----------------------------------------------------------------------------

-- Timestamp Update Trigger Function
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Attach Updated_At Triggers to ALL Tables
CREATE TRIGGER trg_organizations_updated BEFORE UPDATE ON public.organizations FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER trg_profiles_updated BEFORE UPDATE ON public.profiles FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER trg_branding_updated BEFORE UPDATE ON public.branding_settings FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER trg_landlords_updated BEFORE UPDATE ON public.landlords FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER trg_properties_updated BEFORE UPDATE ON public.properties FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER trg_units_updated BEFORE UPDATE ON public.units FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER trg_tenants_updated BEFORE UPDATE ON public.tenants FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER trg_agreement_templates_updated BEFORE UPDATE ON public.agreement_templates FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER trg_leases_updated BEFORE UPDATE ON public.leases FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER trg_meter_readings_updated BEFORE UPDATE ON public.meter_readings FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER trg_invoices_updated BEFORE UPDATE ON public.invoices FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER trg_receipts_updated BEFORE UPDATE ON public.receipts FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER trg_landlord_payouts_updated BEFORE UPDATE ON public.landlord_payouts FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER trg_bbps_updated BEFORE UPDATE ON public.bbps_transactions FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER trg_police_verifications_updated BEFORE UPDATE ON public.police_verifications FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER trg_esign_transactions_updated BEFORE UPDATE ON public.esign_transactions FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER trg_whatsapp_logs_updated BEFORE UPDATE ON public.whatsapp_logs FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER trg_property_leads_updated BEFORE UPDATE ON public.property_leads FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER trg_maintenance_tickets_updated BEFORE UPDATE ON public.maintenance_tickets FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER trg_integration_logs_updated BEFORE UPDATE ON public.integration_logs FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- AUTOMATED TRIGGER 1: Syncs unit occupancy status and current_lease_id in DB
CREATE OR REPLACE FUNCTION sync_unit_lease_status()
RETURNS TRIGGER AS $$
BEGIN
    -- Handle unit reassignment on active lease update
    IF TG_OP = 'UPDATE' AND OLD.unit_id <> NEW.unit_id AND OLD.status = 'ACTIVE' THEN
        UPDATE public.units
        SET status = 'AVAILABLE',
            current_lease_id = NULL,
            updated_at = CURRENT_TIMESTAMP
        WHERE id = OLD.unit_id AND current_lease_id = OLD.id;
    END IF;

    IF NEW.status = 'ACTIVE' THEN
        UPDATE public.units 
        SET status = 'OCCUPIED', 
            current_lease_id = NEW.id,
            updated_at = CURRENT_TIMESTAMP
        WHERE id = NEW.unit_id;
    ELSIF NEW.status IN ('EXPIRED', 'TERMINATED', 'CANCELLED') THEN
        UPDATE public.units 
        SET status = 'AVAILABLE', 
            current_lease_id = NULL,
            updated_at = CURRENT_TIMESTAMP
        WHERE current_lease_id = NEW.id;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_sync_unit_lease_status
AFTER INSERT OR UPDATE OF status, unit_id ON public.leases
FOR EACH ROW EXECUTE FUNCTION sync_unit_lease_status();

-- AUTOMATED TRIGGER 2: Syncs invoice paid_amount, balance_due & status on receipt insert/update/delete
CREATE OR REPLACE FUNCTION sync_invoice_payment_status()
RETURNS TRIGGER AS $$
DECLARE
    total_payments DECIMAL(10, 2);
    target_invoice_id UUID;
    target_total DECIMAL(10, 2);
    target_due_date DATE;
    target_status invoice_status;
BEGIN
    IF TG_OP = 'DELETE' THEN
        target_invoice_id := OLD.invoice_id;
    ELSE
        target_invoice_id := NEW.invoice_id;
    END IF;

    IF target_invoice_id IS NOT NULL THEN
        SELECT COALESCE(SUM(amount), 0.00) INTO total_payments
        FROM public.receipts
        WHERE invoice_id = target_invoice_id;

        SELECT total_amount, due_date, status
        INTO target_total, target_due_date, target_status
        FROM public.invoices
        WHERE id = target_invoice_id;

        IF target_total IS NOT NULL THEN
            UPDATE public.invoices
            SET paid_amount = total_payments,
                status = CASE
                            WHEN target_status IN ('DRAFT', 'CANCELLED') THEN target_status
                            WHEN total_payments >= target_total THEN 'PAID'::invoice_status
                            WHEN target_due_date < CURRENT_DATE THEN 'OVERDUE'::invoice_status
                            WHEN total_payments > 0 THEN 'PARTIAL'::invoice_status
                            ELSE 'UNPAID'::invoice_status
                         END,
                updated_at = CURRENT_TIMESTAMP
            WHERE id = target_invoice_id;
        END IF;
    END IF;

    -- Handle re-assignment if invoice_id changed on UPDATE
    IF TG_OP = 'UPDATE' AND OLD.invoice_id IS NOT NULL AND OLD.invoice_id <> NEW.invoice_id THEN
        SELECT COALESCE(SUM(amount), 0.00) INTO total_payments
        FROM public.receipts
        WHERE invoice_id = OLD.invoice_id;

        SELECT total_amount, due_date, status
        INTO target_total, target_due_date, target_status
        FROM public.invoices
        WHERE id = OLD.invoice_id;

        IF target_total IS NOT NULL THEN
            UPDATE public.invoices
            SET paid_amount = total_payments,
                status = CASE
                            WHEN target_status IN ('DRAFT', 'CANCELLED') THEN target_status
                            WHEN total_payments >= target_total THEN 'PAID'::invoice_status
                            WHEN target_due_date < CURRENT_DATE THEN 'OVERDUE'::invoice_status
                            WHEN total_payments > 0 THEN 'PARTIAL'::invoice_status
                            ELSE 'UNPAID'::invoice_status
                         END,
                updated_at = CURRENT_TIMESTAMP
            WHERE id = OLD.invoice_id;
        END IF;
    END IF;

    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_sync_invoice_payment_status
AFTER INSERT OR UPDATE OR DELETE ON public.receipts
FOR EACH ROW EXECUTE FUNCTION sync_invoice_payment_status();

-- -----------------------------------------------------------------------------
-- 8. COMPOSITE INDEXES & UNIQUE SCOPED CONSTRAINTS
-- -----------------------------------------------------------------------------

-- Unique Scoped Constraints per Organization
CREATE UNIQUE INDEX IF NOT EXISTS uniq_receipt_number_per_org ON public.receipts(organization_id, receipt_number);
CREATE UNIQUE INDEX IF NOT EXISTS uniq_invoice_number_per_org ON public.invoices(organization_id, invoice_number);
CREATE UNIQUE INDEX IF NOT EXISTS uniq_payout_number_per_org ON public.landlord_payouts(organization_id, payout_number);

CREATE UNIQUE INDEX IF NOT EXISTS uq_one_active_lease_per_unit
    ON public.leases(unit_id)
    WHERE status = 'ACTIVE';

-- Prevent duplicate MONTHLY_RENT invoices for the same lease and period
CREATE UNIQUE INDEX IF NOT EXISTS idx_unique_monthly_rent_invoice
    ON public.invoices (lease_id, billing_period_start)
    WHERE invoice_type = 'MONTHLY_RENT';

ALTER TABLE public.tenants ADD CONSTRAINT uq_org_phone UNIQUE (organization_id, phone);

-- High-Performance Composite Multi-Tenant Performance Indexes
CREATE INDEX IF NOT EXISTS idx_leases_org_status ON public.leases(organization_id, status);
CREATE INDEX IF NOT EXISTS idx_landlords_org ON public.landlords(managing_organization_id, is_active);
CREATE INDEX IF NOT EXISTS idx_properties_landlord ON public.properties(landlord_id);
CREATE INDEX IF NOT EXISTS idx_units_property_status ON public.units(property_id, status);
CREATE INDEX IF NOT EXISTS idx_units_org_status ON public.units(organization_id, status);
CREATE INDEX IF NOT EXISTS idx_meter_readings_unit ON public.meter_readings(unit_id, reading_date);
CREATE INDEX IF NOT EXISTS idx_invoices_org_status ON public.invoices(organization_id, status);
CREATE INDEX IF NOT EXISTS idx_invoices_tenant_status ON public.invoices(tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_invoices_overdue_lookup
    ON public.invoices(organization_id, due_date)
    WHERE status IN ('UNPAID', 'PARTIAL');
CREATE INDEX IF NOT EXISTS idx_receipts_org_tenant ON public.receipts(organization_id, tenant_id);
CREATE INDEX IF NOT EXISTS idx_receipts_invoice ON public.receipts(invoice_id);
CREATE INDEX IF NOT EXISTS idx_payouts_landlord ON public.landlord_payouts(landlord_id, payout_status);
CREATE INDEX IF NOT EXISTS idx_whatsapp_logs_org_phone ON public.whatsapp_logs(organization_id, phone_number);
CREATE INDEX IF NOT EXISTS idx_leads_org_status ON public.property_leads(organization_id, status);
CREATE INDEX IF NOT EXISTS idx_integration_logs_provider ON public.integration_logs(provider);

-- -----------------------------------------------------------------------------
-- 9. ROW LEVEL SECURITY (RLS) POLICIES
-- -----------------------------------------------------------------------------

CREATE OR REPLACE FUNCTION public.current_organization_id()
RETURNS UUID
LANGUAGE SQL
STABLE
AS $$
    SELECT NULLIF(current_setting('app.current_organization_id', true), '')::UUID
$$;

ALTER TABLE public.organizations ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.branding_settings ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.landlords ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.properties ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.units ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.tenants ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.agreement_templates ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.leases ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.meter_readings ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.invoices ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.invoice_line_items ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.receipts ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.landlord_payouts ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.bbps_transactions ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.police_verifications ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.esign_transactions ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.whatsapp_logs ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.property_leads ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.maintenance_tickets ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.integration_logs ENABLE ROW LEVEL SECURITY;

-- Public showcase RLS policies
CREATE POLICY "Public can view active properties" ON public.properties
    FOR SELECT USING (is_active = TRUE);

CREATE POLICY "Public can view available units" ON public.units
    FOR SELECT USING (status = 'AVAILABLE');

-- Organization authenticated RLS policies (Defense-in-depth DB Multi-tenancy)
CREATE POLICY "Org members can manage properties" ON public.properties
    FOR ALL USING (organization_id = public.current_organization_id())
    WITH CHECK (organization_id = public.current_organization_id());

CREATE POLICY "Org members can manage units" ON public.units
    FOR ALL USING (organization_id = public.current_organization_id())
    WITH CHECK (organization_id = public.current_organization_id());

CREATE POLICY "Org members can manage leases" ON public.leases
    FOR ALL USING (organization_id = public.current_organization_id())
    WITH CHECK (organization_id = public.current_organization_id());

CREATE POLICY "Org members can manage invoices" ON public.invoices
    FOR ALL USING (organization_id = public.current_organization_id())
    WITH CHECK (organization_id = public.current_organization_id());

CREATE POLICY "Org members can manage receipts" ON public.receipts
    FOR ALL USING (organization_id = public.current_organization_id())
    WITH CHECK (organization_id = public.current_organization_id());

CREATE POLICY "Org members can manage payouts" ON public.landlord_payouts
    FOR ALL USING (organization_id = public.current_organization_id())
    WITH CHECK (organization_id = public.current_organization_id());

CREATE POLICY "Org members can manage organizations" ON public.organizations
    FOR ALL USING (id = public.current_organization_id())
    WITH CHECK (id = public.current_organization_id());

CREATE POLICY "Org members can manage profiles" ON public.profiles
    FOR ALL USING (organization_id = public.current_organization_id())
    WITH CHECK (organization_id = public.current_organization_id());

CREATE POLICY "Org members can manage branding" ON public.branding_settings
    FOR ALL USING (organization_id = public.current_organization_id())
    WITH CHECK (organization_id = public.current_organization_id());

CREATE POLICY "Org members can manage landlords" ON public.landlords
    FOR ALL USING (managing_organization_id = public.current_organization_id())
    WITH CHECK (managing_organization_id = public.current_organization_id());

CREATE POLICY "Org members can manage tenants" ON public.tenants
    FOR ALL USING (organization_id = public.current_organization_id())
    WITH CHECK (organization_id = public.current_organization_id());

CREATE POLICY "Org members can manage agreement templates" ON public.agreement_templates
    FOR ALL USING (organization_id = public.current_organization_id())
    WITH CHECK (organization_id = public.current_organization_id());

CREATE POLICY "Org members can manage meter readings" ON public.meter_readings
    FOR ALL USING (organization_id = public.current_organization_id())
    WITH CHECK (organization_id = public.current_organization_id());

CREATE POLICY "Org members can manage invoice line items" ON public.invoice_line_items
    FOR ALL USING (organization_id = public.current_organization_id())
    WITH CHECK (organization_id = public.current_organization_id());

CREATE POLICY "Org members can manage BBPS transactions" ON public.bbps_transactions
    FOR ALL USING (organization_id = public.current_organization_id())
    WITH CHECK (organization_id = public.current_organization_id());

CREATE POLICY "Org members can manage police verifications" ON public.police_verifications
    FOR ALL USING (organization_id = public.current_organization_id())
    WITH CHECK (organization_id = public.current_organization_id());

CREATE POLICY "Org members can manage e-sign transactions" ON public.esign_transactions
    FOR ALL USING (organization_id = public.current_organization_id())
    WITH CHECK (organization_id = public.current_organization_id());

CREATE POLICY "Org members can manage WhatsApp logs" ON public.whatsapp_logs
    FOR ALL USING (organization_id = public.current_organization_id())
    WITH CHECK (organization_id = public.current_organization_id());

CREATE POLICY "Org members can manage property leads" ON public.property_leads
    FOR ALL USING (organization_id = public.current_organization_id())
    WITH CHECK (organization_id = public.current_organization_id());

CREATE POLICY "Org members can manage maintenance tickets" ON public.maintenance_tickets
    FOR ALL USING (organization_id = public.current_organization_id())
    WITH CHECK (organization_id = public.current_organization_id());

CREATE POLICY "Org members can manage integration logs" ON public.integration_logs
    FOR ALL USING (organization_id = public.current_organization_id())
    WITH CHECK (organization_id = public.current_organization_id());