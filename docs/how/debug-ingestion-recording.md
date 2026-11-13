---
title: Debug Ingestion with Recording & Replay
description: "Capture and replay Orrery ingestion runs locally to debug connector issues offline using the Orrery CLI."
---

# Debug Ingestion with Recording & Replay

:::note Beta Feature
Recording and replay is currently in beta. The feature is stable for debugging purposes but the archive format may change in future releases.
:::

When troubleshooting ingestion issues, it can be difficult to reproduce problems in a development environment. The recording and replay feature captures all external I/O during ingestion, allowing you to replay runs offline with full debugger support.

## Overview

The recording system captures:

- **HTTP Traffic**: All requests to external APIs (Looker, PowerBI, Snowflake REST, etc.) and Orrery GMS
- **Database Queries**: SQL queries and results from native database connectors

Recordings are stored in encrypted, compressed archives that can be replayed offline to reproduce issues exactly as they occurred.

## Quick Start

### 1. Install the Debug Recording Plugin

```shell
pip install 'orrery[debug-recording]'

# Or with your source connectors
pip install 'orrery[looker,debug-recording]'
```

### 2. Record an Ingestion Run

```shell
# Record with password protection (saves to temp directory)
orrery ingest run -c recipe.yaml --record --record-password mysecret --no-s3-upload

# Record to a specific directory
export INGESTION_ARTIFACT_DIR=/path/to/recordings
orrery ingest run -c recipe.yaml --record --record-password mysecret --no-s3-upload

# Record and upload directly to S3
orrery ingest run -c recipe.yaml --record --record-password mysecret \
    --record-output-path s3://my-bucket/recordings/my-run.zip
```

### 3. Replay the Recording

```shell
# Replay in air-gapped mode (no network required)
orrery ingest replay recording.zip --password mysecret

# Replay from S3
orrery ingest replay s3://my-bucket/recordings/my-run.zip --password mysecret

# Replay with live sink (emit to real Orrery)
orrery ingest replay recording.zip --password mysecret \
    --live-sink --server http://localhost:8080
```

## Configuration

### CLI Options

**Recording options:**

| Option                  | Description                                                       |
| ----------------------- | ----------------------------------------------------------------- |
| `--record`              | Enable recording                                                  |
| `--record-password`     | Encryption password (or use `ORRERY_RECORDING_PASSWORD` env var) |
| `--record-output-path`  | Output path: local file or S3 URL (`s3://bucket/path/file.zip`)   |
| `--no-s3-upload`        | Save locally only (uses `INGESTION_ARTIFACT_DIR` or temp dir)     |
| `--no-secret-redaction` | Keep real credentials (⚠️ use only for local debugging)           |

**Replay options:**

| Option                | Description                                                                      |
| --------------------- | -------------------------------------------------------------------------------- |
| `--password`          | Decryption password                                                              |
| `--live-sink`         | Emit to real GMS instead of mocking responses                                    |
| `--server`            | GMS URL for live sink mode                                                       |
| `--use-responses-lib` | Use responses library for HTTP replay (for sources with VCR issues, e.g. Looker) |

### Recipe Configuration

You can also configure recording in your recipe file:

```yaml
source:
  type: looker
  config:
    # ... source config ...

# Recording configuration
recording:
  enabled: true
  password: ${ORRERY_RECORDING_PASSWORD}
  s3_upload: true # Set to true for S3 upload
  output_path: s3://my-bucket/recordings/ # Required when s3_upload is true
```

### Environment Variables

| Variable                     | Description                                        |
| ---------------------------- | -------------------------------------------------- |
| `ORRERY_RECORDING_PASSWORD` | Default password for encryption/decryption         |
| `INGESTION_ARTIFACT_DIR`     | Directory for local recordings (when not using S3) |

## Managing Recording Archives

### View Archive Information

```shell
orrery recording info recording.zip --password mysecret

# Sample output:
# Recording Archive: recording.zip
# --------------------------------------------------
# Run ID:          snowflake-2024-12-03-10_30_00-abc123
# Source Type:     snowflake
# Sink Type:       orrery-rest
# Orrery Version: 0.14.0
# Created At:      2024-12-03T10:35:00Z
# Format Version:  1.0.0
# File Count:      3
```

Use `--json` for machine-readable output.

### Extract Archive Contents

```shell
orrery recording extract recording.zip --password mysecret --output-dir ./extracted
```

Extracts:

- `manifest.json` - Archive metadata
- `recipe.yaml` - Redacted recipe (secrets replaced with placeholders)
- `http/cassette.yaml` - HTTP recordings
- `db/queries.jsonl` - Database query recordings

### Verify Recording Accuracy

Compare recorded and replayed output to verify semantic equivalence:

```shell
# Capture output during recording
orrery ingest run -c recipe.yaml --record --record-password test --no-s3-upload \
    | tee recording_output.json

# Capture output during replay
orrery ingest replay recording.zip --password test \
    | tee replay_output.json

# Compare (ignoring timestamps and run IDs)
orrery check metadata-diff \
    --ignore-path "root['*']['systemMetadata']['lastObserved']" \
    --ignore-path "root['*']['systemMetadata']['runId']" \
    recording_output.json replay_output.json
```

A successful replay shows **PERFECT SEMANTIC MATCH**.

## Supported Sources

### HTTP-Based Sources (Full Support)

Looker, PowerBI, Tableau, Superset, Mode, Sigma, dbt Cloud, Fivetran

### Database Sources (Full Support)

Snowflake, Redshift, Databricks, BigQuery, PostgreSQL, MySQL, MSSQL

## Best Practices

1. **Use strong passwords** (16+ characters) and store securely
2. **Test replay immediately** after recording to verify completeness
3. **Minimize scope** using patterns for faster recordings
4. **Never commit recordings** to source control
5. **Delete recordings** after debugging is complete

## Troubleshooting

### "Module not found: vcrpy"

Install the debug-recording plugin:

```shell
pip install 'orrery[debug-recording]'
```

### "No match for request" during replay

The recording may be incomplete. Check for captured exceptions:

```shell
orrery recording info recording.zip --password mysecret
# Look for "has_exception: true"
```

### Replay produces different MCP count

Small differences are normal due to timing variations. Use `metadata-diff` to verify semantic equivalence (see above).

### VCR compatibility errors during replay

Some sources (e.g., Looker) use custom HTTP transport layers that conflict with VCR.py's urllib3 patching, causing errors like:

```
TypeError: super(type, obj): obj must be an instance or subtype of type
```

**Automatic fallback**: The replay command will automatically retry using the `responses` library if VCR.py fails.

**Manual override**: For known problematic sources, you can skip the VCR attempt entirely:

```shell
orrery ingest replay recording.zip --password mysecret --use-responses-lib
```

## Limitations

- **Performance**: Recording serializes HTTP calls, slowing parallel operations
- **Archive Size**: Large sources may produce 50-200MB archives
- **Protocols**: gRPC and WebSocket not currently supported

For detailed technical information, see the [Recording Module README](../../metadata-ingestion/src/orrery/ingestion/recording/README.md).
