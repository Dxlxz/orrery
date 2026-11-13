"""Generic Orrery Ingest via Recipe

This example demonstrates how to load any configuration file and run a
Orrery ingestion pipeline within an Airflow DAG.
"""

from datetime import datetime, timedelta

from airflow import DAG
from airflow.operators.python import PythonOperator

from orrery.configuration.config_loader import load_config_file
from orrery.ingestion.run.pipeline import Pipeline

default_args = {
    "owner": "airflow",
    "depends_on_past": False,
    "email": ["jdoe@example.com"],
    "email_on_failure": False,
    "email_on_retry": False,
    "retries": 1,
    "retry_delay": timedelta(minutes=5),
    "execution_timeout": timedelta(minutes=120),
}


def orrery_recipe():
    # Note that this will also resolve environment variables in the recipe.
    config = load_config_file("path/to/recipe.yml")

    pipeline = Pipeline.create(config)
    pipeline.run()
    pipeline.raise_from_status()


with DAG(
    "orrery_ingest_using_recipe",
    default_args=default_args,
    description="An example DAG which runs a Orrery ingestion recipe",
    schedule=timedelta(days=1),
    start_date=datetime(2023, 1, 1),
    catchup=False,
) as dag:
    ingest_task = PythonOperator(
        task_id="ingest_using_recipe",
        python_callable=orrery_recipe,
    )
