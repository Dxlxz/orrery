# Orrery Base Path Configuration

This document describes how to configure Orrery to run from a custom base path (e.g., `/my-app/` instead of `/`).

## Overview

Orrery now supports serving from a custom base path, allowing you to:

- Serve Orrery from a subdirectory (e.g., `https://company.com/orrery/`)
- Deploy multiple Orrery instances on the same domain
- Integrate Orrery behind reverse proxies with path-based routing

## Configuration

### Environment Variables

Set the following environment variable to configure the base path:

```bash
export ORRERY_BASE_PATH="/my-app"
```

**Important Notes:**

- The base path should start with `/` but not end with `/`
- Examples: `/orrery`, `/my-app`, `/tools/orrery`
- Leave empty or unset for root path deployment

### Component Configuration

The base path configuration affects three main components:

#### 1. Frontend (orrery-web-react)

The frontend determines the base path using multiple detection methods (in priority order):

1. **Environment Variable** (highest priority):

   ```bash
   # Set in .env file or environment
   REACT_APP_BASE_PATH=/my-app
   ```

2. **Server Configuration** (fetched from `/config` endpoint):

   - The React app fetches base path from the backend's configuration
   - Most reliable for production deployments

3. **URL Analysis** (fallback):
   - Analyzes current URL to detect base path
   - Used when server config is unavailable

**Development Configuration:**

```bash
# Method 1: Environment variable (recommended for development)
echo "REACT_APP_BASE_PATH=/my-app" >> .env
yarn start  # Development server at http://localhost:3000/my-app

# Method 2: Using Vite preview (for testing production builds)
yarn build
npx vite preview --port 3001  # Uses base path from REACT_APP_BASE_PATH
# Accessible at: http://localhost:3001/my-app

# Method 3: Override base path via command line
npx vite preview --port 3001 --base /custom-path
```

**Production Behavior:**

- Automatically fetches base path from server configuration
- Falls back to URL detection if server config unavailable
- Provides console warnings to help with debugging

#### 2. Orrery Frontend Service (Play Framework)

The Play Framework application supports base path through:

- `orrery.basePath` configuration in `application.conf`
- `play.http.context` for Play Framework native support
- Environment variable: `ORRERY_BASE_PATH`

#### 3. Metadata Service (Spring Boot)

The metadata service (GMS) supports base path through:

- `server.servlet.context-path` in `application.yaml`
- Environment variable: `ORRERY_BASE_PATH`

## Deployment Examples

### Docker Compose

```yaml
version: "3.8"
services:
  orrery-frontend:
    environment:
      - ORRERY_BASE_PATH=/orrery
	  - PLAY_HTTP_CONTEXT=/orrery

  orrery-gms:
    environment:
      - ORRERY_BASE_PATH=/orrery
```

### Kubernetes

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: orrery-frontend
spec:
  template:
    spec:
      containers:
        - name: orrery-frontend
          env:
            - name: ORRERY_BASE_PATH
              value: "/orrery"
            - name: ORRERY_GMS_BASE_PATH
              value: "/orrery"
            - name: PLAY_HTTP_CONTEXT
              value: "/orrery"
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: orrery-gms
spec:
  template:
    spec:
      containers:
        - name: orrery-frontend
          env:
            - name: ORRERY_BASE_PATH
              value: "/orrery"
            - name: ORRERY_GMS_BASE_PATH
              value: "/orrery"
```

## Limitations

- Base path must be consistent across all Orrery components
- Changing base path requires restat of GMS and front end deployments.
- Testing of multiple Orrery environments served by the same URL, e.g. http://example.com/orrery1, http://example.com/orrery2, hasn't been extensively validated.
