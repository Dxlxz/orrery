from orrery.ingestion.graph.client import OrreryClientConfig, OrreryGraph

graph = OrreryGraph(
    config=OrreryClientConfig(
        server="http://localhost:8080",
    )
)

assertion_urn = "urn:li:assertion:my-assertion"

# Delete the Assertion
graph.delete_entity(urn=assertion_urn, hard=True)

print(f"Deleted assertion {assertion_urn}")
