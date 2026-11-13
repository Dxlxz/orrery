"""This example DAG demonstrates how to create and use a OrreryGraph client."""

from datetime import timedelta

import pendulum
from airflow.sdk import dag, task

from orrery.ingestion.graph.client import OrreryGraph, RemovedStatusFilter
from orrery_airflow_plugin.hooks.orrery import OrreryRestHook


@dag(
    start_date=pendulum.datetime(2021, 1, 1, tz="UTC"),
    schedule=timedelta(days=1),
    catchup=False,
)
def orrery_graph_usage_sample_dag():
    @task()
    def use_the_graph():
        graph: OrreryGraph = OrreryRestHook("my_orrery_rest_conn_id").make_graph()
        graph.test_connection()

        # Example usage: Find all soft-deleted BigQuery DEV entities
        # in Orrery, and hard delete them.
        for urn in graph.get_urns_by_filter(
            platform="bigquery",
            env="DEV",
            status=RemovedStatusFilter.ONLY_SOFT_DELETED,
        ):
            graph.hard_delete_entity(urn)

    use_the_graph()


orrery_graph_usage_sample_dag()
