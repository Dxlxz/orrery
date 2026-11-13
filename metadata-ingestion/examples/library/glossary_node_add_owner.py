from orrery.metadata.urns import CorpUserUrn
from orrery.sdk import GlossaryNodeUrn, OrreryClient

client = OrreryClient.from_env()

node = client.entities.get(GlossaryNodeUrn("7f3d2c1a"))
node.add_owner(CorpUserUrn("jdoe"))
client.entities.upsert(node)
