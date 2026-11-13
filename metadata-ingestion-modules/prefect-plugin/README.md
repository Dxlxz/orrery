<!-- PyPI long description. Keep concise, feature-discovery-first. -->

# Orrery Prefect Plugin

**Automatic lineage and run metadata from Prefect into Orrery** — captures flow structure, task inputs/outputs, and run history with minimal setup.

## What you can do

- **Emit flow and task metadata** to Orrery as pipeline runs
- **Capture dataset lineage** — declare inputs and outputs per task and see them in Orrery
- **Configure via Prefect blocks** — store your Orrery connection settings as a reusable block
- **Works with any Orrery deployment** — self-hosted or Orrery Cloud

## Installation

```bash
pip install prefect-orrery
```

## Quickstart

### 1. Save your Orrery connection as a Prefect block

```python
from prefect_orrery.orrery_emitter import OrreryEmitter

OrreryEmitter(
    orrery_rest_url="http://localhost:8080",
    env="PROD",
).save("my-orrery")
```

### 2. Use it in your flows

```python
from prefect import flow, task
from prefect_orrery.orrery_emitter import OrreryEmitter
from prefect_orrery.entities import Dataset

emitter = OrreryEmitter.load("my-orrery")

@task
def transform(data, emitter):
    emitter.add_task(
        inputs=[Dataset("snowflake", "mydb.schema.source_table")],
        outputs=[Dataset("snowflake", "mydb.schema.output_table")],
    )
    return data

@flow
def my_pipeline():
    data = extract()
    transform(data, emitter)
    emitter.emit_flow()   # required — emits all metadata at the end
```

## Configuration options

| Option              | Default                 | Description                         |
| ------------------- | ----------------------- | ----------------------------------- |
| `orrery_rest_url`  | `http://localhost:8080` | Orrery GMS URL                     |
| `env`               | `PROD`                  | Environment tag for assets          |
| `platform_instance` | `None`                  | Platform instance for assets        |
| `token`             | `None`                  | Auth token (if GMS auth is enabled) |

## Links

- [Full documentation](https://docs.orrery.dev/docs/lineage/prefect)
- [Prefect](https://www.prefect.io/)
- [GitHub](https://github.com/Dxlxz/orrery)
- [Slack community](https://orrery.dev/slack)
