"""BigQueryPlatformResourceHelper: reconciling BigQuery labels with the
PlatformResource entities that back Orrery tags."""

from unittest.mock import create_autospec, patch

import pytest
from pydantic import BaseModel

from orrery.api.entities.platformresource.platform_resource import (
    PlatformResource,
    PlatformResourceKey,
)
from orrery.ingestion.graph.client import OrreryGraph
from orrery.ingestion.source.bigquery_v2.bigquery_platform_resource_helper import (
    BigQueryLabel,
    BigQueryLabelInfo,
    BigQueryPlatformResourceHelper,
)
from orrery.metadata.urns import TagUrn

LABEL = BigQueryLabel(key="team", value="data")
TAG = TagUrn("team:data")
OTHER_TAG = TagUrn("other")
MODULE = "orrery.ingestion.source.bigquery_v2.bigquery_platform_resource_helper"


def label_key() -> PlatformResourceKey:
    return PlatformResourceKey(
        platform="bigquery",
        resource_type="BigQueryLabelInfo",
        platform_instance=None,
        primary_key=LABEL.primary_key(),
    )


def existing_resource(
    tag: TagUrn = TAG, managed_by_orrery: bool = False
) -> PlatformResource:
    return PlatformResource.create(
        key=label_key(),
        value=BigQueryLabelInfo(
            orrery_urn=tag.urn(),
            managed_by_orrery=managed_by_orrery,
            key=LABEL.key,
            value=LABEL.value or "",
        ),
    )


def label_info(resource: PlatformResource) -> BigQueryLabelInfo:
    assert resource.resource_info is not None
    assert resource.resource_info.value is not None
    return resource.resource_info.value.as_pydantic_object(BigQueryLabelInfo)


def helper() -> BigQueryPlatformResourceHelper:
    return BigQueryPlatformResourceHelper(
        "my-project", graph=create_autospec(OrreryGraph, instance=True)
    )


def test_without_graph_a_new_resource_is_created_and_cached() -> None:
    no_graph = BigQueryPlatformResourceHelper("my-project", graph=None)

    first = no_graph.generate_label_platform_resource(LABEL, TAG)
    second = no_graph.generate_label_platform_resource(LABEL, TAG)

    assert label_info(first).orrery_urn == TAG.urn()
    assert label_info(first).managed_by_orrery is True
    assert second.id == first.id
    assert no_graph.get_platform_resource(label_key()) is None


def test_existing_resource_with_same_mapping_is_reused() -> None:
    # One helper for both lookups: the cache is meant to be per helper, and the
    # process-wide sharing is the defect pinned below.
    the_helper = helper()
    with patch(
        f"{MODULE}.PlatformResource.from_orrery", return_value=existing_resource()
    ) as from_orrery:
        resource = the_helper.generate_label_platform_resource(
            LABEL, TAG, managed_by_orrery=False
        )
        again = the_helper.generate_label_platform_resource(
            LABEL, TAG, managed_by_orrery=False
        )

    assert label_info(resource).orrery_urn == TAG.urn()
    assert again is resource
    # Second lookup is served from the cache.
    from_orrery.assert_called_once()


@pytest.mark.xfail(
    strict=True,
    raises=AssertionError,
    reason="platform_resource_cache is a class attribute, so it is shared by every "
    "helper in the process: a later ingestion run (new helper, new graph) is "
    "served the previous run's PlatformResource instead of asking Orrery.",
)
def test_cache_is_per_helper_not_process_wide() -> None:
    with patch(f"{MODULE}.PlatformResource.from_orrery", return_value=None):
        helper().generate_label_platform_resource(LABEL, TAG)

    with patch(
        f"{MODULE}.PlatformResource.from_orrery",
        return_value=existing_resource(tag=OTHER_TAG),
    ):
        found = helper().get_platform_resource(label_key())

    assert found is not None
    assert label_info(found).orrery_urn == OTHER_TAG.urn()


def test_orrery_managed_resource_wins_over_a_conflicting_label() -> None:
    """A mapping Orrery itself created is authoritative: a BigQuery label that
    now points at a different tag does not overwrite it."""
    managed = existing_resource(tag=OTHER_TAG, managed_by_orrery=True)
    with patch(f"{MODULE}.PlatformResource.from_orrery", return_value=managed):
        resource = helper().generate_label_platform_resource(
            LABEL, TAG, managed_by_orrery=False
        )

    assert label_info(resource).orrery_urn == OTHER_TAG.urn()


def test_conflicting_unmanaged_resource_is_rejected() -> None:
    conflicting = existing_resource(tag=OTHER_TAG, managed_by_orrery=False)
    with (
        patch(f"{MODULE}.PlatformResource.from_orrery", return_value=conflicting),
        pytest.raises(ValueError),
    ):
        helper().generate_label_platform_resource(LABEL, TAG, managed_by_orrery=False)


def test_unreadable_existing_value_is_replaced_by_a_new_resource() -> None:
    """A resource written by an older connector with a different value schema
    must not abort tag extraction; it is replaced."""

    class LegacyShape(BaseModel):
        something_else: str

    stale = PlatformResource.create(
        key=label_key(), value=LegacyShape(something_else="x")
    )
    with patch(f"{MODULE}.PlatformResource.from_orrery", return_value=stale):
        resource = helper().generate_label_platform_resource(LABEL, TAG)

    assert label_info(resource).orrery_urn == TAG.urn()


def test_label_without_value_uses_bare_key_as_primary_key() -> None:
    assert BigQueryLabel(key="flag", value=None).primary_key() == "flag"
    assert LABEL.primary_key() == "team/data"
