# Base Path Support in Orrery Frontend

Orrery's React frontend supports runtime base path detection, allowing the same build to be deployed at different URL paths (e.g., `/` or `/orrery/`) without requiring rebuild or reconfiguration.

## How It Works

The base path is determined through a hierarchical detection system:

1. **Server Template** (Primary): The Play Framework backend injects the base path into the HTML template
2. **Config Endpoint** (Fallback): Attempts to fetch base path from `/config` or `config` endpoints
3. **Default** (Last Resort): Falls back to root path `/`

## Using Base Paths in Code

### For Asset URLs

Use the `resolveRuntimePath()` function to resolve any asset path:

```typescript
import { resolveRuntimePath } from '../utils/runtimeBasePath';

// Resolve asset paths
const logoUrl = resolveRuntimePath('/assets/logo.png');
const apiUrl = resolveRuntimePath('/api/graphql');

// Use in components
<img src={resolveRuntimePath('/assets/icons/favicon.ico')} alt="Orrery" />
```

### For Navigation Links

Use React Router's relative paths or resolve absolute paths:

```typescript
import { resolveRuntimePath } from '../utils/runtimeBasePath';

// For React Router navigation (preferred - automatic)
<Link to="/datasets">Datasets</Link>

// For absolute URLs when needed
<a href={resolveRuntimePath('/browse')}>Browse</a>
```

### For API Endpoints

```typescript
import { resolveRuntimePath } from '../utils/runtimeBasePath';

// Resolve API endpoints
const endpoint = resolveRuntimePath('/api/v2/graphql');
fetch(endpoint, { ... });
```

## Configuration

### Server Configuration

The base path is configured in the backend Play application:

```hocon
# orrery-frontend/conf/application.conf
orrery.basePath = "/orrery"
```

### Environment Variables

For containerized deployments:

```bash
# Docker/Kubernetes
ORRERY_BASE_PATH=/orrery
```

## Examples

### Development

```bash
# Serve at root
yarn start  # Available at http://localhost:3000/

# Serve at subpath
yarn preview --base /orrery  # Available at http://localhost:3000/orrery/
```

### Production Deployment

**At Root Path:**

```bash
ORRERY_BASE_PATH="" docker run orrery-frontend
# Accessible at: https://orrery.company.com/
```

or unset

```
unset ORRERY_BASE_PATH
docker run orrery-frontend
```

**At Subpath:**

```bash
ORRERY_BASE_PATH=/orrery docker run orrery-frontend
# Accessible at: https://company.com/orrery/
```

## Browser Support

The implementation uses standard HTML `<base>` tags and modern JavaScript features:

- All modern browsers (Chrome 60+, Firefox 60+, Safari 12+, Edge 79+)
- Progressive enhancement with fallback detection

## Troubleshooting

### Assets Not Loading

1. Check browser console for 404 errors
2. Verify `window.__ORRERY_BASE_PATH__` is set correctly
3. Ensure all asset references use `resolveRuntimePath()`

### Incorrect Redirects

1. Check that authentication endpoints use resolved paths
2. Verify React Router basename configuration
3. Test config endpoint accessibility

### Development Issues

```bash
# Clear cache and rebuild
yarn clean
yarn build

# Check base path detection
console.log(window.__ORRERY_BASE_PATH__);
```

## Implementation Details

- **HTML Template**: `orrery-frontend/app/views/index.scala.html`
- **Runtime Utility**: `src/utils/runtimeBasePath.ts`
- **Asset Resolution**: All `src/` files using `resolveRuntimePath()`
- **Server Handler**: `orrery-frontend/app/controllers/Application.java`

The system automatically handles:

- Favicon and manifest paths
- Main application JS/CSS assets
- Platform logos and theme assets
- API endpoint resolution
- Authentication redirects
