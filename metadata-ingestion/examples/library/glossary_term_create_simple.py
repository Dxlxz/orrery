from orrery.sdk import OrreryClient
from orrery.sdk.glossary_term import GlossaryTerm

client = OrreryClient.from_env()

term = GlossaryTerm(
    id="e5f67890",
    display_name="Rate of Return",
    definition="A rate of return (RoR) is the net gain or loss of an investment over a specified time period.",
)

client.entities.upsert(term)
