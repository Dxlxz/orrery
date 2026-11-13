import logging
from typing import Optional

from orrery.api.entities.external.external_entities import (
    PlatformResourceRepository,
)
from orrery.ingestion.graph.client import OrreryGraph
from orrery.ingestion.source.aws.tag_entities import (
    LakeFormationTagPlatformResource,
    LakeFormationTagPlatformResourceId,
)

logger = logging.getLogger(__name__)


class GluePlatformResourceRepository(
    PlatformResourceRepository[
        LakeFormationTagPlatformResourceId, LakeFormationTagPlatformResource
    ]
):
    """AWS Glue-specific platform resource repository with tag-related operations."""

    def __init__(
        self,
        graph: OrreryGraph,
        platform_instance: Optional[str] = None,
        catalog: Optional[str] = None,
    ):
        super().__init__(graph, platform_instance)
        self.catalog = catalog
