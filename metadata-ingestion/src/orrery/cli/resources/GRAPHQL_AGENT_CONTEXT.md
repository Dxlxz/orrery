# Orrery GraphQL CLI - Agent Context

Best practices for AI agents consuming `orrery graphql`.

## Output Discipline

- Always use `--format json` for machine consumption; default is `human`.
- Use `--no-pretty` when piping output to reduce whitespace.

```bash
orrery graphql --list-operations --format json --no-pretty
orrery graphql --query "{ me { corpUser { urn } } }" --format json
```

## Discovery: Know What's Available

Before executing, discover available operations from the live schema:

```bash
# All queries and mutations
orrery graphql --list-operations --format json

# Queries only / mutations only
orrery graphql --list-queries --format json
orrery graphql --list-mutations --format json

# Describe a specific operation (args, types)
orrery graphql --describe dataset --format json

# Describe with full recursive type expansion
orrery graphql --describe dataset --recurse --format json

# Use local schema files instead of live introspection
orrery graphql --list-operations --schema-path /path/to/schema/
```

## Dry Run

Always `--dry-run` first to preview the query and variables before executing.

```bash
orrery graphql --query "{ me { corpUser { urn } } }" --dry-run
orrery graphql --operation dataset --variables '{"urn":"urn:li:dataset:(...)"}' --dry-run
```

The output is JSON with `dry_run`, `query`, and `variables`:

```json
{
  "dry_run": true,
  "query": "query { ... }",
  "variables": {}
}
```

## Execution

### Raw query string

```bash
orrery graphql --query "{ me { corpUser { urn username } } }" --format json
```

### Query from file

```bash
orrery graphql --query my_query.graphql --variables vars.json --format json
```

### Named operation with variables

```bash
orrery graphql --operation dataset \
  --variables '{"urn": "urn:li:dataset:(urn:li:dataPlatform:snowflake,db.table,PROD)"}' \
  --format json
```

### Variables as JSON string or file

```bash
# Inline JSON
orrery graphql --operation listUsers --variables '{"start": 0, "count": 10}'

# From file
orrery graphql --operation listUsers --variables vars.json
```

## Error Handling

Errors are written to stderr as JSON (in non-TTY/agent context):

```json
{
  "error": "usage_error",
  "message": "...",
  "suggestion": "orrery graphql --list-operations --format json"
}
```

Error types: `usage_error`, `graphql_error`, `schema_error`, `permission_denied`, `connection_error`.

Exit codes: `0` success, `1` general error, `2` usage error, `4` permission denied, `5` connection error.

## Common Recipes

```bash
# Discover what's available
orrery graphql --list-operations --format json

# Get current user info
orrery graphql --query "{ me { corpUser { urn username properties { displayName email } } } }" --format json

# Fetch a dataset by URN
orrery graphql --query '{ dataset(urn: "urn:li:dataset:(urn:li:dataPlatform:snowflake,db.table,PROD)") { urn properties { name description } } }' --format json

# List users (first 10)
orrery graphql --operation listUsers --variables '{"start": 0, "count": 10}' --format json

# Preview before executing
orrery graphql --operation dataset \
  --variables '{"urn": "urn:li:dataset:(...)"}' \
  --dry-run

# Describe an operation's arguments fully
orrery graphql --describe listDataFlows --recurse --format json
```
