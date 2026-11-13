from dagster import (
    AssetIn,
    AssetOut,
    Definitions,
    Output,
    asset,
    define_asset_job,
    multi_asset,
)

from orrery.ingestion.graph.config import OrreryClientConfig
from orrery.utilities.urns.dataset_urn import DatasetUrn
from orrery_dagster_plugin.sensors.orrery_sensors import (
    OrreryDagsterSourceConfig,
    make_orrery_sensor,
)


@multi_asset(
    outs={
        "extract": AssetOut(
            metadata={"orrery.outputs": [DatasetUrn("snowflake", "tableD").urn()]}
        ),
    }
)
def extract():
    results = [1, 2, 3, 4]
    metadata = {
        "num_record": len(results),
    }
    return Output(value=results, metadata=metadata)


@asset(
    ins={
        "extract": AssetIn(
            "extract",
            metadata={"orrery.inputs": [DatasetUrn("snowflake", "tableC").urn()]},
        )
    }
)
def transform(extract):
    results = []
    for each in extract:
        results.append(str(each))
    return results


assets_job = define_asset_job(name="assets_job")

config = OrreryDagsterSourceConfig(
    orrery_client_config=OrreryClientConfig(server="http://localhost:8080"),
    dagster_url="http://localhost:3000",
)

orrery_sensor = make_orrery_sensor(config=config)

defs = Definitions(
    assets=[extract, transform], jobs=[assets_job], sensors=[orrery_sensor]
)
