from dagster import Definitions

from orrery.ingestion.graph.client import OrreryClientConfig
from orrery_dagster_plugin.sensors.orrery_sensors import (
    OrreryDagsterSourceConfig,
    make_orrery_sensor,
)

config = OrreryDagsterSourceConfig(
    orrery_client_config=OrreryClientConfig(
        server="https://your_orrery_url/gms", token="your_orrery_token"
    ),
    dagster_url="https://my-dagster-cloud.dagster.cloud",
)

orrery_sensor = make_orrery_sensor(config=config)

defs = Definitions(
    sensors=[orrery_sensor],
)
