from typing import Type, TypeVar

from orrery.emitter.mcp import MetadataChangeProposalWrapper
from orrery.ingestion.api.workunit import MetadataWorkUnit
from orrery.metadata.schema_classes import _Aspect

_AspectT = TypeVar("_AspectT", bound=_Aspect)


def aspect_of(wu: MetadataWorkUnit) -> _Aspect:
    assert isinstance(wu.metadata, MetadataChangeProposalWrapper)
    assert wu.metadata.aspect is not None
    return wu.metadata.aspect


def aspect_as(wu: MetadataWorkUnit, aspect_type: Type[_AspectT]) -> _AspectT:
    assert isinstance(wu.metadata, MetadataChangeProposalWrapper)
    assert isinstance(wu.metadata.aspect, aspect_type)
    return wu.metadata.aspect


def entity_urn_of(wu: MetadataWorkUnit) -> str:
    assert isinstance(wu.metadata, MetadataChangeProposalWrapper)
    assert wu.metadata.entityUrn is not None
    return wu.metadata.entityUrn
