# StayFile Environment Variables Guide

This project uses environment variables for configuration management across local development and Docker deployment scenarios.

## Environment File Structure

```
stayfile-app/
├── .env                    # Docker Compose environment (development)
├── .env.example           # Docker Compose environment template
├── .env.prod              # Docker Compose environment (production)
├── .env.prod.example      # Docker Compose environment template (production)
├── backend/
│   ├── .env              # Backend-specific environment variables
│   └── .env.example      # Backend environment template
└── frontend/
    └── .env.local        # Frontend environment (NEXT_PUBLIC_API_URL)
```

## Security Considerations

### ✅ Secure
- Database credentials injected via `DB_USERNAME` and `DB_PASSWORD`
- JWT secrets injected via `JWT_SECRET`
- PII AES key injected via `PII_SECRET_KEY`
- CORS origins configured via `CORS_ALLOWED_ORIGINS`

### ❌ Never Commit
- Unencrypted production keys or database passwords
- Do not commit `.env` or `.env.prod` to version control

---

© 2026 **StayFile Platform**. All rights reserved.
