from orrery.api.graphql import Operation

ORRERY_HOST = "https//:org.orrery.dev/gms"
ORRERY_TOKEN = "<your-orrery-access-token"

dataset_urn = "urn:li:dataset:(urn:li:dataPlatform:hive,fct_users_created,PROD)"

operation_client = Operation(
    orrery_host=ORRERY_HOST,
    orrery_token=ORRERY_TOKEN,
)

operation_type = "INSERT"
source_type = "DATA_PROCESS"  # Source of the operation (data platform or DAG task)

# Report a change operation for the Dataset.
operation_client.report_operation(
    urn=dataset_urn, operation_type=operation_type, source_type=source_type
)
