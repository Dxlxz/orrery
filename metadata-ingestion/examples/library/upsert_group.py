from orrery.api.entities.corpgroup.corpgroup import (
    CorpGroup,
    CorpGroupGenerationConfig,
)
from orrery.ingestion.graph.client import OrreryGraph, OrreryGraphConfig
from orrery.metadata.urns import CorpUserUrn

group_email = "foogroup@orrery.dev"
group = CorpGroup(
    id=group_email,
    owners=[str(CorpUserUrn("orrery"))],
    members=[
        str(CorpUserUrn("bar@orrery.dev")),
        str(CorpUserUrn("joe@orrery.dev")),
    ],
    display_name="Foo Group",
    email=group_email,
    description="Software engineering team",
    slack="@foogroup",
)

# Create graph client
orrery_graph = OrreryGraph(OrreryGraphConfig(server="http://localhost:8080"))

for event in group.generate_mcp(
    generation_config=CorpGroupGenerationConfig(
        override_editable=False, orrery_graph=orrery_graph
    )
):
    orrery_graph.emit(event)
print(f"Upserted group {group.urn}")
