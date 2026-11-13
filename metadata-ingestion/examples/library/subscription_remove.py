from orrery.sdk import OrreryClient

# Initialize the client
client = OrreryClient(
    server="https://your-orrery-cloud-instance.com", token="your-token"
)

# Unsubscribe from all changes for a dataset
client.subscriptions.unsubscribe(
    urn="urn:li:dataset:(urn:li:dataPlatform:snowflake,purchases,PROD)",
    subscriber_urn="urn:li:corpuser:john.doe",
    # entity_change_types defaults to all existing change types
)
print("Successfully unsubscribed from all dataset notifications")

# Unsubscribe from specific assertion change types
client.subscriptions.unsubscribe(
    urn="urn:li:assertion:your-assertion-id",
    subscriber_urn="urn:li:corpuser:john.doe",
    entity_change_types=[
        "ASSERTION_PASSED"
    ],  # Keep ASSERTION_FAILED and ASSERTION_ERROR
)
print("Successfully unsubscribed from specific assertion change types")

# Unsubscribe a group from assertion changes
client.subscriptions.unsubscribe(
    urn="urn:li:assertion:your-assertion-id",
    subscriber_urn="urn:li:corpGroup:data-team",
    entity_change_types=["ASSERTION_FAILED", "ASSERTION_ERROR"],
)
print("Successfully unsubscribed group from assertion notifications")
