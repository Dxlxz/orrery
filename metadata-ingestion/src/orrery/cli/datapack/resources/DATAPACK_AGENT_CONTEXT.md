# Orrery Datapack CLI - Agent Context

Best practices for AI agents using `orrery datapack`.

**Status: Experimental** — command surface and behavior may change.

## Quick Reference

```bash
# Discover available packs
orrery datapack list --format json

# Inspect a specific pack
orrery datapack info showcase-ecommerce

# Load a named pack (with schema downshift + time-shifting)
orrery datapack load showcase-ecommerce

# Load from an arbitrary URL
orrery datapack load my-pack --url https://example.com/data.json --trust-custom

# Load from a local file
orrery datapack load my-pack --url file:///path/to/data.json --trust-custom

# Preview without ingesting
orrery datapack load showcase-ecommerce --dry-run

# Remove loaded pack data
orrery datapack unload showcase-ecommerce
```

## Key Concepts

- **Data packs** are curated collections of MCPs (Metadata Change Proposals) that populate a Orrery instance with sample or reference metadata.
- **Registry**: Packs are discovered from a JSON registry (remote with local cache, bundled fallback for offline use).
- **Trust tiers**: `verified` (loads freely), `community` (requires `--trust-community`), `custom` (requires `--trust-custom`).
- **Schema downshift**: Automatically filters out MCPs with aspects unsupported by the target server (prevents batch poisoning).
- **Time-shifting**: Rebases timestamps so historical data appears fresh relative to the current time.
- **Load tracking**: Each load records its `run_id` for clean unload via rollback.

## Built-in Packs

| Name                 | Description                                                                        | Size    |
| -------------------- | ---------------------------------------------------------------------------------- | ------- |
| `bootstrap`          | Lightweight bootstrap data (datasets, dashboards, users, tags)                     | ~100 KB |
| `showcase-ecommerce` | Rich e-commerce demo with 1049 entities across Snowflake, Looker, PowerBI, Tableau | ~2.7 MB |
| `search-quality`     | Search-test corpus. Load with `file://` until the pack is on `master`              | ~9 MB   |

## Agent Workflow

### Loading sample data into a fresh instance

```bash
# 1. Ensure orrery is initialized
orrery init --username orrery --password orrery

# 2. Load the showcase-ecommerce pack
orrery datapack load showcase-ecommerce

# 3. Verify it loaded
orrery datapack info showcase-ecommerce
```

### Loading with custom time anchor

```bash
# Make timestamps appear as if data was loaded on a specific date
orrery datapack load showcase-ecommerce --as-of 2025-01-15
```

### Cleaning up

```bash
# Soft-delete (reversible)
orrery datapack unload showcase-ecommerce

# Hard-delete (irreversible)
orrery datapack unload showcase-ecommerce --hard
```

## Error Handling

- **Version mismatch**: `--force` to override server version checks.
- **Trust blocked**: Use `--trust-community` or `--trust-custom` as appropriate.
- **SHA256 mismatch**: Hard error, cannot override. Re-download with `--no-cache`.
- **Schema incompatibility**: Handled automatically by downshift filter. Filtered MCPs are logged.
- **Dangling references**: Warnings are informational. Data loads successfully; some UI links may not resolve.

## Ingestion Source

Data packs can also be loaded via ingestion recipes using `demo-data`:

```yaml
source:
  type: demo-data
  config:
    pack_name: "showcase-ecommerce" # OR pack_url: "https://..."
    no_time_shift: false
    as_of: "2025-01-15T00:00:00Z"
    trust_community: false
    trust_custom: false
    no_cache: false
```

With no config, `demo-data` loads the `bootstrap` pack (backward compatible).
