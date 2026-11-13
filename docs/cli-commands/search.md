# orrery search

The `orrery search` command provides a powerful interface to search across all Orrery entities directly from the command line. It supports both keyword search (default) and semantic search with flexible filtering, multiple output formats, and comprehensive discovery features.

The search command is a **command group** with multiple subcommands:

- `query` (default) - Execute search queries across Orrery entities
- `diagnose` - Diagnose search configuration and availability

## Quick Start

```shell
# Basic keyword search (query is the default subcommand)
orrery search "users"
orrery search query "users"  # Explicit subcommand

# Search with filters
orrery search "*" --filter platform=snowflake --filter entity_type=dataset

# Semantic search
orrery search --semantic "financial reports"

# Get results in table format
orrery search "dashboard" --table

# Get just URNs for piping
orrery search "dataset" --urns-only

# Preview query without executing
orrery search "users" --filter platform=snowflake --dry-run

# Custom field projection (reduce response size)
orrery search "users" --projection "urn type platform { name }"

# Diagnose search configuration
orrery search diagnose
orrery search diagnose --format json
```

## Core Features

### 1. Dual Search Modes

**Keyword Search (Default)**: Traditional text-based search using Orrery's search index.

```shell
orrery search "users"
orrery search "customer_data"
```

**Semantic Search**: AI-powered search that understands meaning and context.

```shell
orrery search --semantic "financial reports"
orrery search --semantic "user analytics dashboards"
```

### 2. Flexible Filtering

**Simple Filters**: Easy key=value syntax with implicit AND logic.

```shell
# Single filter
orrery search "*" --filter platform=snowflake

# Multiple filters (AND)
orrery search "*" --filter platform=snowflake --filter entity_type=dataset

# OR logic with comma-separated values
orrery search "*" --filter platform=snowflake,bigquery
```

**Complex Filters**: JSON-based filters with AND/OR/NOT logic.

```shell
# AND logic
orrery search "*" --filters '{"and": [{"platform": ["snowflake"]}, {"env": ["PROD"]}]}'

# OR logic
orrery search "*" --filters '{"or": [{"entity_type": ["chart"]}, {"entity_type": ["dashboard"]}]}'

# NOT logic
orrery search "*" --filters '{"not": {"platform": ["mysql"]}}'

# Complex nested logic
orrery search "*" --filters '{
  "and": [
    {"platform": ["snowflake", "bigquery"]},
    {"env": ["PROD"]},
    {"not": {"tag": ["urn:li:tag:Deprecated"]}}
  ]
}'
```

### 3. Multiple Output Formats

**JSON (Default)**: Structured output with full metadata.

```shell
orrery search "users" --format json
# or simply
orrery search "users"
```

**Table**: Human-readable table format.

```shell
orrery search "users" --table
# or
orrery search "users" --format table
```

**URNs**: List of URNs only, perfect for piping.

```shell
orrery search "dataset" --urns-only
# or
orrery search "dataset" --format urns
```

### 4. Filter Discovery

**List Available Filters**: See all filter types and their descriptions.

```shell
orrery search --list-filters
```

Output includes:

- entity_type, entity_subtype
- platform, domain, container
- tag, glossary_term, owner
- env, status
- custom, and, or, not

**Describe Specific Filter**: Get detailed information about a filter.

```shell
orrery search --describe-filter platform
orrery search --describe-filter entity_type
orrery search --describe-filter custom
```

### 5. Faceted Search

**Facets-Only Mode**: Get aggregation counts without search results.

```shell
orrery search "*" --facets-only

# With filters
orrery search "*" --filter entity_type=dataset --facets-only --format table
```

Useful for:

- Understanding data distribution
- Building filter UIs
- Data exploration

### 6. Pagination

```shell
# Get first 20 results
orrery search "*" --limit 20

# Get next page (results 20-40)
orrery search "*" --limit 20 --offset 20

# Maximum limit is 50
orrery search "*" --limit 50
```

### 7. Sorting

```shell
# Sort by field
orrery search "*" --sort-by lastModified

# Sort ascending
orrery search "*" --sort-by name --sort-order asc

# Sort descending (default)
orrery search "*" --sort-by name --sort-order desc
```

### 8. Orrery Views

Apply saved Orrery Views to searches:

```shell
orrery search "*" --view "urn:li:orreryView:12345678"
```

### 9. Dry Run

Preview the compiled GraphQL query and variables without connecting to Orrery:

```shell
# See what query would be sent
orrery search "users" --dry-run

# Dry run with filters and sorting
orrery search "*" --filter platform=snowflake --sort-by lastModified --dry-run

# Dry run with semantic search
orrery search --semantic "financial data" --dry-run

# Dry run with projection
orrery search "users" --projection "urn type" --dry-run
```

Output is a JSON object containing `operation_name`, `graphql_field`, `variables`, and optionally `projection` and `query` (when using `--projection`).

### 10. Field Projection

Control which GraphQL fields are returned per entity to reduce response size:

```shell
# Return only URN and type
orrery search "users" --projection "urn type"

# Return URN with platform info
orrery search "users" --projection "urn platform { ...PlatformFields }"

# Load projection from a file
orrery search "users" --projection @my_fields.gql

# Braces are optional — these are equivalent
orrery search "users" --projection "{ urn type }"
orrery search "users" --projection "urn type"
```

When `--projection` is omitted, the full default selection set from `search_queries.gql` is used. Projections have a 5000-character limit and must not contain mutations, introspection queries, variables, or directives.

### 11. Agent Context

Print best-practice guidance for AI agents consuming the search CLI:

```shell
# Print agent context and exit
orrery search --agent-context
```

When stdout is not a TTY (e.g., piped to an AI agent), `--help` automatically appends the agent context.

## Command Reference

### Main Command

```shell
orrery search [OPTIONS] COMMAND [ARGS]
```

The `search` command is a command group. The default subcommand is `query`, so `orrery search "text"` is equivalent to `orrery search query "text"`.

### Subcommands

#### `query` (default)

Execute search queries across Orrery entities.

```shell
orrery search [QUERY] [OPTIONS]
orrery search query [QUERY] [OPTIONS]
```

**Options:**

| Option              | Type   | Description                                                 |
| ------------------- | ------ | ----------------------------------------------------------- |
| `QUERY`             | string | Search query string (default: "\*" for all entities)        |
| `--semantic`        | flag   | Use semantic search instead of keyword search               |
| `-f, --filter`      | string | Simple filter: key=value (repeatable, comma for OR)         |
| `--filters`         | string | Complex filters as JSON string (AND/OR/NOT logic)           |
| `-n, --limit`       | int    | Number of results to return (default: 10, max: 50)          |
| `--offset`          | int    | Starting position for pagination (default: 0)               |
| `--sort-by`         | string | Field name to sort by                                       |
| `--sort-order`      | choice | Sort order: `asc` or `desc` (default: desc)                 |
| `--format`          | choice | Output format: `json`, `table`, or `urns` (default: json)   |
| `--table`           | flag   | Shortcut for --format table                                 |
| `--urns-only`       | flag   | Shortcut for --format urns                                  |
| `--list-filters`    | flag   | List all available filter fields                            |
| `--describe-filter` | string | Describe a specific filter field                            |
| `--facets-only`     | flag   | Return only facets, no search results                       |
| `--view`            | string | Orrery View URN to apply                                   |
| `--dry-run`         | flag   | Show compiled GraphQL query and variables without executing |
| `--projection`      | string | Custom GraphQL selection set for entity (inline or @file)   |
| `--agent-context`   | flag   | Print agent skill context (best practices for AI agents)    |

#### `diagnose`

Diagnose search configuration and availability. Checks semantic search configuration, backend connectivity, embedding provider, model details, and enabled entity types.

```shell
orrery search diagnose [OPTIONS]
```

**Options:**

| Option     | Type   | Description                                     |
| ---------- | ------ | ----------------------------------------------- |
| `--format` | choice | Output format: `text` or `json` (default: text) |

**Examples:**

```shell
# Text output (default)
orrery search diagnose

# JSON output
orrery search diagnose --format json
```

### Available Filters

#### Entity Filters

- **entity_type**: Filter by entity type (dataset, dashboard, chart, corpuser, etc.)
- **entity_subtype**: Filter by entity subtype (Table, View, Model, etc.)

#### Platform & Location

- **platform**: Filter by data platform (snowflake, bigquery, looker, etc.)
- **container**: Filter by container URN (database, schema, etc.)
- **domain**: Filter by domain URN

#### Metadata

- **tag**: Filter by tag URN
- **glossary_term**: Filter by glossary term URN
- **owner**: Filter by owner URN (user or group)

#### Environment

- **env**: Filter by environment (PROD, DEV, STAGING, QA)
- **status**: Filter by deletion status (NOT_SOFT_DELETED, SOFT_DELETED)

#### Advanced

- **custom**: Custom field filter with flexible conditions
- **and**: Logical AND of filters
- **or**: Logical OR of filters
- **not**: Logical NOT of a filter

## Usage Examples

### Basic Searches

```shell
# Search for everything
orrery search "*"

# Search for specific term
orrery search "users"

# Search with limit
orrery search "dashboard" --limit 20
```

### Semantic Search

```shell
# Find financial datasets
orrery search --semantic "financial reports and revenue data"

# Find user analytics
orrery search --semantic "user behavior analysis dashboards"

# Find ML models
orrery search --semantic "machine learning models for prediction"
```

### Simple Filtering

```shell
# Platform filter
orrery search "*" --filter platform=snowflake

# Entity type filter
orrery search "*" --filter entity_type=dataset

# Multiple filters (AND)
orrery search "*" --filter platform=snowflake --filter env=PROD

# OR with comma separation
orrery search "*" --filter platform=snowflake,bigquery

# Combine different filters
orrery search "customer" --filter platform=mysql --filter entity_type=dataset,dashboard
```

### Complex Filtering

```shell
# Find Snowflake PROD datasets
orrery search "*" --filters '{
  "and": [
    {"platform": ["snowflake"]},
    {"entity_type": ["dataset"]},
    {"env": ["PROD"]}
  ]
}'

# Find charts OR dashboards in Looker
orrery search "*" --filters '{
  "and": [
    {"platform": ["looker"]},
    {"or": [
      {"entity_type": ["chart"]},
      {"entity_type": ["dashboard"]}
    ]}
  ]
}'

# Exclude deprecated datasets
orrery search "*" --filters '{
  "and": [
    {"entity_type": ["dataset"]},
    {"not": {"tag": ["urn:li:tag:Deprecated"]}}
  ]
}'

# Custom field filter
orrery search "*" --filters '{
  "custom": {
    "field": "name",
    "condition": "CONTAIN",
    "values": ["customer"]
  }
}'
```

### Output Formats

```shell
# JSON (default) - full metadata
orrery search "users" --format json

# Table - human readable
orrery search "users" --table

# URNs only - for piping to other commands
orrery search "*" --filter entity_type=dataset --urns-only | while read urn; do
  orrery get --urn "$urn" --aspect ownership
done
```

### Discovery & Exploration

```shell
# List all available filters
orrery search --list-filters

# Learn about a specific filter
orrery search --describe-filter platform
orrery search --describe-filter entity_type
orrery search --describe-filter custom

# Explore data distribution with facets
orrery search "*" --facets-only --format table
orrery search "*" --filter entity_type=dataset --facets-only

# Diagnose search configuration
orrery search diagnose
orrery search diagnose --format json
```

### Pagination

```shell
# First page (10 results)
orrery search "dataset"

# Second page
orrery search "dataset" --limit 10 --offset 10

# Large page
orrery search "*" --limit 50 --offset 100
```

### Sorting

```shell
# Sort by last modified (newest first)
orrery search "*" --sort-by lastModified --sort-order desc

# Sort by name (alphabetically)
orrery search "*" --sort-by name --sort-order asc
```

## Advanced Use Cases

### Finding Datasets in Multiple Platforms

```shell
# Quick version with simple filters
orrery search "*" --filter platform=snowflake,bigquery,postgres --filter entity_type=dataset --table

# Complex version with AND/OR
orrery search "*" --filters '{
  "and": [
    {"entity_type": ["dataset"]},
    {"or": [
      {"platform": ["snowflake"]},
      {"platform": ["bigquery"]},
      {"platform": ["postgres"]}
    ]}
  ]
}' --table
```

### Finding Production Data with Specific Tags

```shell
orrery search "*" --filters '{
  "and": [
    {"env": ["PROD"]},
    {"tag": ["urn:li:tag:PII", "urn:li:tag:Sensitive"]}
  ]
}' --table
```

### Finding Datasets Owned by Specific Team

```shell
orrery search "*" --filters '{
  "and": [
    {"entity_type": ["dataset"]},
    {"owner": ["urn:li:corpGroup:data-eng"]}
  ]
}' --table
```

### Pipeline: Search and Process

```shell
# Find all PROD datasets and check their ownership
orrery search "*" \
  --filter entity_type=dataset \
  --filter env=PROD \
  --urns-only | \
while read urn; do
  echo "Checking $urn"
  orrery get --urn "$urn" --aspect ownership
done

# Find datasets and export to file
orrery search "*" \
  --filter platform=snowflake \
  --format json > snowflake_datasets.json
```

### Semantic Search for Data Discovery

```shell
# Find datasets about customers
orrery search --semantic "customer information and profiles"

# Find ML models for forecasting
orrery search --semantic "predictive models and forecasting algorithms"

# Find compliance-related documentation
orrery search --semantic "GDPR compliance and privacy documentation"
```

## Output Examples

### JSON Output (Default)

```json
{
  "count": 2,
  "facets": [
    {
      "aggregations": [
        { "count": 10, "value": "DATASET" },
        { "count": 5, "value": "DASHBOARD" }
      ],
      "displayName": "Type",
      "field": "_entityType"
    }
  ],
  "searchResults": [
    {
      "entity": {
        "platform": { "name": "snowflake" },
        "properties": {
          "description": "User data table",
          "name": "users"
        },
        "urn": "urn:li:dataset:(urn:li:dataPlatform:snowflake,db.users,PROD)"
      },
      "matchedFields": [{ "name": "name", "value": "users" }]
    }
  ],
  "start": 0,
  "total": 15
}
```

### Table Output

```
+--------------------------------------------------------------+------------+-----------+-------------------------+
| URN                                                          | Name       | Platform  | Description             |
+==============================================================+============+===========+=========================+
| urn:li:dataset:(urn:li:dataPlatform:snowflake,db.users,PROD)| users      | snowflake | User data table         |
+--------------------------------------------------------------+------------+-----------+-------------------------+
| urn:li:dashboard:(looker,dashboards.19)                     | User Stats | looker    | User analytics dash...  |
+--------------------------------------------------------------+------------+-----------+-------------------------+

Showing 1-2 of 15 results
```

### URNs Output

```
urn:li:dataset:(urn:li:dataPlatform:snowflake,db.users,PROD)
urn:li:dashboard:(looker,dashboards.19)
urn:li:chart:(looker,chart.123)
```

## Integration Examples

### Shell Scripts

```bash
#!/bin/bash
# Find and tag all PROD datasets in Snowflake

DATASETS=$(orrery search "*" \
  --filter platform=snowflake \
  --filter entity_type=dataset \
  --filter env=PROD \
  --urns-only)

for urn in $DATASETS; do
  echo "Tagging $urn"
  orrery put --urn "$urn" --aspect globalTags -d '{
    "tags": [{"tag": "urn:li:tag:Production"}]
  }'
done
```

### Data Quality Checks

```bash
#!/bin/bash
# Find datasets without ownership

DATASETS=$(orrery search "*" \
  --filter entity_type=dataset \
  --limit 50 \
  --format json)

echo "$DATASETS" | jq -r '.searchResults[] |
  select(.entity.ownership == null) |
  .entity.urn'
```

### Monitoring

```bash
#!/bin/bash
# Monitor new datasets added today

COUNT=$(orrery search "*" \
  --filter entity_type=dataset \
  --format json | jq '.total')

echo "Total datasets: $COUNT"
```

## Error Handling

The CLI provides clear error messages for common issues:

```shell
# Semantic search not enabled
orrery search --semantic "test"
# Error: Semantic search is not enabled on this Orrery instance.
# Use keyword search (default) or contact your Orrery administrator.

# Invalid filter format
orrery search "*" --filter invalid_format
# Error: Invalid filter: invalid_format. Expected key=value format.

# Conflicting filter options
orrery search "*" --filter platform=snowflake --filters '{"env": ["PROD"]}'
# Error: Cannot use both --filter and --filters.
# Use --filter for simple filters or --filters for complex logic.

# Invalid limit
orrery search "*" --limit 0
# Error: --limit must be at least 1

# Limit too high
orrery search "*" --limit 100
# Warning: limit capped at 50
```

## Diagnostics

The `diagnose` subcommand helps troubleshoot search configuration issues, particularly for semantic search:

```shell
# Check semantic search configuration
orrery search diagnose

# Get detailed JSON output
orrery search diagnose --format json
```

The diagnostic output includes:

- Semantic search availability and status
- Embedding provider configuration
- Model ID and embedding key
- AWS region (if applicable)
- Enabled entity types for semantic search
- Recommendations for resolving issues

## Tips and Best Practices

1. **Start Broad**: Begin with `orrery search "*"` and add filters progressively
2. **Use Discovery**: Run `--list-filters` and `--describe-filter` to learn available options
3. **Test with `--table`**: Use table format for quick exploration before JSON processing
4. **Semantic Search**: Use semantic search for natural language queries and concept-based discovery
5. **Facets First**: Use `--facets-only` to understand data distribution before filtering
6. **Pipe URNs**: Combine `--urns-only` with other `orrery` commands for bulk operations
7. **Limit Wisely**: Start with small limits during development, increase for production
8. **Save Complex Filters**: Store complex JSON filters in files for reusability
9. **Check Pagination**: Use `total` field in JSON output to determine if pagination is needed
10. **Environment Variables**: Configure `ORRERY_GMS_URL` and `ORRERY_GMS_TOKEN` for easier access

## Troubleshooting

### Common Issues

**"Semantic search is not enabled"**: Your Orrery instance doesn't have semantic search configured. Use keyword search instead or contact your administrator.

**"No results found"**: Try broadening your search query or removing filters. Use `orrery search "*"` to verify connectivity.

**"Invalid filter"**: Check filter syntax. Use `--list-filters` to see available filters and `--describe-filter <name>` for details.

**Slow searches**: Use more specific filters to narrow results. Check network latency to Orrery server.

**Permission denied**: Ensure you've run `orrery init` and have valid credentials configured.

### Getting Help

```shell
# Command help
orrery search --help

# Subcommand help
orrery search query --help
orrery search diagnose --help

# List available filters
orrery search --list-filters

# Learn about specific filter
orrery search --describe-filter platform

# Diagnose search configuration
orrery search diagnose

# Check Orrery connection
orrery check plugins
```

## See Also

- [Orrery CLI](../cli.md) - Main CLI documentation
- [GraphQL Command](./graphql.md) - GraphQL query interface
- [Dataset Command](./dataset.md) - Dataset-specific operations
