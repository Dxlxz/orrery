from typing import List, Tuple

from prefect import flow, task

from prefect_orrery.entities import Dataset
from prefect_orrery.orrery_emitter import OrreryEmitter

orrery_emitter_block = OrreryEmitter.load("orrery-emitter-test")


@task(name="Extract", description="Extract the data")
def extract() -> str:
    data = "This is data"
    return data


@task(name="Transform", description="Transform the data")
def transform(
    data: str, orrery_emitter: OrreryEmitter
) -> Tuple[List[str], OrreryEmitter]:
    data_list_str = data.split(" ")
    orrery_emitter.add_task(
        inputs=[
            Dataset(
                platform="snowflake",
                name="mydb.schema.tableA",
                env=orrery_emitter.env,
                platform_instance=orrery_emitter.platform_instance,
            )
        ],
        outputs=[
            Dataset(
                platform="snowflake",
                name="mydb.schema.tableB",
                env=orrery_emitter.env,
                platform_instance=orrery_emitter.platform_instance,
            )
        ],
    )
    return data_list_str, orrery_emitter


@flow(name="ETL", description="Extract transform load flow")
def etl() -> None:
    orrery_emitter = orrery_emitter_block
    data = extract()
    return_value = transform(data, orrery_emitter)  # type: ignore
    emitter = return_value[1]
    emitter.emit_flow()


etl()
