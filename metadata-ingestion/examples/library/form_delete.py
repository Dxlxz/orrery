from orrery.ingestion.graph.client import OrreryClientConfig, OrreryGraph
from orrery.metadata.urns import FormUrn

graph = OrreryGraph(
    config=OrreryClientConfig(
        server="http://localhost:8080",
    )
)

form_urn = FormUrn("metadata_initiative_1")

# Hard delete the form
graph.delete_entity(urn=str(form_urn), hard=True)
# Delete references to this form (must do)
graph.delete_references_to_urn(urn=str(form_urn), dry_run=False)

print(f"Deleted form {form_urn}")
