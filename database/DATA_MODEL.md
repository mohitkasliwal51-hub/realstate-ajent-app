# 🏡 BrokerProp - Database Data Model Documentation

This document provides a comprehensive technical reference for the **BrokerProp** PostgreSQL database schema. It details every table, attribute, data type, integrity constraint, and realistic sample data for multi-tenant property & PG management.

---

## 📑 Table of Contents

1. [Enums & Custom Data Types](#1-enums--custom-data-types)
2. [1. organizations](#1-organizations)
3. [2. profiles](#2-profiles)
4. [3. branding_settings](#3-branding_settings)
5. [4. properties](#4-properties)
6. [5. units](#5-units)
7. [6. tenants](#6-tenants)
8. [7. leases](#7-leases)
9. [8. receipts](#8-receipts)
10. [9. police_verifications](#9-police_verifications)
11. [10. esign_transactions](#10-esign_transactions)
12. [11. maintenance_tickets](#11-maintenance_tickets)
13. [12. integration_logs](#12-integration_logs)

---

## 1. Enums & Custom Data Types

| Enum Name | Allowed Values | Description |
| :--- | :--- | :--- |
| `user_role` | `SUPER_ADMIN`, `OWNER_ADMIN`, `PROPERTY_MANAGER`, `STAFF_ASSISTANT`, `TENANT`, `PUBLIC_GUEST` | User permission levels in the RBAC hierarchy |
| `property_type` | `PG`, `FULL_FLAT`, `COMMERCIAL`, `HOSTEL` | Property classification |
| `sharing_type` | `SINGLE`, `DOUBLE`, `TRIPLE`, `FOUR_SHARING`, `FULL_FLAT`, `CUSTOM` | Occupancy / sharing configuration for rooms/units |
| `unit_status` | `AVAILABLE`, `OCCUPIED`, `RESERVED`, `MAINTENANCE`, `DISABLED` | Inventory availability state |
| `lease_status` | `DRAFT`, `PENDING_ESIGN`, `ACTIVE`, `EXPIRED`, `TERMINATED`, `CANCELLED` | Tenancy agreement status |
| `receipt_type` | `SECURITY_DEPOSIT`, `RENT_PAYMENT`, `UTILITY_BILL`, `MAINTENANCE`, `TOKEN_BOOKING`, `OTHER` | Purpose of financial payment receipt |
| `payment_mode` | `UPI`, `BANK_TRANSFER`, `CASH`, `CHEQUE`, `RAZORPAY_ONLINE`, `OTHER` | Payment instrument used |
| `verification_status` | `NOT_STARTED`, `PENDING`, `SUBMITTED`, `VERIFIED`, `REJECTED` | Status for Police Verification / KYC checks |

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
| `amenities` | `JSONB` | `DEFAULT '[]'::jsonb` | Room Specific Amenities | `["Attached Balcony", "Study Desk", "Personal Locker"]` |
| `notes` | `TEXT` | - | Internal Notes | `"Corner room with garden view"` |
| `metadata` | `JSONB` | `DEFAULT '{}'::jsonb` | Custom room variables | `{"meter_type": "SUB_METER"}` |

---

## 6. `tenants`
Stores resident profiles, emergency contacts, and verified KYC information.

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
| `id_proof_number`| `TEXT` | - | Aadhaar / PAN Number | `"XXXX-XXXX-4921"` |
| `is_id_verified`| `BOOLEAN` | `DEFAULT FALSE` | Surepass/Digio verification status | `true` |
| `id_proof_front_url`| `TEXT` | - | Aadhaar Front Scan URL | `"https://cdn.brokerprop.com/docs/t1_aadhaar_front.pdf"` |
| `id_proof_back_url`| `TEXT` | - | Aadhaar Back Scan URL | `"https://cdn.brokerprop.com/docs/t1_aadhaar_back.pdf"` |
| `tenant_photo_url`| `TEXT` | - | Passport Photo URL | `"https://cdn.brokerprop.com/docs/t1_photo.jpg"` |
| `metadata` | `JSONB` | `DEFAULT '{}'::jsonb` | Custom tenant metadata | `{"vehicle_number": "MH-12-AB-1234"}` |

---

## 7. `leases`
Stores 11-month or custom tenancy agreements, rental terms, and signed agreement document paths.

| Attribute / Field | Data Type | Constraints | Description | Example Data |
| :--- | :--- | :--- | :--- | :--- |
| `id` | `UUID` | `PRIMARY KEY`, `DEFAULT gen_random_uuid()` | Lease ID | `"06dd...0f77"` |
| `organization_id`| `UUID` | `FOREIGN KEY` ➔ `organizations(id)` | Parent Organization | `"a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11"` |
| `unit_id` | `UUID` | `FOREIGN KEY` ➔ `units(id)` | Assigned Unit / Bed | `"e4bb...0d55"` |
| `tenant_id` | `UUID` | `FOREIGN KEY` ➔ `tenants(id)` | Renter / Resident | `"f5cc...0e66"` |
| `owner_id` | `UUID` | `FOREIGN KEY` ➔ `profiles(id)` | Landlord / Broker | `"b1ee...0a22"` |
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

---

## 8. `receipts`
Stores auto-sequenced financial payment receipts for rent, security deposits, and maintenance.

| Attribute / Field | Data Type | Constraints | Description | Example Data |
| :--- | :--- | :--- | :--- | :--- |
| `id` | `UUID` | `PRIMARY KEY`, `DEFAULT gen_random_uuid()` | Receipt Record ID | `"17ee...1088"` |
| `receipt_number` | `TEXT` | `UNIQUE`, `NOT NULL` | Auto-Generated Receipt No | `"REC-2026-001001"` |
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
| `receipt_pdf_url`| `TEXT` | - | Generated PDF Receipt URL | `"https://cdn.brokerprop.com/receipts/REC-2026-001001.pdf"` |
| `metadata` | `JSONB` | `DEFAULT '{}'::jsonb` | Custom payment payload | `{"razorpay_payment_id": "pay_L8x92aK"}` |

---

## 9. `police_verifications`
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
| `metadata` | `JSONB` | `DEFAULT '{}'::jsonb` | Custom police intimation metadata | `{"police_chalan_verified": true}` |

---

## 10. `esign_transactions`
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

---

## 11. `maintenance_tickets`
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

---

## 12. `integration_logs`
Stores audit logs for third-party API interactions (Surepass Aadhaar/PAN, Razorpay Payments, Meta WhatsApp API).

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
| `created_at` | `TIMESTAMPTZ`| `DEFAULT NOW()` | Execution Timestamp | `"2026-10-02T14:30:12Z"` |

---

## 🔒 Security & Data Isolation Summary
1. **Multi-Tenancy Isolation**: Enforced across every single table via `organization_id` and `owner_id` columns linked to PostgreSQL Row Level Security (RLS) policies.
2. **Audit Trails**: Every modification automatically updates `updated_at` timestamps via PostgreSQL triggers, and financial/e-Sign API calls are logged in `integration_logs` and `esign_transactions`.
