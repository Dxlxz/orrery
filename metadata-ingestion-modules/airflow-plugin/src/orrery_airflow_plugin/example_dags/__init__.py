"""
Orrery Airflow Plugin Example DAGs

Example DAGs demonstrating various features of the Orrery Airflow plugin.

## Available Examples

### Lineage Collection
- `lineage_backend_demo.py`: Basic lineage collection using inlets/outlets
- `lineage_backend_taskflow_demo.py`: Lineage collection with TaskFlow API

### Data Ingestion
- `snowflake_sample_dag.py`: Ingest Snowflake metadata into Orrery
- `mysql_sample_dag.py`: Ingest MySQL metadata into Orrery
- `generic_recipe_sample_dag.py`: Run any Orrery recipe from Airflow

### Advanced Features
- `lineage_emission_dag.py`: Custom lineage emission with OrreryEmitterOperator
- `graph_usage_sample_dag.py`: Use the OrreryGraph client from within a DAG
"""
