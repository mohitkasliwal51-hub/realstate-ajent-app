# StayFile Frontend (Next.js App Router + Tailwind CSS)

StayFile is a property operations OS for brokers, owners, property managers, staff, and tenants.

## Production Stack

- **Framework**: Next.js 15 (App Router)
- **Styling**: Tailwind CSS + Lucide Icons
- **HTTP Client**: Axios (configured with auth token interceptors)
- **State Management**: React Context (`AuthContext.tsx`)
- **Backend API**: Spring Boot REST API (`http://localhost:8080`)

## Route Structure

```text
app/
├── (auth)/
│   ├── login/          # Staff & Owner Login
│   └── register/       # Organization Registration
├── (portal)/
│   ├── app/            # Staff Portal Dashboard
│   ├── properties/     # Properties & Unit Inventory
│   │   └── [id]/       # Property Details & Rooms/Beds Inventory
│   ├── tenants/        # Tenant Onboarding & KYC
│   ├── leases/         # Lease Workflow & PDF Download
│   ├── receipts/       # Rent Receipts Generation
│   └── settings/       # Organization Branding Settings
├── tenant/             # Tenant Self-Service Portal
└── p/[organizationSlug]/ # Public White-Label Property Showcase
```

## Running Locally

```bash
npm install
npm run dev
```

The frontend runs at `http://localhost:3000` and connects to the Spring Boot REST API at `http://localhost:8080`.

Set `NEXT_PUBLIC_API_URL` in `.env` to override the API base URL.
