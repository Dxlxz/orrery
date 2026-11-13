from orrery.api.graphql import Operation

ORRERY_HOST = "https//:org.orrery.dev/gms"
ORRERY_TOKEN = "<your-orrery-access-token"

dataset_urn = "urn:li:dataset:(urn:li:dataPlatform:hive,fct_users_created,PROD)"

operation_client = Operation(
    orrery_host=ORRERY_HOST,
    orrery_token=ORRERY_TOKEN,
)

# Query for changes to the Dataset.
operations = operation_client.query_operations(
    urn=dataset_urn,
    # limit=5,
    # start_time_millis=<timestamp>,
    # end_time_millis=<timestamo>
)
