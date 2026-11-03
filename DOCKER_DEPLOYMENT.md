# StayFile - Docker Deployment Guide

## Overview

StayFile is containerized with a comprehensive Docker setup that includes:
- **Backend**: Spring Boot 3.4.11 (Java 21 LTS) API container
- **Frontend**: Next.js 15 App Router web container
- **Database**: PostgreSQL 16 Alpine container with RLS init scripts
- Development and Production environments
- Container health checks and network isolation
- Volume persistence for database data

## Architecture

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Frontend      │    │   Backend       │    │   Database      │
│   (Next.js)     │◄──►│   (Spring Boot) │◄──►│   (PostgreSQL)  │
│   Port: 3000    │    │   Port: 8080    │    │   Port: 5433    │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

## Quick Start

### Prerequisites
- Docker Engine 20.10+
- Docker Compose V2
- 4GB+ RAM recommended

### 1. Build and Run
```bash
# Build images
./docker.sh build

# Start services (Development)
./docker.sh up

# Or for Production
./docker.sh up-prod
```

### 2. Verify Deployment
- **Frontend**: http://localhost:3000
- **Backend API**: http://localhost:8080
- **Actuator Health**: http://localhost:8080/actuator/health

### 3. Docker Management Commands
```bash
./docker.sh status        # View running containers
./docker.sh logs-f        # Follow all container logs
./docker.sh down          # Stop all containers
./docker.sh db-shell      # Enter PostgreSQL shell
./docker.sh backend-shell # Enter backend container shell
```

---

© 2026 **StayFile Platform**. All rights reserved.
