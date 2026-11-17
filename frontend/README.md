# StayFile Frontend (Next.js App Router + Tailwind CSS)

StayFile is a property operations OS for brokers, owners, property managers, staff, and tenants.

## Production Stack

- **Framework**: Next.js 15 (App Router)
- **Styling**: Tailwind CSS + Lucide Icons
- **HTTP Client**: Axios (configured with auth token interceptors)
- **State Management**: React Context (`AuthContext.tsx`, `ToastContext.tsx`)
- **Backend API**: Spring Boot REST API (`http://localhost:8080`)

## Route Structure

```text
app/
├── (auth)/
│   ├── login/          # Staff & Owner Login
│   └── register/       # Organization Registration
├── (portal)/
│   ├── app/            # Operations Dashboard & KPI Cards
│   │   ├── properties/ # Properties & Unit Inventory
│   │   │   └── [id]/   # Property Details & Rooms/Beds Inventory
│   │   ├── landlords/  # Landlords Directory (Brokerage Mode)
│   │   ├── tenants/    # Tenant Onboarding & KYC Records
│   │   ├── leases/     # Lease Lifecycle, Clauses & PDF Generator
│   │   ├── invoices/   # Rent & Electricity Invoices (GST & HRA compliant)
│   │   ├── receipts/   # Official Rent Receipts & PDF Downloads
│   │   ├── payouts/    # Landlord Payout Calculations (Brokerage Mode)
│   │   ├── meter-readings/ # Utility Sub-Meters & Multi-Tier Split Engine
│   │   ├── tickets/    # Resident Maintenance Desk & Workflow
│   │   ├── leads/      # CRM Inquiries from Public Showcase
│   │   ├── settings/   # Organization Branding, Modes & Bank Details
│   │   └── tenant/     # Tenant Self-Service Portal (Residence, Invoices, Tickets)
└── p/[organizationSlug]/ # Public White-Label Property Showcase & Lead Capture
```

## Running Locally

```bash
npm install
npm run dev
```

The frontend runs at `http://localhost:3000` and connects to the Spring Boot REST API at `http://localhost:8080`.

Set `NEXT_PUBLIC_API_URL` in `.env` to override the API base URL.
