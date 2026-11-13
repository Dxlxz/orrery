from orrery.emitter.mcp_builder import DatabaseKey
from orrery.sdk import Container, OrreryClient

client = OrreryClient.from_env()

container = Container(
    container_key=DatabaseKey(platform="snowflake", database="my_database"),
    display_name="MY_DATABASE",
)

client.entities.upsert(container)
