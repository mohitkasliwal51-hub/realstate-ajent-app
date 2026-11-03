# StayFile Authentication & Multi-Tenancy Documentation

This document describes the user authentication and multi-tenant authorization system implemented in **StayFile**.

---

## 🔐 Overview

StayFile implements stateless JWT authentication with organization-scoped Multi-Tenancy:
- **User Registration & Organization Provisioning**: Registers user and initializes their default organization (or joins an existing one).
- **Password Security**: Strong hashing with Spring Security's BCrypt password encoder.
- **JWT Authentication**: HS256/HS384 signed tokens containing user identity, role, and organization claims.
- **Tenant Isolation**: Handled via `TenantContextFilter` which sets the active tenant context and enforces PostgreSQL Row Level Security (`SET LOCAL app.current_organization_id`).

---

## 👥 Roles & Permissions (RBAC)

- `SUPER_ADMIN`: Platform operations and system administration.
- `OWNER_ADMIN`: Primary organization administrator (property owner or agency owner).
- `PROPERTY_MANAGER`: Manager handling listings, onboarding tenants, and lease creation.
- `STAFF_ASSISTANT`: Front-desk and support staff with scoped read/write access.
- `TENANT`: Resident or rentee accessing invoices, rent receipts, and agreements.
- `PUBLIC_GUEST`: Public portal visitor viewing public property showcase.

---

## 📡 Key Authentication Endpoints

### 1. Register User & Organization
`POST /api/auth/register`
```json
{
  "email": "owner@example.com",
  "password": "SecurePassword123!",
  "fullName": "Rajesh Sharma",
  "phone": "+919876543210",
  "organizationName": "Sunshine Living Spaces",
  "organizationType": "OWNER"
}
```

### 2. Login
`POST /api/auth/login`
```json
{
  "email": "owner@example.com",
  "password": "SecurePassword123!"
}
```
**Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "type": "Bearer",
  "id": "uuid-here",
  "email": "owner@example.com",
  "fullName": "Rajesh Sharma",
  "role": "OWNER_ADMIN",
  "organizationId": "org-uuid-here"
}
```

---

## 🛡️ Request Headers for Protected Endpoints

All authenticated API requests should include:
```http
Authorization: Bearer <JWT_TOKEN>
X-Organization-Id: <ORGANIZATION_UUID>
```

---

© 2026 **StayFile Platform**. All rights reserved.
