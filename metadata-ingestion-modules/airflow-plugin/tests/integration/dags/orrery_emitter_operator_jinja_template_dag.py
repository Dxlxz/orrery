from datetime import datetime, timedelta

from airflow import DAG

from orrery.emitter.mcp import MetadataChangeProposalWrapper
from orrery.metadata.com.linkedin.pegasus2avro.mxe import MetadataChangeEvent
from orrery.metadata.schema_classes import (
    BrowsePathEntryClass,
    BrowsePathsV2Class,
    DatasetPropertiesClass,
    DatasetSnapshotClass,
)
from orrery_airflow_plugin.operators.orrery import OrreryEmitterOperator

default_args = {
    "owner": "airflow",
    "depends_on_past": False,
    "start_date": datetime(2023, 1, 1),
    "email": ["jdoe@example.com"],
    "email_on_failure": False,
    "execution_timeout": timedelta(minutes=5),
}


with DAG(
    "orrery_emitter_operator_jinja_template_dag",
    default_args=default_args,
    description="An example dag with jinja template",
    schedule=None,
    tags=["example_tag"],
    catchup=False,
):
    add_custom_properties = OrreryEmitterOperator(
        task_id="orrery_emitter_operator_jinja_template_dag_task",
        mces=[
            MetadataChangeProposalWrapper(
                entityUrn="urn:li:dataset:(urn:li:dataPlatform:hive,orrery.example.mcpw_example,DEV)",
                aspect=BrowsePathsV2Class(
                    path=[BrowsePathEntryClass("mcpw_example {{ ds }}")],
                ),
            ),
            MetadataChangeProposalWrapper(
                entityUrn="urn:li:dataset:(urn:li:dataPlatform:hive,orrery.example.mcpw_example_{{ ts_nodash }},DEV)",
                aspect=BrowsePathsV2Class(
                    path=[BrowsePathEntryClass("mcpw_example {{ ds }}")],
                ),
            ),
            MetadataChangeEvent(
                proposedSnapshot=DatasetSnapshotClass(
                    urn="urn:li:dataset:(urn:li:dataPlatform:hive,orrery.example.lineage_example,DEV)",
                    aspects=[
                        DatasetPropertiesClass(
                            customProperties={"jinjaTemplate": "{{ ds }}"}
                        )
                    ],
                ),
            ),
            MetadataChangeEvent(
                proposedSnapshot=DatasetSnapshotClass(
                    urn="urn:li:dataset:(urn:li:dataPlatform:hive,orrery.example.lineage_example_{{ ts_nodash }},DEV)",
                    aspects=[
                        DatasetPropertiesClass(
                            customProperties={"jinjaTemplate": "{{ ds }}"}
                        )
                    ],
                ),
            ),
        ],
        orrery_conn_id="orrery_file_default",
    )
