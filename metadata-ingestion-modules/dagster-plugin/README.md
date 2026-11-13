<!-- PyPI long description. Keep concise, feature-discovery-first. -->

# Orrery Dagster Plugin

**Automatic lineage and run metadata from Dagster into Orrery** — captures asset definitions, job runs, and dataset-level lineage with no manual instrumentation.

## What you can do

- **Capture asset lineage** — automatically extract upstream/downstream relationships between Dagster assets and external datasets
- **Track run history** — record job execution status and task-level outcomes in Orrery
- **Map Dagster assets to Orrery entities** — assets appear as datasets in your Orrery catalog
- **Works with any Orrery deployment** — self-hosted or Orrery Cloud

## Installation

```bash
pip install orrery-dagster-plugin
```

## Quickstart

Add the Orrery sensor to your Dagster project:

```python
from orrery.ingestion.graph.config import OrreryClientConfig
from orrery_dagster_plugin.sensors.orrery_sensors import (
    OrreryDagsterSourceConfig,
    make_orrery_sensor,
)

config = OrreryDagsterSourceConfig(
    orrery_client_config=OrreryClientConfig(server="http://localhost:8080"),
    dagster_url="http://localhost:3000",
)
orrery_sensor = make_orrery_sensor(config=config)
```

Register it alongside your jobs and assets in your `Definitions`:

```python
from dagster import Definitions

defs = Definitions(
    assets=[...],
    jobs=[...],
    sensors=[orrery_sensor],
)
```

Once the sensor is running, every job run will emit lineage and status metadata to Orrery automatically.

## Links

- [Full documentation](https://docs.orrery.dev/docs/lineage/dagster)
- [Dagster](https://dagster.io/)
- [GitHub](https://github.com/Dxlxz/orrery)
- [Slack community](https://orrery.dev/slack)
