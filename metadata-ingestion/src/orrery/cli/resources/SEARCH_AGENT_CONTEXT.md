# Orrery Search CLI - Agent Context

Best practices for AI agents consuming `orrery search`.

## Output Discipline

- Always use `--format json` (the default) for machine consumption.
- Always set `--limit` explicitly; default is 10, max is 50.
- Use `--urns-only` when piping URNs to other commands.

```bash
orrery search "customers" --format json --limit 20
orrery search "customers" --urns-only | xargs -I{} orrery get --urn {}
```

## Projection

Use `--projection` to limit returned fields and reduce token cost.

```bash
# Minimal: just URNs and types
orrery search "customers" --projection "urn type"

# With dataset properties
orrery search "customers" --projection "urn type ... on Dataset { properties { name description } platform { ...PlatformFields } }"

# From a file
orrery search "customers" --projection @fields.gql
```

Common projections:

| Use case           | Projection                                                            |
| ------------------ | --------------------------------------------------------------------- |
| URN list           | `urn`                                                                 |
| Name + platform    | `urn type ... on Dataset { properties { name } platform { name } }`   |
| Schema exploration | `urn ... on Dataset { schemaMetadata { fields { fieldPath type } } }` |

## Dry Run

Always `--dry-run` first to verify the compiled query before executing.

```bash
orrery search "customers" --filter platform=snowflake --dry-run
```

The output is JSON with `operation_name`, `graphql_field`, `variables`, and optionally `query` (when `--projection` is set).

## Filters

Use `--filter key=value` for simple cases (repeatable, comma for OR on same field).
Use `--where 'SQL expr'` for SQL-like expressions (recommended for agents — most readable).
Use `--filters '{json}'` for complex AND/OR/NOT logic.

These three options are mutually exclusive — use one at a time.

```bash
# Simple
orrery search "*" --filter platform=snowflake --filter env=PROD

# OR on same field
orrery search "*" --filter platform=snowflake,bigquery

# SQL-like WHERE (recommended for agents)
orrery search "*" --where "platform = snowflake AND env = PROD"
orrery search "*" --where "platform IN (snowflake, bigquery)"
orrery search "*" --where "entity_type = dataset AND (platform = snowflake OR platform = bigquery)"
orrery search "*" --where "glossary_term IS NOT NULL"
orrery search "*" --where "NOT env = DEV"

# Complex JSON
orrery search "*" --filters '{"and": [{"platform": ["snowflake"]}, {"env": ["PROD"]}]}'
```

Discover available filters:

```bash
orrery search list-filters
orrery search describe-filter platform
```

## Pagination

Use `--limit` + `--offset`. Max 50 results per page.

```bash
orrery search "customers" --limit 50 --offset 0    # page 1
orrery search "customers" --limit 50 --offset 50   # page 2
```

## Sorting

```bash
orrery search "*" --sort-by name --sort-order asc
```

## Semantic Search

Beta feature. Check availability first:

```bash
orrery search diagnose --format json
```

Then use:

```bash
orrery search --semantic "financial reports about quarterly revenue"
```

## Views

Apply a saved Orrery view:

```bash
orrery search "*" --view urn:li:orreryView:my_view
```

## Error Handling

Errors are written to stderr as JSON (in non-TTY/agent context):

```json
{
  "error": "semantic_search_unavailable",
  "message": "...",
  "suggestion": "orrery search diagnose"
}
```

Error types: `usage_error`, `search_error`, `semantic_search_unavailable`, `permission_denied`, `connection_error`.

Exit codes: `0` success, `1` general error, `2` usage error, `4` permission denied, `5` connection error.

## Common Recipes

```bash
# Find all Snowflake datasets in PROD
orrery search "*" --where "platform = snowflake AND env = PROD AND entity_type = dataset" --limit 50

# Find datasets tagged with a specific term
orrery search "*" --where "glossary_term IS NOT NULL AND platform = snowflake"

# Find entities with a tag (exact URN required)
orrery search "*" --where "tag = urn:li:tag:PII"

# Get URNs for pipeline input
orrery search "customers" --filter platform=snowflake --urns-only

# Explore available platforms and counts
orrery search "*" --facets-only --format json

# Verify query before running
orrery search "revenue" --where "platform = bigquery" --dry-run
```
