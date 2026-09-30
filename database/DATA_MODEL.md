# 🏡 BrokerProp - Database Data Model Documentation (LOCKED & FINALIZED v3)

This document provides a comprehensive technical reference for the **BrokerProp** PostgreSQL database schema. It details every table, attribute, data type, integrity constraint, automated DB trigger, and realistic sample data for multi-tenant property & PG management.

---

## 📑 Table of Contents

1. [Enums & Custom Data Types](#1-enums--custom-data-types)
2. [1. organizations](#1-organizations)
3. [2. profiles](#2-profiles)
4. [3. branding_settings](#3-branding_settings)
5. [4. properties](#4-properties)
6. [5. units](#5-units)
7. [6. tenants](#6-tenants)
8. [7. agreement_templates](#7-agreement_templates)
9. [8. leases](#8-leases)
10. [9. receipts](#9-receipts)
11. [10. bbps_transactions](#10-bbps_transactions)
12. [11. police_verifications](#11-police_verifications)
13. [12. esign_transactions](#12-esign_transactions)
14. [13. whatsapp_logs](#13-whatsapp_logs)
15. [14. property_leads](#14-property_leads)
16. [15. maintenance_tickets](#15-maintenance_tickets)
17. [16. integration_logs](#16-integration_logs)
18. [Automated Triggers & Multi-Tenant Performance Indexes](#automated-triggers--multi-tenant-performance-indexes)

---

## 1. Enums & Custom Data Types

| Enum Name | Allowed Values | Description |
| :--- | :--- | :--- |
| `user_role` | `SUPER_ADMIN`, `OWNER_ADMIN`, `PROPERTY_MANAGER`, `STAFF_ASSISTANT`, `TENANT`, `PUBLIC_GUEST` | User permission levels in the RBAC hierarchy |
| `property_type` | `PG`, `FLAT`, `COMMERCIAL`, `HOSTEL` | Property classification |
| `sharing_type` | `SINGLE`, `DOUBLE`, `TRIPLE`, `FOUR_SHARING`, `FULL_FLAT`, `COMMERCIAL_SPACE` | Occupancy / sharing configuration for rooms/units |
| `unit_status` | `AVAILABLE`, `OCCUPIED`, `RESERVED`, `MAINTENANCE`, `DISABLED` | Inventory availability state |
| `lease_status` | `DRAFT`, `PENDING_ESIGN`, `ACTIVE`, `EXPIRED`, `TERMINATED`, `CANCELLED` | Tenancy agreement status |
| `receipt_type` | `SECURITY_DEPOSIT`, `RENT_PAYMENT`, `UTILITY_BILL`, `MAINTENANCE`, `TOKEN_BOOKING`, `OTHER` | Purpose of financial payment receipt |
| `payment_mode` | `UPI`, `BANK_TRANSFER`, `CASH`, `CHEQUE`, `RAZORPAY_ONLINE`, `OTHER` | Payment instrument used |
| `verification_status` | `NOT_STARTED`, `PENDING`, `SUBMITTED`, `VERIFIED`, `REJECTED` | Status for Police Verification / KYC checks |
| `whatsapp_direction` | `INBOUND`, `OUTBOUND` | Direction of WhatsApp message |
| `whatsapp_msg_type` | `TEXT`, `TEMPLATE`, `DOCUMENT`, `IMAGE`, `INTERACTIVE`, `LOCATION` | Type of WhatsApp payload |
| `whatsapp_status` | `SENT`, `DELIVERED`, `READ`, `FAILED` | Delivery receipt state |
| `bbps_status` | `BILL_FETCHED`, `PAYMENT_INITIATED`, `SUCCESS`, `FAILED` | State of BBPS Utility payment |
| `lead_source` | `WHATSAPP`, `WEBSITE`, `NINETYNINE_ACRES`, `NOBROKER`, `MAGICBRICKS`, `DIRECT` | Acquisition origin of prospective lead |
| `lead_status` | `NEW`, `CONTACTED`, `VISITED`, `CONVERTED`, `LOST` | Lead conversion pipeline state |

---

## 1. `organizations`
Stores top-level business entities/agencies. Supports multi-branch brokers and companies.

| Attribute / Field | Data Type | Constraints | Description | Example Data |
| :--- | :--- | :--- | :--- | :--- |
| `id` | `UUID` | `PRIMARY KEY`, `DEFAULT gen_random_uuid()` | Unique Organization Identifier | `"a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11"` |
| `name` | `TEXT` | `NOT NULL` | Operating Agency Legal Name | `"Sunshine Real Estate & PG Services"` |
| `slug` | `TEXT` | `UNIQUE`, `NOT NULL` | URL slug for white-label site | `"sunshine-properties"` |
| `is_active` | `BOOLEAN` | `DEFAULT TRUE` | Agency account status | `true` |
| `metadata` | `JSONB` | `DEFAULT '{}'::jsonb` | Extensible custom org settings | `{"plan": "pro", "max_properties": 50}` |
| `created_at` | `TIMESTAMPTZ`| `DEFAULT NOW()` | Record creation timestamp | `"2026-09-25T10:00:00Z"` |
| `updated_at` | `TIMESTAMPTZ`| `DEFAULT NOW()` | Record last updated timestamp | `"2026-09-25T10:00:00Z"` |

---

## 2. `profiles`
Stores user accounts mapped to auth provider IDs (Supabase Auth / Spring Boot Users) with role permissions.

| Attribute / Field | Data Type | Constraints | Description | Example Data |
| :--- | :--- | :--- | :--- | :--- |
| `id` | `UUID` | `PRIMARY KEY` | User Unique ID | `"b1ee...0a22"` |
| `organization_id`| `UUID` | `FOREIGN KEY` ➔ `organizations(id)` | Parent agency | `"a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11"` |
| `email` | `TEXT` | `UNIQUE`, `NOT NULL` | Account Email Address | `"owner@sunshinepg.com"` |
| `phone` | `TEXT` | - | Primary Phone Number | `"+919876543210"` |
| `full_name` | `TEXT` | `NOT NULL` | Full Name of user | `"Rajesh Sharma"` |
| `avatar_url` | `TEXT` | - | Profile Picture URL | `"https://cdn.brokerprop.com/avatars/user1.jpg"` |
| `role` | `user_role`| `NOT NULL`, `DEFAULT 'OWNER_ADMIN'` | Role in system | `"OWNER_ADMIN"` |
| `permissions` | `JSONB` | `DEFAULT '[]'::jsonb` | Custom RBAC flags | `["manage_properties", "issue_receipts"]` |
| `is_active` | `BOOLEAN` | `DEFAULT TRUE` | User account active state | `true` |
| `metadata` | `JSONB` | `DEFAULT '{}'::jsonb` | Custom metadata attributes | `{"preferred_language": "en-IN"}` |
| `created_at` | `TIMESTAMPTZ`| `DEFAULT NOW()` | Record creation timestamp | `"2026-09-25T10:00:00Z"` |
| `updated_at` | `TIMESTAMPTZ`| `DEFAULT NOW()` | Record last updated timestamp | `"2026-09-25T10:00:00Z"` |

---

## 3. `branding_settings`
Stores white-label branding, owner legal credentials, and financial payout details for each agency.

| Attribute / Field | Data Type | Constraints | Description | Example Data |
| :--- | :--- | :--- | :--- | :--- |
| `id` | `UUID` | `PRIMARY KEY`, `DEFAULT gen_random_uuid()` | Branding Record ID | `"c2ff...0b33"` |
| `organization_id`| `UUID` | `UNIQUE`, `FOREIGN KEY` ➔ `organizations(id)` | Linked Organization | `"a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11"` |
| `legal_business_name`| `TEXT` | `NOT NULL` | Legal Business Name for Agreements | `"Sunshine Hospitality & Real Estate Private Limited"` |
| `trade_name` | `TEXT` | - | Display Brand Name | `"Sunshine Stays"` |
| `owner_pan` | `TEXT` | - | Owner / Company PAN | `"ABCDE1234F"` |
| `owner_gstin` | `TEXT` | - | Owner GST Number | `"27ABCDE1234F1Z5"` |
| `rera_number` | `TEXT` | - | RERA Registration Number | `"P51800012345"` |
| `registered_office_address`| `TEXT` | - | Official Registered Address | `"Flat 101, Sunshine Heights, M.G. Road, Pune, MH 411001"` |
| `contact_phone` | `TEXT` | - | Public Support Phone | `"+912025551234"` |
| `contact_email` | `TEXT` | - | Public Support Email | `"support@sunshinepg.com"` |
| `agency_logo_url`| `TEXT` | - | Logo Image URL | `"https://cdn.brokerprop.com/logos/sunshine.png"` |
| `primary_color` | `TEXT` | `DEFAULT '#2563eb'` | Brand Primary Hex Color | `"#2563eb"` |
| `secondary_color`| `TEXT` | `DEFAULT '#1e293b'` | Brand Secondary Hex Color | `"#1e293b"` |
| `signature_url` | `TEXT` | - | Owner Digital Signature PNG URL | `"https://cdn.brokerprop.com/signatures/owner_sig.png"` |
| `owner_upi_id` | `TEXT` | - | Direct Rent Collection UPI VPA | `"sunshinepg@icici"` |
| `bank_account_number`| `TEXT` | - | Bank Account Number | `"918020011223344"` |
| `bank_ifsc_code`| `TEXT` | - | Bank IFSC Code | `"ICIC0000101"` |
| `bank_name` | `TEXT` | - | Bank Name | `"ICICI Bank"` |
| `account_holder_name`| `TEXT` | - | Account Holder Name | `"Sunshine Hospitality Pvt Ltd"` |
| `metadata` | `JSONB` | `DEFAULT '{}'::jsonb` | Custom branding metadata | `{"whatsapp_welcome_msg": "Welcome to Sunshine Stays!"}` |
| `created_at` | `TIMESTAMPTZ`| `DEFAULT NOW()` | Record creation timestamp | `"2026-09-25T10:00:00Z"` |
| `updated_at` | `TIMESTAMPTZ`| `DEFAULT NOW()` | Record last updated timestamp | `"2026-09-25T10:00:00Z"` |

---

## 4. `properties`
Stores buildings, complexes, or flat properties owned/managed by the agency.

| Attribute / Field | Data Type | Constraints | Description | Example Data |
| :--- | :--- | :--- | :--- | :--- |
| `id` | `UUID` | `PRIMARY KEY`, `DEFAULT gen_random_uuid()` | Property ID | `"d3aa...0c44"` |
| `organization_id`| `UUID` | `FOREIGN KEY` ➔ `organizations(id)` | Parent organization | `"a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11"` |
| `owner_id` | `UUID` | `FOREIGN KEY` ➔ `profiles(id)` | Managing Owner | `"b1ee...0a22"` |
| `name` | `TEXT` | `NOT NULL` | Property Name | `"GreenStays Luxury Co-Living PG"` |
| `type` | `property_type`| `NOT NULL`, `DEFAULT 'PG'` | PG vs Flat vs Commercial | `"PG"` |
| `address` | `TEXT` | `NOT NULL` | Full Street Address | `"Plot 42, Sector 15, Vashi"` |
| `city` | `TEXT` | `NOT NULL` | City Name | `"Navi Mumbai"` |
| `state` | `TEXT` | `NOT NULL` | State Name | `"Maharashtra"` |
| `pincode` | `VARCHAR(10)`| `NOT NULL` | Postal Pincode | `"400703"` |
| `landmark` | `TEXT` | - | Nearby Landmark | `"Near Vashi Railway Station"` |
| `latitude` | `DECIMAL(10,8)`| - | Map Coordinate Latitude | `19.07706500` |
| `longitude` | `DECIMAL(11,8)`| - | Map Coordinate Longitude | `72.99899300` |
| `amenities` | `JSONB` | `DEFAULT '[]'::jsonb` | Property Amenities List | `["High Speed WiFi", "AC", "Daily Housekeeping", "Power Backup", "CCTV"]` |
| `rules` | `TEXT[]` | `DEFAULT '{}'` | House Rules Array | `["No smoking inside rooms", "Visitor gate closure at 10 PM"]` |
| `images` | `TEXT[]` | `DEFAULT '{}'` | Photo Gallery Array | `["https://cdn.brokerprop.com/props/p1_front.jpg", "https://cdn.brokerprop.com/props/p1_lobby.jpg"]` |
| `description` | `TEXT` | - | Detailed description | `"Premium Co-living space for IT professionals and students."` |
| `is_active` | `BOOLEAN` | `DEFAULT TRUE` | Property listing active state| `true` |
| `metadata` | `JSONB` | `DEFAULT '{}'::jsonb` | Custom property flags | `{"ev_charging_available": true}` |
| `created_at` | `TIMESTAMPTZ`| `DEFAULT NOW()` | Record creation timestamp | `"2026-09-25T10:00:00Z"` |
| `updated_at` | `TIMESTAMPTZ`| `DEFAULT NOW()` | Record last updated timestamp | `"2026-09-25T10:00:00Z"` |

---

## 5. `units`
Stores individual PG rooms, bed slots, or full flat units inside a property.

| Attribute / Field | Data Type | Constraints | Description | Example Data |
| :--- | :--- | :--- | :--- | :--- |
| `id` | `UUID` | `PRIMARY KEY`, `DEFAULT gen_random_uuid()` | Unit / Bed ID | `"e4bb...0d55"` |
| `property_id` | `UUID` | `FOREIGN KEY` ➔ `properties(id)` | Parent Property | `"d3aa...0c44"` |
| `organization_id`| `UUID` | `FOREIGN KEY` ➔ `organizations(id)` | Parent Organization | `"a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11"` |
| `unit_number` | `TEXT` | `NOT NULL` | Room or Bed Identifier | `"Room 201 - Bed A"` |
| `floor_number` | `INTEGER` | `DEFAULT 0` | Floor Number | `2` |
| `sharing_type` | `sharing_type`| `NOT NULL`, `DEFAULT 'FULL_FLAT'` | Occupancy Type | `"DOUBLE"` |
| `monthly_rent` | `DECIMAL(10,2)`| `NOT NULL` | Monthly Rent Amount (INR) | `12000.00` |
| `security_deposit`| `DECIMAL(10,2)`| `NOT NULL` | Security Deposit Amount (INR) | `24000.00` |
| `status` | `unit_status`| `NOT NULL`, `DEFAULT 'AVAILABLE'` | Availability Status | `"AVAILABLE"` |
| `current_lease_id`| `UUID` | `FOREIGN KEY` ➔ `leases(id)` (`ON DELETE SET NULL`) | Fast O(1) active lease lookup | `"06dd...0f77"` |
| `amenities` | `JSONB` | `DEFAULT '[]'::jsonb` | Room Specific Amenities | `["Attached Balcony", "Study Desk", "Personal Locker"]` |
| `notes` | `TEXT` | - | Internal Notes | `"Corner room with garden view"` |
| `metadata` | `JSONB` | `DEFAULT '{}'::jsonb` | Custom room variables | `{"meter_type": "SUB_METER"}` |
| `created_at` | `TIMESTAMPTZ`| `DEFAULT NOW()` | Record creation timestamp | `"2026-09-25T10:00:00Z"` |
| `updated_at` | `TIMESTAMPTZ`| `DEFAULT NOW()` | Record last updated timestamp | `"2026-09-25T10:00:00Z"` |

---

## 6. `tenants`
Stores resident profiles, emergency contacts, and verified KYC information (with AES-256 / `pgp_sym_encrypt` encryption for document compliance).

| Attribute / Field | Data Type | Constraints | Description | Example Data |
| :--- | :--- | :--- | :--- | :--- |
| `id` | `UUID` | `PRIMARY KEY`, `DEFAULT gen_random_uuid()` | Tenant ID | `"f5cc...0e66"` |
| `organization_id`| `UUID` | `FOREIGN KEY` ➔ `organizations(id)` | Parent Organization | `"a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11"` |
| `owner_id` | `UUID` | `FOREIGN KEY` ➔ `profiles(id)` | Assigned Broker/Owner | `"b1ee...0a22"` |
| `full_name` | `TEXT` | `NOT NULL` | Tenant Full Name | `"Aarav Mehta"` |
| `email` | `TEXT` | - | Tenant Email Address | `"aarav.mehta@gmail.com"` |
| `phone` | `TEXT` | `NOT NULL` | Tenant Mobile Number | `"+919812345678"` |
| `alternate_phone`| `TEXT` | - | Alternate Phone Number | `"+919812345679"` |
| `gender` | `TEXT` | - | Gender | `"Male"` |
| `date_of_birth` | `DATE` | - | Date of Birth | `"2001-05-14"` |
| `permanent_address`| `TEXT` | `NOT NULL` | Permanent Home Address | `"House 12, Civil Lines, Jaipur, Rajasthan 302006"` |
| `occupation` | `TEXT` | - | Occupation / Profession | `"Software Engineer"` |
| `organization_or_college`| `TEXT` | - | Employer / University Name | `"Infosys Limited"` |
| `work_address` | `TEXT` | - | Work / College Address | `"Phase 1, Hinjewadi IT Park, Pune"` |
| `emergency_contact_name`| `TEXT` | - | Emergency Contact Name | `"Suresh Mehta"` |
| `emergency_contact_phone`| `TEXT` | - | Emergency Contact Phone | `"+919414012345"` |
| `emergency_contact_relation`| `TEXT` | - | Relationship | `"Father"` |
| `id_proof_type` | `TEXT` | `DEFAULT 'Aadhaar'` | Document Type | `"Aadhaar"` |
| `id_proof_last4` | `VARCHAR(4)`| - | Unencrypted last 4 digits for UI | `"4921"` |
| `id_proof_number`| `TEXT` | - | Encrypted document ID (AES-256) | `"pgp:encrypted:a8f912..."` |
| `is_id_verified`| `BOOLEAN` | `DEFAULT FALSE` | Surepass/Digio verification status | `true` |
| `id_proof_front_url`| `TEXT` | - | Aadhaar Front Scan URL | `"https://cdn.brokerprop.com/docs/t1_aadhaar_front.pdf"` |
| `id_proof_back_url`| `TEXT` | - | Aadhaar Back Scan URL | `"https://cdn.brokerprop.com/docs/t1_aadhaar_back.pdf"` |
| `tenant_photo_url`| `TEXT` | - | Passport Photo URL | `"https://cdn.brokerprop.com/docs/t1_photo.jpg"` |
| `metadata` | `JSONB` | `DEFAULT '{}'::jsonb` | Custom tenant metadata | `{"vehicle_number": "MH-12-AB-1234"}` |
| `created_at` | `TIMESTAMPTZ`| `DEFAULT NOW()` | Record creation timestamp | `"2026-09-25T10:00:00Z"` |
| `updated_at` | `TIMESTAMPTZ`| `DEFAULT NOW()` | Record last updated timestamp | `"2026-09-25T10:00:00Z"` |

---

## 7. `agreement_templates`
Stores multi-template configurations for legal rent agreements (e.g. 11-Month PG, Flat Leave & License, Commercial).

| Attribute / Field | Data Type | Constraints | Description | Example Data |
| :--- | :--- | :--- | :--- | :--- |
| `id` | `UUID` | `PRIMARY KEY`, `DEFAULT gen_random_uuid()` | Template ID | `"77aa...1500"` |
| `organization_id`| `UUID` | `FOREIGN KEY` ➔ `organizations(id)` | Parent Organization | `"a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11"` |
| `title` | `TEXT` | `NOT NULL` | Template Name | `"Standard 11-Month PG Agreement"` |
| `template_type` | `TEXT` | `NOT NULL`, `DEFAULT 'PG_11_MONTH'` | Template Category | `"PG_11_MONTH"` |
| `content_template`| `TEXT` | `NOT NULL` | Markdown / HTML with Mustache tokens | `"# RENT AGREEMENT\n\nThis agreement is made between {{landlord_name}} and {{tenant_name}}..."` |
| `is_default` | `BOOLEAN` | `DEFAULT FALSE` | Default selection flag | `true` |
| `metadata` | `JSONB` | `DEFAULT '{}'::jsonb` | Custom template variables | `{"notice_period_default": 30}` |
| `created_at` | `TIMESTAMPTZ`| `DEFAULT NOW()` | Record creation timestamp | `"2026-09-25T10:00:00Z"` |
| `updated_at` | `TIMESTAMPTZ`| `DEFAULT NOW()` | Record last updated timestamp | `"2026-09-25T10:00:00Z"` |

---

## 8. `leases`
Stores 11-month or custom tenancy agreements, rental terms, template links, and signed agreement document paths.

| Attribute / Field | Data Type | Constraints | Description | Example Data |
| :--- | :--- | :--- | :--- | :--- |
| `id` | `UUID` | `PRIMARY KEY`, `DEFAULT gen_random_uuid()` | Lease ID | `"06dd...0f77"` |
| `organization_id`| `UUID` | `FOREIGN KEY` ➔ `organizations(id)` | Parent Organization | `"a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11"` |
| `unit_id` | `UUID` | `FOREIGN KEY` ➔ `units(id)` | Assigned Unit / Bed | `"e4bb...0d55"` |
| `tenant_id` | `UUID` | `FOREIGN KEY` ➔ `tenants(id)` | Renter / Resident | `"f5cc...0e66"` |
| `owner_id` | `UUID` | `FOREIGN KEY` ➔ `profiles(id)` | Landlord / Broker | `"b1ee...0a22"` |
| `agreement_template_id`| `UUID`| `FOREIGN KEY` ➔ `agreement_templates(id)` | Linked Template | `"77aa...1500"` |
| `start_date` | `DATE` | `NOT NULL` | Tenancy Start Date | `"2026-10-01"` |
| `end_date` | `DATE` | `NOT NULL` | Tenancy End Date | `"2027-08-31"` |
| `monthly_rent` | `DECIMAL(10,2)`| `NOT NULL` | Monthly Rent Amount | `12000.00` |
| `security_deposit`| `DECIMAL(10,2)`| `NOT NULL` | Deposit Held Amount | `24000.00` |
| `rent_due_day` | `INTEGER` | `NOT NULL`, `DEFAULT 5` | Monthly Rent Due Day | `5` |
| `notice_period_days`| `INTEGER` | `DEFAULT 30` | Notice Period (Days) | `30` |
| `lock_in_period_months`| `INTEGER`| `DEFAULT 6` | Lock-in Period (Months) | `6` |
| `custom_clauses`| `JSONB` | `DEFAULT '[]'::jsonb` | Custom Legal Clauses | `["Electricity charged at ₹10 per unit via sub-meter", "No loud music after 10 PM"]` |
| `status` | `lease_status`| `NOT NULL`, `DEFAULT 'ACTIVE'`| Lease Lifecycle State | `"ACTIVE"` |
| `agreement_pdf_url`| `TEXT` | - | Generated PDF Agreement URL | `"https://cdn.brokerprop.com/agreements/lease_2026_001.pdf"` |
| `is_esign_completed`| `BOOLEAN`| `DEFAULT FALSE` | Aadhaar OTP e-Sign status | `true` |
| `metadata` | `JSONB` | `DEFAULT '{}'::jsonb` | Custom lease variables | `{"witness_name": "Suresh Kumar"}` |
| `created_at` | `TIMESTAMPTZ`| `DEFAULT NOW()` | Record creation timestamp | `"2026-09-25T10:00:00Z"` |
| `updated_at` | `TIMESTAMPTZ`| `DEFAULT NOW()` | Record last updated timestamp | `"2026-09-25T10:00:00Z"` |

---

## 9. `receipts`
Stores financial payment receipts for rent, security deposits, and maintenance, uniquely scoped per organization.

| Attribute / Field | Data Type | Constraints | Description | Example Data |
| :--- | :--- | :--- | :--- | :--- |
| `id` | `UUID` | `PRIMARY KEY`, `DEFAULT gen_random_uuid()` | Receipt Record ID | `"17ee...1088"` |
| `receipt_number` | `TEXT` | `NOT NULL` | Org-Scoped Receipt Number | `"REC-2026-000001"` |
| `organization_id`| `UUID` | `FOREIGN KEY` ➔ `organizations(id)` | Parent Organization | `"a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11"` |
| `lease_id` | `UUID` | `FOREIGN KEY` ➔ `leases(id)` | Associated Lease | `"06dd...0f77"` |
| `tenant_id` | `UUID` | `FOREIGN KEY` ➔ `tenants(id)` | Payer Tenant | `"f5cc...0e66"` |
| `owner_id` | `UUID` | `FOREIGN KEY` ➔ `profiles(id)` | Recipient Owner | `"b1ee...0a22"` |
| `type` | `receipt_type`| `NOT NULL`, `DEFAULT 'RENT_PAYMENT'` | Purpose of Receipt | `"RENT_PAYMENT"` |
| `amount` | `DECIMAL(10,2)`| `NOT NULL` | Amount Paid (INR) | `12000.00` |
| `payment_mode` | `payment_mode`| `NOT NULL`, `DEFAULT 'UPI'` | Instrument Used | `"UPI"` |
| `transaction_ref`| `TEXT` | - | UTR / Bank Ref / Gateway ID | `"UPI/626910294812/Paytm"` |
| `payment_date` | `DATE` | `NOT NULL`, `DEFAULT CURRENT_DATE` | Date Received | `"2026-10-02"` |
| `period_start` | `DATE` | - | Billing Period Start Date | `"2026-10-01"` |
| `period_end` | `DATE` | - | Billing Period End Date | `"2026-10-31"` |
| `notes` | `TEXT` | - | Receipt Remarks | `"Rent for October 2026 paid via UPI"` |
| `receipt_pdf_url`| `TEXT` | - | Generated PDF Receipt URL | `"https://cdn.brokerprop.com/receipts/REC-2026-000001.pdf"` |
| `metadata` | `JSONB` | `DEFAULT '{}'::jsonb` | Custom payment payload | `{"razorpay_payment_id": "pay_L8x92aK"}` |
| `created_at` | `TIMESTAMPTZ`| `DEFAULT NOW()` | Record creation timestamp | `"2026-09-25T10:00:00Z"` |
| `updated_at` | `TIMESTAMPTZ`| `DEFAULT NOW()` | Record last updated timestamp | `"2026-09-25T10:00:00Z"` |

---

## 10. `bbps_transactions`
Stores Bharat Bill Payment System (BBPS) utility bill payments (Electricity, Water, Gas via BBPS / Decentro / Razorpay).

| Attribute / Field | Data Type | Constraints | Description | Example Data |
| :--- | :--- | :--- | :--- | :--- |
| `id` | `UUID` | `PRIMARY KEY`, `DEFAULT gen_random_uuid()` | BBPS Transaction ID | `"88bb...1600"` |
| `organization_id`| `UUID` | `FOREIGN KEY` ➔ `organizations(id)` | Parent Organization | `"a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11"` |
| `tenant_id` | `UUID` | `FOREIGN KEY` ➔ `tenants(id)` | Paying Tenant | `"f5cc...0e66"` |
| `lease_id` | `UUID` | `FOREIGN KEY` ➔ `leases(id)` | Associated Lease | `"06dd...0f77"` |
| `biller_id` | `TEXT` | `NOT NULL` | BBPS Biller Code | `"MSEDCL000MAH01"` |
| `biller_name` | `TEXT` | `NOT NULL` | Utility Provider Name | `"MSEDCL Electricity - Maharashtra"` |
| `customer_param_name`| `TEXT` | `DEFAULT 'Consumer Number'` | Consumer Number Label | `"Consumer Number"` |
| `customer_param_value`| `TEXT`| `NOT NULL` | Consumer / Account Number | `"102938475612"` |
| `amount` | `DECIMAL(10,2)`| `NOT NULL` | Utility Bill Amount (INR) | `1450.00` |
| `bill_date` | `DATE` | - | Bill Date | `"2026-09-20"` |
| `due_date` | `DATE` | - | Due Date | `"2026-10-05"` |
| `payment_date` | `TIMESTAMPTZ`| - | Exact Payment Timestamp | `"2026-10-02T16:20:00Z"` |
| `status` | `bbps_status`| `NOT NULL`, `DEFAULT 'BILL_FETCHED'`| Transaction Status | `"SUCCESS"` |
| `bbps_reference_id`| `TEXT` | - | BBPS Official Reference ID | `"BBPSMH202610029812"` |
| `razorpay_payment_id`| `TEXT` | - | Payment Gateway ID | `"pay_L9y91bM"` |
| `receipt_pdf_url`| `TEXT` | - | Utility Payment Receipt URL | `"https://cdn.brokerprop.com/bbps/bbps_rec_88bb.pdf"` |
| `metadata` | `JSONB` | `DEFAULT '{}'::jsonb` | Custom payload metadata | `{}` |
| `created_at` | `TIMESTAMPTZ`| `DEFAULT NOW()` | Record creation timestamp | `"2026-09-25T10:00:00Z"` |
| `updated_at` | `TIMESTAMPTZ`| `DEFAULT NOW()` | Record last updated timestamp | `"2026-09-25T10:00:00Z"` |

---

## 11. `police_verifications`
Stores tenant intimation forms and police station verification tracking.

| Attribute / Field | Data Type | Constraints | Description | Example Data |
| :--- | :--- | :--- | :--- | :--- |
| `id` | `UUID` | `PRIMARY KEY`, `DEFAULT gen_random_uuid()` | Record ID | `"28ff...1199"` |
| `organization_id`| `UUID` | `FOREIGN KEY` ➔ `organizations(id)` | Parent Organization | `"a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11"` |
| `lease_id` | `UUID` | `FOREIGN KEY` ➔ `leases(id)` | Associated Lease | `"06dd...0f77"` |
| `tenant_id` | `UUID` | `FOREIGN KEY` ➔ `tenants(id)` | Tenant | `"f5cc...0e66"` |
| `owner_id` | `UUID` | `FOREIGN KEY` ➔ `profiles(id)` | Landlord / Property Owner | `"b1ee...0a22"` |
| `police_station_name`| `TEXT` | `NOT NULL` | Local Police Station | `"Vashi Police Station"` |
| `jurisdiction_district`| `TEXT`| `NOT NULL` | Police District | `"Navi Mumbai Police Commissionerate"` |
| `status` | `verification_status`| `NOT NULL`, `DEFAULT 'NOT_STARTED'` | Intimation State | `"SUBMITTED"` |
| `submission_date`| `DATE` | - | Date Submitted to Police | `"2026-10-03"` |
| `application_reference_no`| `TEXT`| - | Police Portal Ack Number | `"POL-MH-2026-98123"` |
| `verification_pdf_url`| `TEXT` | - | Generated Form / Ack PDF URL | `"https://cdn.brokerprop.com/police/pv_form_001.pdf"` |
| `created_at` | `TIMESTAMPTZ`| `DEFAULT NOW()` | Record creation timestamp | `"2026-09-25T10:00:00Z"` |
| `updated_at` | `TIMESTAMPTZ`| `DEFAULT NOW()` | Record last updated timestamp | `"2026-09-25T10:00:00Z"` |

---

## 12. `esign_transactions`
Stores e-Stamp paper procurement details and Aadhaar OTP e-Sign transaction audit logs (Digio/Leegality).

| Attribute / Field | Data Type | Constraints | Description | Example Data |
| :--- | :--- | :--- | :--- | :--- |
| `id` | `UUID` | `PRIMARY KEY`, `DEFAULT gen_random_uuid()` | E-Sign Transaction ID | `"39aa...1200"` |
| `organization_id`| `UUID` | `FOREIGN KEY` ➔ `organizations(id)` | Parent Organization | `"a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11"` |
| `lease_id` | `UUID` | `FOREIGN KEY` ➔ `leases(id)` | Target Agreement Lease | `"06dd...0f77"` |
| `provider` | `TEXT` | `NOT NULL` | Integration Provider Name | `"Digio"` |
| `transaction_id` | `TEXT` | `NOT NULL` | Provider API Reference Token | `"DID261002154210982"` |
| `stamp_paper_number`| `TEXT` | - | Government e-Stamp Number | `"IN-MH981230481234L"` |
| `stamp_amount` | `DECIMAL(10,2)`| - | E-Stamp Paper Value (INR) | `100.00` |
| `status` | `TEXT` | `NOT NULL` | Transaction Status | `"SIGNED"` |
| `signed_pdf_url` | `TEXT` | - | Digitally Signed PDF URL | `"https://cdn.brokerprop.com/signed/lease_signed_001.pdf"` |
| `audit_trail_json`| `JSONB` | `DEFAULT '{}'::jsonb` | Legal Audit Trail Response | `{"signer_ip": "103.21.12.4", "signed_at": "2026-10-02T14:32:00Z"}` |
| `created_at` | `TIMESTAMPTZ`| `DEFAULT NOW()` | Record creation timestamp | `"2026-09-25T10:00:00Z"` |
| `updated_at` | `TIMESTAMPTZ`| `DEFAULT NOW()` | Record last updated timestamp | `"2026-09-25T10:00:00Z"` |

---

## 13. `whatsapp_logs`
Stores Meta WhatsApp Business Cloud API communication audit trail and delivery receipts.

| Attribute / Field | Data Type | Constraints | Description | Example Data |
| :--- | :--- | :--- | :--- | :--- |
| `id` | `UUID` | `PRIMARY KEY`, `DEFAULT gen_random_uuid()` | Log ID | `"99cc...1700"` |
| `organization_id`| `UUID` | `FOREIGN KEY` ➔ `organizations(id)` | Parent Organization | `"a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11"` |
| `tenant_id` | `UUID` | `FOREIGN KEY` ➔ `tenants(id)` (`ON DELETE SET NULL`)| Tenant (Nullable for leads) | `"f5cc...0e66"` |
| `wamid` | `TEXT` | - | Meta WhatsApp Message ID | `"wamid.HBgMOTE5ODc2NTQzMjEwFQIAERgSQ0E1N..."` |
| `phone_number` | `TEXT` | `NOT NULL` | Recipient Phone Number | `"+919812345678"` |
| `direction` | `whatsapp_direction`| `NOT NULL`, `DEFAULT 'OUTBOUND'` | Message Flow Direction | `"OUTBOUND"` |
| `type` | `whatsapp_msg_type`| `NOT NULL`, `DEFAULT 'TEXT'` | Message Type | `"TEMPLATE"` |
| `message_body` | `TEXT` | - | Rendered Message Text | `"Hi Aarav, your rent receipt REC-2026-001 is attached."` |
| `template_name` | `TEXT` | - | WhatsApp Template Name | `"rent_due_reminder_v1"` |
| `status` | `whatsapp_status`| `NOT NULL`, `DEFAULT 'SENT'` | Delivery Status | `"READ"` |
| `error_details` | `JSONB` | `DEFAULT '{}'::jsonb` | Delivery failure error details | `{}` |
| `created_at` | `TIMESTAMPTZ`| `DEFAULT NOW()` | Record creation timestamp | `"2026-09-25T10:00:00Z"` |
| `updated_at` | `TIMESTAMPTZ`| `DEFAULT NOW()` | Record last updated timestamp | `"2026-09-25T10:00:00Z"` |

---

## 14. `property_leads`
Stores CRM enquiries and leads captured via WhatsApp, white-label website, 99acres, or NoBroker.

| Attribute / Field | Data Type | Constraints | Description | Example Data |
| :--- | :--- | :--- | :--- | :--- |
| `id` | `UUID` | `PRIMARY KEY`, `DEFAULT gen_random_uuid()` | Lead ID | `"aa11...1800"` |
| `organization_id`| `UUID` | `FOREIGN KEY` ➔ `organizations(id)` | Parent Organization | `"a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11"` |
| `property_id` | `UUID` | `FOREIGN KEY` ➔ `properties(id)` | Target Property | `"d3aa...0c44"` |
| `unit_id` | `UUID` | `FOREIGN KEY` ➔ `units(id)` (`ON DELETE SET NULL`) | Specific Room / Bed Slot | `"e4bb...0d55"` |
| `name` | `TEXT` | `NOT NULL` | Prospective Lead Name | `"Vikram Singh"` |
| `phone` | `TEXT` | `NOT NULL` | Contact Number | `"+919876501234"` |
| `email` | `TEXT` | - | Contact Email | `"vikram.s@gmail.com"` |
| `source` | `lead_source`| `NOT NULL`, `DEFAULT 'WHATSAPP'` | Acquisition Channel | `"WHATSAPP"` |
| `status` | `lead_status`| `NOT NULL`, `DEFAULT 'NEW'` | CRM Funnel State | `"VISITED"` |
| `notes` | `TEXT` | - | Lead Notes / Preference | `"Looking for Double Sharing room near Vashi"` |
| `follow_up_date`| `TIMESTAMPTZ`| - | Scheduled Follow-up | `"2026-10-05T11:00:00Z"` |
| `created_at` | `TIMESTAMPTZ`| `DEFAULT NOW()` | Record creation timestamp | `"2026-09-25T10:00:00Z"` |
| `updated_at` | `TIMESTAMPTZ`| `DEFAULT NOW()` | Record last updated timestamp | `"2026-09-25T10:00:00Z"` |

---

## 15. `maintenance_tickets`
Stores resident maintenance requests (e.g. Plumbing, Electrical, WiFi).

| Attribute / Field | Data Type | Constraints | Description | Example Data |
| :--- | :--- | :--- | :--- | :--- |
| `id` | `UUID` | `PRIMARY KEY`, `DEFAULT gen_random_uuid()` | Ticket ID | `"40bb...1311"` |
| `organization_id`| `UUID` | `FOREIGN KEY` ➔ `organizations(id)` | Parent Organization | `"a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11"` |
| `unit_id` | `UUID` | `FOREIGN KEY` ➔ `units(id)` | Affected Room / Bed | `"e4bb...0d55"` |
| `tenant_id` | `UUID` | `FOREIGN KEY` ➔ `tenants(id)` | Reporting Tenant | `"f5cc...0e66"` |
| `title` | `TEXT` | `NOT NULL` | Complaint Title | `"Geyser not heating water"` |
| `description` | `TEXT` | - | Detailed description | `"The bathroom geyser indicator turns on but water stays cold."` |
| `category` | `TEXT` | `DEFAULT 'PLUMBING'` | Category | `"ELECTRICAL"` |
| `priority` | `TEXT` | `DEFAULT 'MEDIUM'` | Priority Level | `"HIGH"` |
| `status` | `TEXT` | `DEFAULT 'OPEN'` | Resolution State | `"IN_PROGRESS"` |
| `images` | `TEXT[]` | `DEFAULT '{}'` | Photo Attachments | `["https://cdn.brokerprop.com/complaints/c1_geyser.jpg"]` |
| `created_at` | `TIMESTAMPTZ`| `DEFAULT NOW()` | Record creation timestamp | `"2026-09-25T10:00:00Z"` |
| `updated_at` | `TIMESTAMPTZ`| `DEFAULT NOW()` | Record last updated timestamp | `"2026-09-25T10:00:00Z"` |

---

## 16. `integration_logs`
Stores audit logs for third-party API interactions (Surepass Aadhaar/PAN, Razorpay Payments, Meta WhatsApp API, Decentro BBPS).

| Attribute / Field | Data Type | Constraints | Description | Example Data |
| :--- | :--- | :--- | :--- | :--- |
| `id` | `UUID` | `PRIMARY KEY`, `DEFAULT gen_random_uuid()` | Log ID | `"51cc...1422"` |
| `organization_id`| `UUID` | `FOREIGN KEY` ➔ `organizations(id)` | Parent Organization | `"a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11"` |
| `provider` | `TEXT` | `NOT NULL` | Provider Name | `"SUREPASS"` |
| `endpoint` | `TEXT` | `NOT NULL` | API Endpoint Called | `"/v1/aadhaar-v2/generate-otp"` |
| `request_payload`| `JSONB` | `DEFAULT '{}'::jsonb` | Sent JSON Request Body | `{"aadhaar_number": "XXXX-XXXX-4921"}` |
| `response_payload`| `JSONB`| `DEFAULT '{}'::jsonb` | Received JSON Response | `{"status": "success", "client_id": "sp_98123"}` |
| `status_code` | `INTEGER` | - | HTTP Response Code | `200` |
| `execution_time_ms`| `INTEGER`| - | API Roundtrip Latency (ms) | `342` |
| `error_message` | `TEXT` | - | Error Message if failed | `null` |
| `created_at` | `TIMESTAMPTZ`| `DEFAULT NOW()` | Record creation timestamp | `"2026-09-25T10:00:00Z"` |
| `updated_at` | `TIMESTAMPTZ`| `DEFAULT NOW()` | Record last updated timestamp | `"2026-09-25T10:00:00Z"` |

---

## Automated Triggers & Multi-Tenant Performance Indexes

1. **Automated Unit Lease Status Sync Trigger (`trg_sync_unit_lease_status`)**:
   - When a lease becomes `ACTIVE`, Postgres automatically marks `units.status = 'OCCUPIED'` and sets `units.current_lease_id = NEW.id`.
   - When a lease becomes `EXPIRED`, `TERMINATED`, or `CANCELLED`, Postgres automatically frees the unit: `units.status = 'AVAILABLE'` and `units.current_lease_id = NULL`.
2. **Organization-Scoped Unique Receipts (`uniq_receipt_number_per_org`)**:
   - `CREATE UNIQUE INDEX uniq_receipt_number_per_org ON receipts(organization_id, receipt_number);`
3. **High-Speed Composite Indexes**:
   - `idx_leases_org_status` ON `leases(organization_id, status)`
   - `idx_units_property_status` ON `units(property_id, status)`
   - `idx_receipts_org_tenant` ON `receipts(organization_id, tenant_id)`
   - `idx_whatsapp_logs_org_phone` ON `whatsapp_logs(organization_id, phone_number)`
