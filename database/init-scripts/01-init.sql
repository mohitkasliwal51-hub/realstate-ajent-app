-- =============================================================================
-- StayFile - Production Data Model & Database Schema
-- Multi-Tenant PostgreSQL Master Script
-- Features: Automated Lease-Unit Sync Triggers, Unique Org Receipts, Composite Indexes,
--           Encrypted KYC, WhatsApp & BBPS Utilities, Property Leads CRM.
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
    'OWNER_ADMIN',        -- Primary Property Owner / Broker Agency Admin
    'PROPERTY_MANAGER',   -- Site / Branch Manager
    'STAFF_ASSISTANT',    -- Front-desk / Maintenance Staff
    'TENANT',             -- PG Resident / Flat Rentee
    'PUBLIC_GUEST'        -- Unauthenticated Showcase Visitor
);

CREATE TYPE property_type AS ENUM (
    'PG',                 -- Paying Guest / Co-Living Space
    'FULL_FLAT',          -- Apartment / Villa / Independent House
    'COMMERCIAL',        -- Office / Shop / Warehouse
    'HOSTEL'              -- Student Hostel / Dormitory
);

CREATE TYPE sharing_type AS ENUM (
    'SINGLE',             -- Single Room / Independent Flat
    'DOUBLE',             -- 2 Sharing
    'TRIPLE',             -- 3 Sharing
    'FOUR_SHARING',       -- 4 Sharing
    'FULL_FLAT',          -- Whole Apartment
    'CUSTOM'              -- Custom Arrangement
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

CREATE TYPE receipt_type AS ENUM (
    'SECURITY_DEPOSIT',   -- Move-in Security Deposit
    'RENT_PAYMENT',       -- Monthly Rent Payment
    'UTILITY_BILL',       -- Electricity / Water / Gas Bill
    'MAINTENANCE',        -- Repairs / Cleaning Fee
    'TOKEN_BOOKING',      -- Advance Booking Amount
    'OTHER'               -- Miscellaneous
);

CREATE TYPE payment_mode AS ENUM (
    'UPI',                -- Google Pay / PhonePe / Paytm / BHIM
    'BANK_TRANSFER',      -- NEFT / RTGS / IMPS
    'CASH',               -- Physical Cash
    'CHEQUE',             -- Bank Cheque
    'RAZORPAY_ONLINE',    -- Payment Gateway Online
    'OTHER'
);

CREATE TYPE verification_status AS ENUM (
    'NOT_STARTED',
    'PENDING',
    'SUBMITTED',
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
    'BILL_FETCHED',
    'PAYMENT_INITIATED',
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
-- 2. CORE MULTI-TENANT & USER TABLES
-- -----------------------------------------------------------------------------

-- Organizations (Supports Multi-Branch Agencies & Companies)
CREATE TABLE IF NOT EXISTS public.organizations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name TEXT NOT NULL,                             -- Agency Legal / Operating Name
    slug TEXT UNIQUE NOT NULL,                       -- URL-friendly slug e.g. "sunshine-properties"
    is_active BOOLEAN DEFAULT TRUE,
    metadata JSONB DEFAULT '{}'::jsonb,              -- Future-proof JSONB for custom org settings
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Profiles (User Accounts mapped to Auth User ID)
CREATE TABLE IF NOT EXISTS public.profiles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),  -- Links to auth.users or Spring Boot User ID
    organization_id UUID REFERENCES public.organizations(id) ON DELETE CASCADE,
    email TEXT UNIQUE NOT NULL,
    phone TEXT,
    full_name TEXT NOT NULL,
    avatar_url TEXT,
    role user_role NOT NULL DEFAULT 'OWNER_ADMIN',
    permissions JSONB DEFAULT '[]'::jsonb,           -- Custom fine-grained permissions array
    is_active BOOLEAN DEFAULT TRUE,
    metadata JSONB DEFAULT '{}'::jsonb,              -- Future-proof extra user attributes
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Custom Branding & Legal Company Settings
CREATE TABLE IF NOT EXISTS public.branding_settings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID UNIQUE NOT NULL REFERENCES public.organizations(id) ON DELETE CASCADE,
    legal_business_name TEXT NOT NULL,              -- Owner's Legal Business Name for Agreements
    trade_name TEXT,                                 -- Display Brand Name
    owner_pan TEXT,                                  -- Owner's PAN Card Number
    owner_gstin TEXT,                                -- Owner's GSTIN Number
    rera_number TEXT,                                -- RERA Registration Number
    registered_office_address TEXT,                  -- Official Business Address
    contact_phone TEXT,
    contact_email TEXT,
    agency_logo_url TEXT,                            -- Brand Logo URL
    primary_color TEXT DEFAULT '#2563eb',            -- Brand Hex Color (Tailwind Blue-600)
    secondary_color TEXT DEFAULT '#1e293b',          -- Brand Secondary Color
    signature_url TEXT,                              -- Digital Signature Image URL for PDFs
    
    -- Payout Account Details
    owner_upi_id TEXT,                               -- e.g. "sunshine@icici"
    bank_account_number TEXT,
    bank_ifsc_code TEXT,
    bank_name TEXT,
    account_holder_name TEXT,
    
    metadata JSONB DEFAULT '{}'::jsonb,              -- Custom branding attributes
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- -----------------------------------------------------------------------------
-- 3. PROPERTY & INVENTORY MANAGEMENT TABLES
-- -----------------------------------------------------------------------------

-- Properties (Building / Complex / Apartment)
CREATE TABLE IF NOT EXISTS public.properties (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES public.organizations(id) ON DELETE CASCADE,
    owner_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    name TEXT NOT NULL,                              -- e.g. "Sunshine Heights" or "GreenStays PG"
    type property_type NOT NULL DEFAULT 'PG',        -- PG vs FULL_FLAT vs COMMERCIAL
    address TEXT NOT NULL,
    city TEXT NOT NULL,
    state TEXT NOT NULL,
    pincode VARCHAR(10) NOT NULL,
    landmark TEXT,
    latitude DECIMAL(10, 8),
    longitude DECIMAL(11, 8),
    amenities JSONB DEFAULT '[]'::jsonb,             -- ["WiFi", "AC", "Power Backup", "Food"]
    rules TEXT[] DEFAULT '{}',                       -- House Rules e.g. {"No Smoking", "Gate closes at 10 PM"}
    images TEXT[] DEFAULT '{}',                      -- Image URLs array
    description TEXT,
    is_active BOOLEAN DEFAULT TRUE,
    metadata JSONB DEFAULT '{}'::jsonb,              -- Future-proof custom property attributes
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Units / Beds (PG Rooms, Bed Slots, or Full Flat Units)
CREATE TABLE IF NOT EXISTS public.units (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    property_id UUID NOT NULL REFERENCES public.properties(id) ON DELETE CASCADE,
    organization_id UUID NOT NULL REFERENCES public.organizations(id) ON DELETE CASCADE,
    unit_number TEXT NOT NULL,                       -- e.g. "Flat 402" or "Room 101 - Bed A"
    floor_number INTEGER DEFAULT 0,
    sharing_type sharing_type NOT NULL DEFAULT 'FULL_FLAT',
    monthly_rent DECIMAL(10, 2) NOT NULL,
    security_deposit DECIMAL(10, 2) NOT NULL,
    status unit_status NOT NULL DEFAULT 'AVAILABLE',
    current_lease_id UUID,                           -- FK added below with ON DELETE SET NULL for O(1) status lookup
    amenities JSONB DEFAULT '[]'::jsonb,             -- Room specific amenities (e.g. Attached Balcony)
    notes TEXT,
    metadata JSONB DEFAULT '{}'::jsonb,              -- Future-proof room attributes
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- -----------------------------------------------------------------------------
-- 4. TENANTS, AGREEMENT TEMPLATES & LEASES
-- -----------------------------------------------------------------------------

-- Tenants (Renters & Residents)
CREATE TABLE IF NOT EXISTS public.tenants (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES public.organizations(id) ON DELETE CASCADE,
    owner_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    full_name TEXT NOT NULL,
    email TEXT,
    phone TEXT NOT NULL,
    alternate_phone TEXT,
    gender TEXT,
    date_of_birth DATE,
    permanent_address TEXT NOT NULL,
    occupation TEXT,                                 -- Student, Working Professional
    organization_or_college TEXT,                    -- Company / College Name
    work_address TEXT,
    emergency_contact_name TEXT,
    emergency_contact_phone TEXT,
    emergency_contact_relation TEXT,
    
    -- Identity Proofs (Compliance & AES-256 / pgp_sym_encrypt Ready)
    id_proof_type TEXT DEFAULT 'Aadhaar',            -- Aadhaar, PAN, Passport, Driving License
    id_proof_last4 VARCHAR(4),                       -- Unencrypted last 4 digits for UI display (e.g., "4921")
    id_proof_number TEXT,                            -- AES-256 / pgp_sym_encrypt encrypted string
    is_id_verified BOOLEAN DEFAULT FALSE,
    id_proof_front_url TEXT,
    id_proof_back_url TEXT,
    tenant_photo_url TEXT,
    
    metadata JSONB DEFAULT '{}'::jsonb,              -- Future-proof tenant attributes
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Agreement Templates (Multi-Template Support for PG, Flat, Commercial)
CREATE TABLE IF NOT EXISTS public.agreement_templates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES public.organizations(id) ON DELETE CASCADE,
    title TEXT NOT NULL,                             -- e.g. "Standard PG 11-Month Agreement", "Flat Leave & License"
    template_type TEXT NOT NULL DEFAULT 'PG_11_MONTH', -- PG_11_MONTH, FLAT_LEAVE_LICENSE, COMMERCIAL
    content_template TEXT NOT NULL,                  -- Markdown/HTML template text with Mustache placeholders
    is_default BOOLEAN DEFAULT FALSE,
    metadata JSONB DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Leases / Tenancy Agreements
CREATE TABLE IF NOT EXISTS public.leases (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES public.organizations(id) ON DELETE CASCADE,
    unit_id UUID NOT NULL REFERENCES public.units(id) ON DELETE RESTRICT,
    tenant_id UUID NOT NULL REFERENCES public.tenants(id) ON DELETE RESTRICT,
    owner_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    agreement_template_id UUID REFERENCES public.agreement_templates(id) ON DELETE SET NULL,
    
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    monthly_rent DECIMAL(10, 2) NOT NULL,
    security_deposit DECIMAL(10, 2) NOT NULL,
    rent_due_day INTEGER NOT NULL DEFAULT 5,         -- Day of month rent is due (e.g. 5th)
    notice_period_days INTEGER DEFAULT 30,
    lock_in_period_months INTEGER DEFAULT 6,
    custom_clauses JSONB DEFAULT '[]'::jsonb,        -- Array of custom legal clause strings
    
    status lease_status NOT NULL DEFAULT 'ACTIVE',
    agreement_pdf_url TEXT,                          -- Stored PDF agreement link
    is_esign_completed BOOLEAN DEFAULT FALSE,
    
    metadata JSONB DEFAULT '{}'::jsonb,              -- Extra lease variables
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Add Foreign Key constraint for units.current_lease_id with ON DELETE SET NULL
ALTER TABLE public.units 
    ADD CONSTRAINT fk_units_current_lease 
    FOREIGN KEY (current_lease_id) REFERENCES public.leases(id) ON DELETE SET NULL;

-- -----------------------------------------------------------------------------
-- 5. RECEIPTS, BBPS UTILITIES & POLICE VERIFICATION
-- -----------------------------------------------------------------------------

-- Receipts (Rent Payments & Security Deposit Invoices)
CREATE TABLE IF NOT EXISTS public.receipts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    receipt_number TEXT NOT NULL,                    -- Scoped per Organization (e.g. REC-2026-000001)
    organization_id UUID NOT NULL REFERENCES public.organizations(id) ON DELETE CASCADE,
    lease_id UUID NOT NULL REFERENCES public.leases(id) ON DELETE RESTRICT,
    tenant_id UUID NOT NULL REFERENCES public.tenants(id) ON DELETE RESTRICT,
    owner_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    
    type receipt_type NOT NULL DEFAULT 'RENT_PAYMENT',
    amount DECIMAL(10, 2) NOT NULL,
    payment_mode payment_mode NOT NULL DEFAULT 'UPI',
    transaction_ref TEXT,                            -- UTR Number / Cheque No. / Gateway Transaction ID
    payment_date DATE NOT NULL DEFAULT CURRENT_DATE,
    period_start DATE,                               -- e.g. 2026-10-01
    period_end DATE,                                 -- e.g. 2026-10-31
    notes TEXT,
    receipt_pdf_url TEXT,
    
    metadata JSONB DEFAULT '{}'::jsonb,              -- Custom receipt payload metadata
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- BBPS Utility Bill Transactions (Electricity, Water, Gas via BBPS / Decentro)
CREATE TABLE IF NOT EXISTS public.bbps_transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES public.organizations(id) ON DELETE CASCADE,
    tenant_id UUID NOT NULL REFERENCES public.tenants(id) ON DELETE CASCADE,
    lease_id UUID REFERENCES public.leases(id) ON DELETE SET NULL,
    
    biller_id TEXT NOT NULL,                         -- e.g. MSEDCL000MAH01
    biller_name TEXT NOT NULL,                       -- e.g. MSEDCL Electricity - Maharashtra
    customer_param_name TEXT DEFAULT 'Consumer Number',
    customer_param_value TEXT NOT NULL,              -- Consumer No e.g. 102938475612
    
    amount DECIMAL(10, 2) NOT NULL,
    bill_date DATE,
    due_date DATE,
    payment_date TIMESTAMPTZ,                        -- Exact Timestamp when payment succeeded for accounting
    status bbps_status NOT NULL DEFAULT 'BILL_FETCHED',
    
    bbps_reference_id TEXT,                          -- Official BBPS Reference Approval ID
    razorpay_payment_id TEXT,                        -- Payment Gateway Payment ID
    receipt_pdf_url TEXT,
    
    metadata JSONB DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Police Verification & Tenant Intimation Forms
CREATE TABLE IF NOT EXISTS public.police_verifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES public.organizations(id) ON DELETE CASCADE,
    lease_id UUID NOT NULL REFERENCES public.leases(id) ON DELETE CASCADE,
    tenant_id UUID NOT NULL REFERENCES public.tenants(id) ON DELETE CASCADE,
    owner_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    
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
    provider TEXT NOT NULL,                          -- e.g. "Digio", "Leegality", "NeSL"
    transaction_id TEXT NOT NULL,                    -- Provider reference token
    stamp_paper_number TEXT,                         -- State e-Stamp number
    stamp_amount DECIMAL(10, 2),
    status TEXT NOT NULL,                            -- PENDING, SIGNED, FAILED
    signed_pdf_url TEXT,
    audit_trail_json JSONB DEFAULT '{}'::jsonb,       -- Legal audit trail response
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
    tenant_id UUID REFERENCES public.tenants(id) ON DELETE SET NULL, -- Nullable for prospective leads
    
    wamid TEXT,                                      -- Meta WhatsApp Message ID (e.g. wamid.HBgMOTE5OD...)
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

-- Property Leads & CRM Enquiries (WhatsApp / Web / 99acres Portal Leads)
CREATE TABLE IF NOT EXISTS public.property_leads (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES public.organizations(id) ON DELETE CASCADE,
    property_id UUID NOT NULL REFERENCES public.properties(id) ON DELETE CASCADE,
    unit_id UUID REFERENCES public.units(id) ON DELETE SET NULL,
    
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
    title TEXT NOT NULL,                             -- e.g. "Geyser not heating"
    description TEXT,
    category TEXT DEFAULT 'PLUMBING',                -- PLUMBING, ELECTRICAL, CLEANING, WIFI
    priority TEXT DEFAULT 'MEDIUM',                  -- LOW, MEDIUM, HIGH, EMERGENCY
    status TEXT DEFAULT 'OPEN',                      -- OPEN, IN_PROGRESS, RESOLVED, CLOSED
    images TEXT[] DEFAULT '{}',                      -- Complaint photo attachments
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Third-Party API Integration Audit Logs (Surepass, Razorpay, WhatsApp, Decentro)
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

-- -----------------------------------------------------------------------------
-- 7. AUTOMATED LEASE-UNIT STATUS SYNC TRIGGER & UPDATED_AT TRIGGERS
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
CREATE TRIGGER trg_properties_updated BEFORE UPDATE ON public.properties FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER trg_units_updated BEFORE UPDATE ON public.units FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER trg_tenants_updated BEFORE UPDATE ON public.tenants FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER trg_agreement_templates_updated BEFORE UPDATE ON public.agreement_templates FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER trg_leases_updated BEFORE UPDATE ON public.leases FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER trg_receipts_updated BEFORE UPDATE ON public.receipts FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER trg_bbps_updated BEFORE UPDATE ON public.bbps_transactions FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER trg_police_verifications_updated BEFORE UPDATE ON public.police_verifications FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER trg_esign_transactions_updated BEFORE UPDATE ON public.esign_transactions FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER trg_whatsapp_logs_updated BEFORE UPDATE ON public.whatsapp_logs FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER trg_property_leads_updated BEFORE UPDATE ON public.property_leads FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER trg_maintenance_tickets_updated BEFORE UPDATE ON public.maintenance_tickets FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER trg_integration_logs_updated BEFORE UPDATE ON public.integration_logs FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- AUTOMATED TRIGGER: Syncs unit occupancy status and current_lease_id directly in DB
CREATE OR REPLACE FUNCTION sync_unit_lease_status()
RETURNS TRIGGER AS $$
BEGIN
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
        WHERE id = NEW.unit_id OR current_lease_id = NEW.id;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_sync_unit_lease_status
AFTER INSERT OR UPDATE OF status ON public.leases
FOR EACH ROW EXECUTE FUNCTION sync_unit_lease_status();

-- -----------------------------------------------------------------------------
-- 8. COMPOSITE INDEXES & UNIQUE SCOPED CONSTRAINTS
-- -----------------------------------------------------------------------------

-- Unique Receipt Number Scoped Per Organization
CREATE UNIQUE INDEX IF NOT EXISTS uniq_receipt_number_per_org ON public.receipts(organization_id, receipt_number);

-- High-Performance Composite Multi-Tenant Indexes
CREATE INDEX IF NOT EXISTS idx_leases_org_status ON public.leases(organization_id, status);
CREATE INDEX IF NOT EXISTS idx_units_property_status ON public.units(property_id, status);
CREATE INDEX IF NOT EXISTS idx_units_org_status ON public.units(organization_id, status);
CREATE INDEX IF NOT EXISTS idx_receipts_org_tenant ON public.receipts(organization_id, tenant_id);
CREATE INDEX IF NOT EXISTS idx_whatsapp_logs_org_phone ON public.whatsapp_logs(organization_id, phone_number);
CREATE INDEX IF NOT EXISTS idx_whatsapp_logs_wamid ON public.whatsapp_logs(organization_id, wamid);
CREATE INDEX IF NOT EXISTS idx_leads_org_status ON public.property_leads(organization_id, status);
CREATE INDEX IF NOT EXISTS idx_bbps_org_status ON public.bbps_transactions(organization_id, status);
CREATE INDEX IF NOT EXISTS idx_integration_logs_provider ON public.integration_logs(provider);

-- -----------------------------------------------------------------------------
-- 9. ROW LEVEL SECURITY (RLS) POLICIES
-- -----------------------------------------------------------------------------

ALTER TABLE public.organizations ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.branding_settings ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.properties ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.units ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.tenants ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.agreement_templates ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.leases ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.receipts ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.bbps_transactions ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.police_verifications ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.whatsapp_logs ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.property_leads ENABLE ROW LEVEL SECURITY;

-- Public showcase RLS rules
CREATE POLICY "Public can view active properties" ON public.properties
    FOR SELECT USING (is_active = TRUE);

CREATE POLICY "Public can view available units" ON public.units
    FOR SELECT USING (status = 'AVAILABLE');