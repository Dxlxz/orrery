<!-- PyPI long description. Keep concise, feature-discovery-first. -->

# Orrery SDK and CLI

**Orrery's ingestion framework and CLI** — pull metadata from 50+ data sources into your catalog, or push it programmatically from your own pipelines and applications.

**Pull-based** integrations crawl your data systems on a schedule: Snowflake, BigQuery, dbt, Looker, Airflow, and many more.

**Push-based** integrations let you emit metadata directly from code as it happens: Python SDK, Java SDK, Spark, Great Expectations, and others.

## What you can do

- **Pull metadata** from databases, warehouses, BI tools, and orchestrators using 50+ ready-made connectors
- **Push metadata** programmatically using the Python or Java SDK
- **Transform and filter** metadata in transit using built-in transformers
- **Schedule and manage** ingestion pipelines via CLI or the Orrery UI
- **Automate lineage, ownership, tags, and documentation** across your data assets

## Supported sources

Snowflake · BigQuery · Redshift · dbt · Databricks · Looker · Tableau · Power BI · Airflow · Spark · Kafka · PostgreSQL · MySQL · Hive · Glue · S3 · Iceberg · Unity Catalog · Sigma · Mode · Superset · Metabase · and [many more](https://docs.orrery.dev/integrations)

## Installation

```bash
pip install orrery
orrery version
```

## Quickstart

### Pull metadata from a source (recipe)

```yaml
# snowflake_recipe.yml
source:
  type: snowflake
  config:
    account_id: my_account
    username: my_user
    password: my_password
    role: ORRERY_ROLE
    warehouse: COMPUTE_WH

sink:
  type: orrery-rest
  config:
    server: http://localhost:8080
```

```bash
orrery ingest -c snowflake_recipe.yml
```

### Emit metadata from code (SDK)

```python
from orrery.sdk import OrreryClient, Dataset

client = OrreryClient.from_env()

dataset = Dataset(platform="snowflake", name="mydb.schema.table")
dataset.set_description("My table description")
dataset.set_owners(["urn:li:corpuser:jane"])

client.entities.upsert(dataset)
```

### Ingest via the Orrery UI

No CLI required — configure and run ingestion directly from the Orrery UI under **Data Sources → Create Source**.

## Ingestion methods

| Method                | Best for                                       |
| --------------------- | ---------------------------------------------- |
| **CLI + YAML recipe** | Scheduled batch ingestion, CI/CD pipelines     |
| **Python SDK**        | Programmatic or event-driven metadata emission |
| **Java SDK**          | JVM-based integrations (Spark, Flink, etc.)    |
| **UI Ingestion**      | One-click setup, no code required              |

## Key CLI commands

```bash
orrery init                          # Connect to a Orrery instance (interactive)
orrery init --username orrery --password orrery  # Quickstart with local defaults
orrery ingest -c recipe.yml          # Run an ingestion pipeline
orrery search "my table"             # Search for entities by keyword
orrery search "*" --filter platform=snowflake --filter entity_type=dataset
orrery graphql --list-operations     # Explore all available GraphQL operations
orrery graphql --query "{ me { corpUser { urn } } }"  # Run a GraphQL query
orrery get --urn <urn>               # Fetch metadata for an entity
orrery delete --urn <urn>            # Delete a metadata entity
orrery timeline --urn <urn>          # View metadata change history
```

## Links

- [Documentation](https://docs.orrery.dev/)
- [Integrations catalog](https://docs.orrery.dev/integrations)
- [Quickstart guide](https://docs.orrery.dev/docs/quickstart)
- [Developing a custom source](https://docs.orrery.dev/docs/metadata-ingestion/adding-source)
- [GitHub](https://github.com/Dxlxz/orrery)
- [Slack community](https://orrery.dev/slack)
