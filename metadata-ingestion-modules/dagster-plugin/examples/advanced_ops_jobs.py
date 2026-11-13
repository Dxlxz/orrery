from typing import Dict

from dagster import (
    Definitions,
    In,
    Out,
    PythonObjectDagsterType,
    RunStatusSensorContext,
    job,
    op,
)

from orrery.ingestion.graph.client import OrreryClientConfig, OrreryGraph
from orrery.utilities.urns.dataset_urn import DatasetUrn
from orrery_dagster_plugin.client.dagster_generator import (
    DagsterGenerator,
    DatasetLineage,
)
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


def asset_lineage_extractor(
    context: RunStatusSensorContext,
    dagster_generator: DagsterGenerator,
    graph: OrreryGraph,
) -> Dict[str, DatasetLineage]:
    from dagster._core.events import DagsterEventType

    logs = context.instance.all_logs(
        context.dagster_run.run_id,
        {
            DagsterEventType.ASSET_MATERIALIZATION,
            DagsterEventType.ASSET_OBSERVATION,
            DagsterEventType.HANDLED_OUTPUT,
            DagsterEventType.LOADED_INPUT,
        },
    )

    dataset_lineage: Dict[str, DatasetLineage] = {}

    for log in logs:
        if not log.dagster_event or not log.step_key:
            continue

        if log.dagster_event.event_type == DagsterEventType.ASSET_MATERIALIZATION:
            if log.step_key not in dataset_lineage:
                dataset_lineage[log.step_key] = DatasetLineage(set(), set())

            materialization = log.asset_materialization
            if not materialization:
                continue

            properties = {
                key: str(value) for (key, value) in materialization.metadata.items()
            }
            asset_key = materialization.asset_key.path
            dataset_urn = dagster_generator.emit_asset(
                graph, asset_key, materialization.description, properties
            )
            dataset_lineage[log.step_key].outputs.add(dataset_urn)

    return dataset_lineage


config = OrreryDagsterSourceConfig(
    orrery_client_config=OrreryClientConfig(server="http://localhost:8080"),
    dagster_url="http://localhost:3000",
    asset_lineage_extractor=asset_lineage_extractor,
)
orrery_sensor = make_orrery_sensor(config=config)
defs = Definitions(jobs=[do_stuff], sensors=[orrery_sensor])
