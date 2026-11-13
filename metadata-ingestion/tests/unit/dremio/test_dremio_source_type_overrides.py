from orrery.ingestion.source.dremio.dremio_config import (
    DremioSourceTypeOverride,
)
from orrery.ingestion.source.dremio.dremio_orrery_source_mapping import (
    DremioToOrrerySourceTypeMapping,
)


def test_no_overrides_uses_builtin_mapping():
    mapper = DremioToOrrerySourceTypeMapping()
    assert mapper.lookup_orrery_platform("S3") == "s3"
    assert mapper.lookup_category("S3") == "file_object_storage"
    assert mapper.lookup_orrery_platform("SNOWFLAKE") == "snowflake"
    assert mapper.lookup_category("SNOWFLAKE") == "database"


def test_unknown_source_type_without_override_falls_back_to_lowercase():
    mapper = DremioToOrrerySourceTypeMapping()
    assert mapper.lookup_orrery_platform("MYORG_KAFKA") == "myorg_kafka"
    assert mapper.lookup_category("MYORG_KAFKA") == "unknown"


def test_override_registers_unknown_source_type():
    mapper = DremioToOrrerySourceTypeMapping(
        extra_mappings={
            "MYORG_KAFKA": DremioSourceTypeOverride(
                platform="kafka", category="database"
            ),
        },
    )
    assert mapper.lookup_orrery_platform("MYORG_KAFKA") == "kafka"
    assert mapper.lookup_category("MYORG_KAFKA") == "database"


def test_override_lookup_is_case_insensitive():
    mapper = DremioToOrrerySourceTypeMapping(
        extra_mappings={
            "myorg_kafka": DremioSourceTypeOverride(
                platform="kafka", category="database"
            ),
        },
    )
    assert mapper.lookup_orrery_platform("MyOrg_Kafka") == "kafka"
    assert mapper.lookup_category("myorg_kafka") == "database"


def test_override_wins_over_builtin():
    mapper = DremioToOrrerySourceTypeMapping(
        extra_mappings={
            "NESSIE": DremioSourceTypeOverride(platform="iceberg-rest"),
        },
    )
    assert mapper.lookup_orrery_platform("NESSIE") == "iceberg-rest"


def test_override_without_category_falls_back_to_unknown():
    mapper = DremioToOrrerySourceTypeMapping(
        extra_mappings={
            "MYORG_THING": DremioSourceTypeOverride(platform="thing"),
        },
    )
    assert mapper.lookup_orrery_platform("MYORG_THING") == "thing"
    assert mapper.lookup_category("MYORG_THING") == "unknown"


def test_overrides_do_not_leak_across_instances():
    # The predecessor add_mapping mutated class state; pin instance isolation.
    mapper_a = DremioToOrrerySourceTypeMapping(
        extra_mappings={
            "MYORG_THING": DremioSourceTypeOverride(
                platform="thing", category="database"
            ),
        },
    )
    mapper_b = DremioToOrrerySourceTypeMapping()
    assert mapper_a.lookup_orrery_platform("MYORG_THING") == "thing"
    assert mapper_b.lookup_orrery_platform("MYORG_THING") == "myorg_thing"
    assert mapper_b.lookup_category("MYORG_THING") == "unknown"


def test_static_lookups_unaffected_by_overrides():
    DremioToOrrerySourceTypeMapping(
        extra_mappings={
            "MYORG_THING": DremioSourceTypeOverride(
                platform="thing", category="database"
            ),
        },
    )
    assert (
        DremioToOrrerySourceTypeMapping.get_orrery_platform("MYORG_THING")
        == "myorg_thing"
    )
    assert DremioToOrrerySourceTypeMapping.get_category("MYORG_THING") == "unknown"


def test_file_object_storage_category_override():
    mapper = DremioToOrrerySourceTypeMapping(
        extra_mappings={
            "MYORG_BLOB": DremioSourceTypeOverride(
                platform="abfs", category="file_object_storage"
            ),
        },
    )
    assert mapper.lookup_category("MYORG_BLOB") == "file_object_storage"
