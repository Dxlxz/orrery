"""Engine selection and the errors it produces when the choice is unusable."""

import pathlib
from importlib.metadata import metadata
from typing import Any, Dict
from unittest import mock

import pytest
from click.testing import CliRunner

from orrery.cli import lite_cli
from orrery.cli.lite_cli import DEFAULT_LITE_IMPL, lite
from orrery.configuration.common import ConfigurationError, DynamicTypedConfig
from orrery.ingestion.run.pipeline import Pipeline
from orrery.lite.lite_registry import lite_registry
from orrery.lite.lite_util import LiteLocalConfig, get_orrery_lite

DUCKDB_EXTRA = "duckdb"


def test_both_engines_are_registered() -> None:
    assert set(lite_registry.mapping) == {"sqlite", "duckdb"}
    assert DEFAULT_LITE_IMPL == "sqlite"


def test_missing_duckdb_dependency_names_an_extra_that_exists(
    monkeypatch: pytest.MonkeyPatch,
) -> None:
    # Simulate what the registry records when `import duckdb` fails. Poking the
    # mapping directly is the only way to get there with duckdb installed;
    # monkeypatch restores it for the other tests.
    monkeypatch.setitem(
        lite_registry._mapping,
        "duckdb",
        ModuleNotFoundError("No module named 'duckdb'", name="duckdb"),
    )

    with pytest.raises(ConfigurationError) as exc_info:
        get_orrery_lite({"type": "duckdb", "config": {"file": "unused.duckdb"}})

    # The hint is only useful if the extra it names is real.
    assert f"orrery[{DUCKDB_EXTRA}]" in str(exc_info.value)
    assert DUCKDB_EXTRA in (metadata("orrery").get_all("Provides-Extra") or [])


def test_unknown_engine_in_config_lists_the_valid_ones() -> None:
    with pytest.raises(Exception) as exc_info:
        get_orrery_lite({"type": "mystery", "config": {"file": "unused.db"}})

    message = str(exc_info.value)
    assert "mystery" in message
    assert "sqlite" in message and "duckdb" in message


def test_init_rejects_an_unknown_engine_before_writing_config() -> None:
    result = CliRunner().invoke(lite, ["init", "--type", "mystery"])

    assert result.exit_code != 0
    assert "mystery" in result.output
    assert "sqlite" in result.output and "duckdb" in result.output


def test_import_writes_to_the_configured_instance(
    monkeypatch: pytest.MonkeyPatch, tmp_path: pathlib.Path
) -> None:
    # A DuckDB instance at a non-default path: if `import` fell back to the
    # sink's own defaults it would write a fresh sqlite file elsewhere, and
    # `lite ls` would report an empty store.
    configured = LiteLocalConfig(
        type="duckdb",
        config={"file": str(tmp_path / "configured.duckdb")},
        forward_to=DynamicTypedConfig(
            type="orrery-rest", config={"server": "http://gms.invalid:8080"}
        ),
    )
    monkeypatch.setattr(lite_cli, "get_lite_config", lambda: configured)

    captured: Dict[str, Any] = {}

    def fake_create(config_dict: Dict[str, Any], *args: Any, **kwargs: Any) -> Any:
        captured.update(config_dict)
        return mock.MagicMock()

    monkeypatch.setattr(Pipeline, "create", staticmethod(fake_create))

    result = CliRunner().invoke(lite, ["import", "--file", "unused.json"])

    assert result.exit_code == 0, result.output
    assert captured["sink"]["config"]["type"] == "duckdb"
    assert captured["sink"]["config"]["config"]["file"] == str(
        tmp_path / "configured.duckdb"
    )
    # An import is a local restore; it must not replay the file to the remote
    # sink just because the instance is configured to forward live writes.
    assert not captured["sink"]["config"].get("forward_to")
