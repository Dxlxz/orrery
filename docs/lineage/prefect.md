---
description: "Capture and emit dataset lineage and run metadata from Prefect flows and tasks into Orrery for end-to-end pipeline visibility."
---

# Prefect Integration with Orrery

## Overview

Orrery supports integration with Prefect, allowing you to ingest:

- Prefect flow and task metadata
- Flow run and Task run information
- Lineage information (when available)

This integration enables you to track and monitor your Prefect workflows within Orrery, providing a comprehensive view of your data pipeline activities.

## Prefect Orrery Block

### What is a Prefect Orrery Block?

Blocks in Prefect are primitives that enable the storage of configuration and provide an interface for interacting with external systems. The `prefect-orrery` block uses the [Orrery REST](../../metadata-ingestion/sink_docs/orrery.md#orrery-rest) emitter to send metadata events while running Prefect flows.

### Prerequisites

1. Use either Prefect Cloud (recommended) or a self-hosted Prefect server.
2. For Prefect Cloud setup, refer to the [Cloud Quickstart](https://docs.prefect.io/latest/getting-started/quickstart/) guide.
3. For self-hosted Prefect server setup, refer to the [Host Prefect Server](https://docs.prefect.io/latest/guides/host/) guide.
4. Ensure the Prefect API URL is set correctly. Verify using:

   ```shell
   prefect profile inspect
   ```

5. API URL format:
   - Prefect Cloud: `https://api.prefect.cloud/api/accounts/<account_id>/workspaces/<workspace_id>`
   - Self-hosted: `http://<host>:<port>/api`

## Upgrading from Prefect v2

`prefect-orrery` ≥ 1.0.0 requires **Prefect v3** and contains two breaking changes for existing users.

**Module path changed.** The emitter moved from `prefect_orrery.prefect_orrery` to `prefect_orrery.orrery_emitter`. Update any direct imports:

```python
# Before (Prefect v2)
from prefect_orrery.prefect_orrery import OrreryEmitter

# After (Prefect v3)
from prefect_orrery.orrery_emitter import OrreryEmitter
```

**Saved blocks must be re-registered.** Blocks previously saved to Prefect Cloud or a self-hosted server under the old entry point (`prefect_orrery.prefect_orrery:OrreryEmitter`) will fail to load after the upgrade because that module no longer exists. Re-register by running `.save()` again:

```python
from prefect_orrery.orrery_emitter import OrreryEmitter

OrreryEmitter(orrery_rest_url="...", env="PROD").save("MY-ORRERY-BLOCK", overwrite=True)
```

In Prefect v3, block types are discovered automatically when the collection package is imported — the entry point no longer needs to reference the class explicitly.

## Setup Instructions

### 1. Installation

Install `prefect-orrery` using pip:

```shell
pip install 'prefect-orrery'
```

Note: Requires Python 3.10+ and Prefect 3.x (`>=3.0.0,<4.0.0`). Prefect 2.x is not supported. If you are upgrading from Prefect 2.x, upgrade Prefect first and re-register the Orrery block (see [Upgrading from Prefect v2](#upgrading-from-prefect-v2)).

### 2. Saving Configurations to a Block

Save your configuration to the [Prefect block document store](https://docs.prefect.io/latest/concepts/blocks/#saving-blocks):

```python
from prefect_orrery.orrery_emitter import OrreryEmitter

OrreryEmitter(
    orrery_rest_url="http://localhost:8080",
    env="PROD",
    platform_instance="local_prefect"
).save("MY-ORRERY-BLOCK")
```

Configuration options:

| Config            | Type       | Default                 | Description                                                                                                                                                                                                                 |
| ----------------- | ---------- | ----------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| orrery_rest_url  | `str`      | `http://localhost:8080` | Orrery GMS REST URL                                                                                                                                                                                                        |
| env               | `str`      | `PROD`                  | Environment for assets (see [FabricType](https://docs.orrery.dev/docs/graphql/enums/#fabrictype))                                                                                                                          |
| platform_instance | `str`      | `None`                  | Platform instance for assets (see [Platform Instances](https://docs.orrery.dev/docs/platform-instances/))                                                                                                                  |
| emit_mode         | `EmitMode` | `ASYNC`                 | Emit mode for writes to Orrery. `ASYNC` (default) avoids blocking on a synchronous commit per write, reducing GMS load at high volume. Use `SYNC_WAIT`/`SYNC_PRIMARY` for read-after-write or raise-on-failure guarantees. |

### 3. Using the Block in Prefect Workflows

Load and use the saved block in your Prefect workflows:

```python
from prefect import flow, task
from prefect_orrery.dataset import Dataset
from prefect_orrery.orrery_emitter import OrreryEmitter

orrery_emitter = OrreryEmitter.load("MY-ORRERY-BLOCK")

@task(name="Transform", description="Transform the data")
def transform(data):
    data = data.split(" ")
    orrery_emitter.add_task(
        inputs=[Dataset("snowflake", "mydb.schema.tableA")],
        outputs=[Dataset("snowflake", "mydb.schema.tableC")],
    )
    return data

@flow(name="ETL flow", description="Extract transform load flow")
def etl():
    data = transform("This is data")
    orrery_emitter.emit_flow()
```

**Note**: To emit tasks, you must call `emit_flow()`. Otherwise, no metadata will be emitted.

## Concept Mapping

| Prefect Concept                                                      | Orrery Concept                                                                                       |
| -------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------- |
| [Flow](https://docs.prefect.io/latest/concepts/flows/)               | [DataFlow](https://docs.orrery.dev/docs/generated/metamodel/entities/dataflow/)                      |
| [Flow Run](https://docs.prefect.io/latest/concepts/flows/#flow-runs) | [DataProcessInstance](https://docs.orrery.dev/docs/generated/metamodel/entities/dataprocessinstance) |
| [Task](https://docs.prefect.io/latest/concepts/tasks/)               | [DataJob](https://docs.orrery.dev/docs/generated/metamodel/entities/datajob/)                        |
| [Task Run](https://docs.prefect.io/latest/concepts/tasks/#tasks)     | [DataProcessInstance](https://docs.orrery.dev/docs/generated/metamodel/entities/dataprocessinstance) |
| [Task Tag](https://docs.prefect.io/latest/concepts/tasks/#tags)      | [Tag](https://docs.orrery.dev/docs/generated/metamodel/entities/tag/)                                |

## Validation and Troubleshooting

### Validating the Setup

1. Check the Prefect UI's Blocks menu for the Orrery emitter.
2. Run a Prefect workflow and look for Orrery-related log messages:

   ```text
   Emitting flow to orrery...
   Emitting tasks to orrery...
   ```

### Debugging Common Issues

#### Incorrect Prefect API URL

If the Prefect API URL is incorrect, set it manually:

```shell
prefect config set PREFECT_API_URL='http://127.0.0.1:4200/api'
```

#### Orrery Connection Error

If you encounter a `ConnectionError: HTTPConnectionPool(host='localhost', port=8080)`, ensure that your Orrery GMS service is running.

## Additional Resources

- [Prefect Documentation](https://docs.prefect.io/)
- [Orrery Documentation](https://docs.orrery.dev/docs/)

For more information or support, please refer to the official Prefect and Orrery documentation or reach out to their respective communities.
