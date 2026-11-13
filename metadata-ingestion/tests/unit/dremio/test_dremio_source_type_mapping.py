"""Pin canonical Dremio source-type strings against Orrery platform names
and categories that URN construction depends on."""

import pytest

from orrery.ingestion.source.dremio.dremio_orrery_source_mapping import (
    DremioToOrrerySourceTypeMapping,
)


@pytest.mark.parametrize(
    "dremio_type,expected_platform",
    [
        ("BIGQUERY", "bigquery"),
        ("RESTCATALOG", "iceberg"),
        ("SAPHANA", "hana"),
        ("SNOWFLAKEOPENCATALOG", "iceberg"),
        ("UNITY", "databricks"),
    ],
)
def test_new_source_types_map_to_expected_platforms(
    dremio_type: str, expected_platform: str
) -> None:
    assert (
        DremioToOrrerySourceTypeMapping.get_orrery_platform(dremio_type)
        == expected_platform
    )


@pytest.mark.parametrize(
    "dremio_type",
    ["BIGQUERY", "RESTCATALOG", "SAPHANA", "SNOWFLAKEOPENCATALOG", "UNITY"],
)
def test_new_source_types_categorized_as_database(dremio_type: str) -> None:
    # Dot-notation path sources, not slash-notation file paths.
    assert DremioToOrrerySourceTypeMapping.get_category(dremio_type) == "database"


def test_case_insensitive_lookup() -> None:
    assert (
        DremioToOrrerySourceTypeMapping.get_orrery_platform("restcatalog")
        == "iceberg"
    )
    assert DremioToOrrerySourceTypeMapping.get_category("restcatalog") == "database"


def test_unknown_source_type_falls_back_to_lowercase_name() -> None:
    # ARP / custom sources rely on this lowercase+unknown fallback.
    assert (
        DremioToOrrerySourceTypeMapping.get_orrery_platform("FUTURE_SOURCE")
        == "future_source"
    )
    assert DremioToOrrerySourceTypeMapping.get_category("FUTURE_SOURCE") == "unknown"


def test_azure_storage_resolves_to_database_despite_dual_membership() -> None:
    # AZURE_STORAGE is in both sets; "database" wins via get_category check order.
    assert DremioToOrrerySourceTypeMapping.get_category("AZURE_STORAGE") == "database"
