from dagster import Definitions, In, Out, PythonObjectDagsterType, job, op

from orrery.ingestion.graph.config import OrreryClientConfig
from orrery.utilities.urns.dataset_urn import DatasetUrn
from orrery_dagster_plugin.sensors.orrery_sensors import (
    OrreryDagsterSourceConfig,
    make_orrery_sensor,
)


@op
def extract():
    results = [1, 2, 3, 4]
    return results


@op(
    ins={
        "data": In(
            dagster_type=PythonObjectDagsterType(list),
            metadata={"orrery.inputs": [DatasetUrn("snowflake", "tableA").urn()]},
        )
    },
    out={
        "result": Out(
            metadata={"orrery.outputs": [DatasetUrn("snowflake", "tableB").urn()]}
        )
    },
)
def transform(data):
    results = []
    for each in data:
        results.append(str(each))
    return results


@job
def do_stuff():
    transform(extract())


config = OrreryDagsterSourceConfig(
    orrery_client_config=OrreryClientConfig(server="http://localhost:8080"),
    dagster_url="http://localhost:3000",
)

orrery_sensor = make_orrery_sensor(config=config)
defs = Definitions(jobs=[do_stuff], sensors=[orrery_sensor])
