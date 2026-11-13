# Orrery Airflow Plugin Example DAGs

Example DAGs demonstrating various features of the Orrery Airflow plugin. All
examples target Airflow 3.1+.

## Example Categories

### 1. Lineage Collection

Examples showing how to automatically collect lineage from your Airflow DAGs:

- **lineage_backend_demo.py**: Basic lineage collection using `inlets` and `outlets`
- **lineage_backend_taskflow_demo.py**: Lineage with TaskFlow API (`@task` decorator)

### 2. Metadata Ingestion

Examples showing how to ingest metadata from data sources into Orrery:

- **snowflake_sample_dag.py**: Ingest Snowflake metadata
- **mysql_sample_dag.py**: Ingest MySQL metadata
- **generic_recipe_sample_dag.py**: Run any Orrery ingestion recipe

### 3. Advanced Features

- **lineage_emission_dag.py**: Directly emit custom lineage as an `UpstreamLineage` MCP using `OrreryEmitterOperator`
- **graph_usage_sample_dag.py**: Use the `OrreryGraph` client from within a DAG

## Running the Examples

1. Copy the example to your Airflow DAGs folder:

   ```bash
   cp lineage_backend_demo.py $AIRFLOW_HOME/dags/
   ```

2. Configure the Orrery connection in Airflow:

   ```bash
   airflow connections add orrery_rest_default \
     --conn-type orrery-rest \
     --conn-host http://localhost:8080
   ```

3. Enable the DAG in the Airflow UI or trigger it manually:
   ```bash
   airflow dags trigger orrery_lineage_backend_demo
   ```

## Additional Resources

- [Orrery Airflow Plugin Documentation](https://orrery.dev/docs/lineage/airflow/)
- [Orrery Metadata Ingestion](https://orrery.dev/docs/metadata-ingestion/)
