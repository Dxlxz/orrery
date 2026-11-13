from orrery.emitter.mcp_patch_builder import MetadataPatchProposal
from orrery.specific.aspect_helpers.ownership import HasOwnershipPatch
from orrery.specific.aspect_helpers.tags import HasTagsPatch
from orrery.specific.aspect_helpers.terms import HasTermsPatch


class DocumentPatchBuilder(
    HasOwnershipPatch, HasTagsPatch, HasTermsPatch, MetadataPatchProposal
):
    """Patch document associations without replacing their containing aspects."""
