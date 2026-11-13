import logging
from typing import Optional

from pydantic.fields import Field

from orrery.configuration.common import ConfigModel
from orrery.ingestion.api.source_helpers import create_dataset_props_patch_builder
from orrery.ingestion.api.workunit import MetadataWorkUnit
from orrery.metadata.schema_classes import (
    DatasetPropertiesClass,
    SystemMetadataClass,
)

logger = logging.getLogger(__name__)


def convert_dataset_properties_to_patch(
    urn: str,
    aspect: DatasetPropertiesClass,
    system_metadata: Optional[SystemMetadataClass],
) -> MetadataWorkUnit:
    patch_builder = create_dataset_props_patch_builder(urn, aspect, system_metadata)
    mcp = next(iter(patch_builder.build()))
    return MetadataWorkUnit(id=MetadataWorkUnit.generate_workunit_id(mcp), mcp_raw=mcp)


# TODO: Use this in SQLCommonConfig. Currently only used in snowflake
class IncrementalPropertiesConfigMixin(ConfigModel):
    incremental_properties: bool = Field(
        default=False,
        description="When enabled, emits dataset properties as incremental to existing dataset properties "
        "in Orrery. When disabled, re-states dataset properties on each run.",
    )
