-- =============================================================================
-- BrokerProp - Production Data Model & Database Schema
-- Multi-Tenant, Future-Proof PostgreSQL / Supabase Migration Script
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
    'SUPER_ADMIN',        -- BrokerProp Platform Administrator
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
    amenities JSONB DEFAULT '[]'::jsonb,             -- Room specific amenities (e.g. Attached Balcony)
    notes TEXT,
    metadata JSONB DEFAULT '{}'::jsonb,              -- Future-proof room attributes
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- -----------------------------------------------------------------------------
-- 4. TENANTS & KYC MANAGEMENT TABLES
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
    
    -- Verified Identity Proofs
    id_proof_type TEXT DEFAULT 'Aadhaar',            -- Aadhaar, PAN, Passport, Driving License
    id_proof_number TEXT,
    is_id_verified BOOLEAN DEFAULT FALSE,
    id_proof_front_url TEXT,
    id_proof_back_url TEXT,
    tenant_photo_url TEXT,
    
    metadata JSONB DEFAULT '{}'::jsonb,              -- Future-proof tenant attributes
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- -----------------------------------------------------------------------------
-- 5. LEASES, RECEIPTS & POLICE VERIFICATION TABLES
-- -----------------------------------------------------------------------------

-- Leases / Tenancy Agreements
CREATE TABLE IF NOT EXISTS public.leases (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES public.organizations(id) ON DELETE CASCADE,
    unit_id UUID NOT NULL REFERENCES public.units(id) ON DELETE RESTRICT,
    tenant_id UUID NOT NULL REFERENCES public.tenants(id) ON DELETE RESTRICT,
    owner_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    
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

-- Receipts (Rent Payments & Security Deposit Invoices)
CREATE TABLE IF NOT EXISTS public.receipts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    receipt_number TEXT UNIQUE NOT NULL,             -- e.g. REC-2026-00001
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
    created_at TIMESTAMPTZ DEFAULT NOW()
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
-- 6. NOTIFICATIONS, MAINTENANCE & AUDIT LOGS
-- -----------------------------------------------------------------------------

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

-- Third-Party API Integration Audit Logs (Surepass, Razorpay, WhatsApp)
CREATE TABLE IF NOT EXISTS public.integration_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID REFERENCES public.organizations(id) ON DELETE CASCADE,
    provider TEXT NOT NULL,                          -- "SUREPASS", "RAZORPAY", "WHATSAPP", "DIGIO"
    endpoint TEXT NOT NULL,
    request_payload JSONB DEFAULT '{}'::jsonb,
    response_payload JSONB DEFAULT '{}'::jsonb,
    status_code INTEGER,
    execution_time_ms INTEGER,
    error_message TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- -----------------------------------------------------------------------------
-- 7. AUTOMATED SEQUENCES, TRIGGERS & RLS POLICIES
-- -----------------------------------------------------------------------------

-- Auto Receipt Sequence Number Generator
CREATE SEQUENCE IF NOT EXISTS receipt_number_seq START WITH 1001;

CREATE OR REPLACE FUNCTION generate_receipt_number()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.receipt_number IS NULL OR NEW.receipt_number = '' THEN
        NEW.receipt_number := 'REC-' || TO_CHAR(CURRENT_DATE, 'YYYY') || '-' || LPAD(NEXTVAL('receipt_number_seq')::TEXT, 6, '0');
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_generate_receipt_number
BEFORE INSERT ON public.receipts
FOR EACH ROW EXECUTE FUNCTION generate_receipt_number();

-- Timestamp Update Trigger Function
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Attach Updated_At Triggers
CREATE TRIGGER trg_organizations_updated BEFORE UPDATE ON public.organizations FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER trg_profiles_updated BEFORE UPDATE ON public.profiles FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER trg_branding_updated BEFORE UPDATE ON public.branding_settings FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER trg_properties_updated BEFORE UPDATE ON public.properties FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER trg_units_updated BEFORE UPDATE ON public.units FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER trg_tenants_updated BEFORE UPDATE ON public.tenants FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER trg_leases_updated BEFORE UPDATE ON public.leases FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- -----------------------------------------------------------------------------
-- 8. INDEXES FOR HIGH-PERFORMANCE SEARCH & FILTERING
-- -----------------------------------------------------------------------------

CREATE INDEX IF NOT EXISTS idx_properties_org ON public.properties(organization_id);
CREATE INDEX IF NOT EXISTS idx_units_property ON public.units(property_id);
CREATE INDEX IF NOT EXISTS idx_units_status ON public.units(status);
CREATE INDEX IF NOT EXISTS idx_tenants_org ON public.tenants(organization_id);
CREATE INDEX IF NOT EXISTS idx_tenants_phone ON public.tenants(phone);
CREATE INDEX IF NOT EXISTS idx_leases_unit ON public.leases(unit_id);
CREATE INDEX IF NOT EXISTS idx_leases_tenant ON public.leases(tenant_id);
CREATE INDEX IF NOT EXISTS idx_receipts_lease ON public.receipts(lease_id);
CREATE INDEX IF NOT EXISTS idx_receipts_number ON public.receipts(receipt_number);
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
ALTER TABLE public.leases ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.receipts ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.police_verifications ENABLE ROW LEVEL SECURITY;

-- Allow public read access to active properties for white-label showcase
CREATE POLICY "Public can view active properties" ON public.properties
    FOR SELECT USING (is_active = TRUE);

-- Allow public read access to available units for booking
CREATE POLICY "Public can view available units" ON public.units
    FOR SELECT USING (status = 'AVAILABLE');
