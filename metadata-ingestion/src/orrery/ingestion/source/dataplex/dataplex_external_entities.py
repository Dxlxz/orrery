from typing import Optional

from orrery.api.entities.external.external_entities import (
    ExternalEntity,
    ExternalEntityId,
    LinkedResourceSet,
)
from orrery.api.entities.platformresource.platform_resource import (
    PlatformResource,
    PlatformResourceKey,
)

DATAPLEX_PLATFORM = "dataplex"
# Must byte-match the resource type + aspect key the Orrery sync-back persists
# (orrery-fork orrery_integrations/propagation/dataplex).
DATAPLEX_ASPECT_RESOURCE_TYPE = "DataplexAspectPlatformResource"
GLOSSARY_TERMS_ASPECT_KEY = "orrery-glossary-terms"


class DataplexAspectId(ExternalEntityId):
    """Identity of a sync-back-authored Dataplex aspect/link platform resource.

    Mirrors the fork's key scheme: primary key ``{entry_name}/{aspect_key}[/{field_key}]``
    on platform ``dataplex``, resource type ``DataplexAspectPlatformResource``.
    """

    aspect_key: str
    entry_name: str
    field_key: Optional[str] = None
    # Set by the repository once a matching resource is found in Orrery.
    persisted: bool = False

    def to_platform_resource_key(self) -> PlatformResourceKey:
        primary_key = f"{self.entry_name}/{self.aspect_key}"
        if self.field_key is not None:
            primary_key = f"{primary_key}/{self.field_key}"
        return PlatformResourceKey(
            platform=DATAPLEX_PLATFORM,
            resource_type=DATAPLEX_ASPECT_RESOURCE_TYPE,
            primary_key=primary_key,
        )


class DataplexAspectPlatformResource(ExternalEntity):
    """Ingestion-side view of a sync-back platform resource.

    Field names deliberately match the fork's persisted ``DataplexAspectInfo`` value
    so the generic repository can deserialize it via ``as_pydantic_object`` (which
    does not validate the schema ref by default).
    """

    orrery_urn: str
    managed_by_orrery: bool
    aspect_key: str
    entry_name: str
    field_key: Optional[str] = None

    def get_id(self) -> ExternalEntityId:
        return DataplexAspectId(
            aspect_key=self.aspect_key,
            entry_name=self.entry_name,
            field_key=self.field_key,
        )

    def is_managed_by_orrery(self) -> bool:
        return self.managed_by_orrery

    def orrery_linked_resources(self) -> LinkedResourceSet:
        # A default (not-yet-linked) entity has an empty ``orrery_urn``; return an
        # empty set rather than [""] so a "" never masquerades as a URN (which would
        # blow up LinkedResourceSet.add / Urn.from_string). Mirrors Unity Catalog.
        return LinkedResourceSet(urns=[self.orrery_urn] if self.orrery_urn else [])

    def as_platform_resource(self) -> PlatformResource:
        return PlatformResource.create(
            key=self.get_id().to_platform_resource_key(),
            secondary_keys=[self.orrery_urn],
            value=self,
        )

    @classmethod
    def create_default(
        cls, entity_id: ExternalEntityId, managed_by_orrery: bool
    ) -> "DataplexAspectPlatformResource":
        assert isinstance(entity_id, DataplexAspectId)
        return cls(
            orrery_urn="",
            managed_by_orrery=managed_by_orrery,
            aspect_key=entity_id.aspect_key,
            entry_name=entity_id.entry_name,
            field_key=entity_id.field_key,
        )

    @classmethod
    def from_external(
        cls, entry_name: str, native_term_urn: str
    ) -> "DataplexAspectPlatformResource":
        """Build a resource marking an externally-authored term link as unmanaged."""
        return cls(
            orrery_urn=native_term_urn,
            managed_by_orrery=False,
            aspect_key=GLOSSARY_TERMS_ASPECT_KEY,
            entry_name=entry_name,
            field_key=native_term_urn,
        )
