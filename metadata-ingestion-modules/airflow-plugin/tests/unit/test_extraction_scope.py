"""Tests for the Orrery extraction scope that prevents duplicated OpenLineage work.

The Orrery listener calls the operator's ``get_openlineage_facets_on_*`` methods
in-process, while Airflow's OpenLineage provider calls the same methods inside its
own ``os.fork()``. Those methods are not side-effect free: they open warehouse
connections and (for Snowflake) emit OpenLineage query events. These tests pin the
guards that keep Orrery's pass from re-running work it does not consume.
"""

from types import SimpleNamespace
from typing import Any, cast
from unittest import mock

import pytest
from openlineage.client.event_v2 import Dataset

from orrery_airflow_plugin._constants import ORRERY_SQL_PARSING_RESULT_KEY
from orrery_airflow_plugin.airflow3 import _sql_parser_patch
from orrery_airflow_plugin.airflow3._extraction_scope import (
    in_orrery_extraction,
    orrery_extraction_scope,
)

_PATCH_MOD = "orrery_airflow_plugin.airflow3._sql_parser_patch"


@pytest.fixture
def fake_parse_result():
    return SimpleNamespace(
        in_tables=["urn:li:dataset:(urn:li:dataPlatform:snowflake,db.sch.src,PROD)"],
        out_tables=["urn:li:dataset:(urn:li:dataPlatform:snowflake,db.sch.dst,PROD)"],
        column_lineage=None,
        debug_info=SimpleNamespace(error=None, table_error=None),
    )


@pytest.fixture
def sql_parser_env(fake_parse_result):
    """Drive the patched SQLParser method with the original parser stubbed out."""
    provider_dataset = Dataset(namespace="snowflake://db", name="db.sch.src")
    original = mock.MagicMock(
        return_value=_sql_parser_patch.OperatorLineage(
            inputs=[provider_dataset],
            outputs=[provider_dataset],
            job_facets={},
            run_facets={"externalQuery": object()},
        )
    )
    config = SimpleNamespace(
        disable_openlineage_plugin=False,
        enable_multi_statement_sql_parsing=False,
    )
    with (
        mock.patch.object(_sql_parser_patch, "_original_sql_parser_method", original),
        mock.patch(
            f"{_PATCH_MOD}.parse_sql_with_orrery", return_value=fake_parse_result
        ),
        mock.patch(
            "orrery_airflow_plugin._config.get_lineage_config", return_value=config
        ),
        mock.patch(
            "orrery_airflow_plugin.orrery_listener.get_airflow_plugin_listener",
            return_value=None,
        ),
        mock.patch(f"{_PATCH_MOD}.get_configured_env", return_value="PROD"),
    ):
        yield original


def _call_patched_parser():
    return _sql_parser_patch._orrery_generate_openlineage_metadata_from_sql(
        self=SimpleNamespace(dialect="snowflake", default_schema="sch"),
        sql="insert into dst select * from src",
        hook=mock.MagicMock(),
        # The patch annotates this as ``dict`` but the provider passes a
        # ``DatabaseInfo``; the code only reads ``.scheme`` / ``.database``.
        database_info=cast(dict, SimpleNamespace(scheme="snowflake", database="db")),
    )


def _installed_guard_operator_class() -> Any:
    """Return SQLExecuteQueryOperator with the guard installed the way production does it.

    Importing _airflow_compat installs the guard process-wide, and patch() is a no-op
    once it is in place, so installing and removing it per test would silently depend
    on test order. Rely on the production install and assert it happened instead.
    """
    from airflow.providers.common.sql.operators.sql import SQLExecuteQueryOperator

    from orrery_airflow_plugin.airflow3 import _airflow_compat  # noqa: F401
    from orrery_airflow_plugin.airflow3._sql_operator_complete_patch import (
        _PATCHED_MARKER,
    )

    assert getattr(SQLExecuteQueryOperator, _PATCHED_MARKER, False), (
        "importing _airflow_compat should install the common-sql completion guard"
    )
    return SQLExecuteQueryOperator


def test_scope_defaults_to_inactive():
    assert in_orrery_extraction() is False


def test_scope_resets_after_exit():
    with orrery_extraction_scope():
        assert in_orrery_extraction() is True
    assert in_orrery_extraction() is False


def test_original_parser_runs_outside_orrery_scope(sql_parser_env):
    """The OpenLineage provider's own pass must keep hitting the warehouse."""
    result = _call_patched_parser()

    assert sql_parser_env.call_count == 1
    assert sql_parser_env.call_args.kwargs["use_connection"] is True
    # The provider's own iolets and facets survive untouched.
    assert [d.name for d in result.inputs] == ["db.sch.src"]
    assert "externalQuery" in result.run_facets
    assert ORRERY_SQL_PARSING_RESULT_KEY in result.run_facets


def test_original_parser_skipped_inside_orrery_scope(sql_parser_env):
    """Orrery's pass must not re-run the provider parser at all.

    Its only warehouse-touching work is the provider parser, and Orrery takes its
    URNs from its own SQL-parsing facet, which is dialect- and quote-aware.
    """
    with orrery_extraction_scope():
        _call_patched_parser()

    assert sql_parser_env.call_count == 0


def test_orrery_scope_returns_facet_without_provider_iolets(sql_parser_env):
    """URNs come from the Orrery facet alone.

    Reconstructing the warehouse's stored casing from a parse tree is not possible:
    the parser strips quotes, so `FROM MySchema.MyTable` and `FROM "MySchema"."MyTable"`
    are indistinguishable while needing opposite folding on a case-folding database.
    The listener reads in_tables/out_tables from this facet, which is produced by a
    dialect- and quote-aware parser, so no reconstruction is attempted.
    """
    with orrery_extraction_scope():
        result = _call_patched_parser()

    assert result.inputs == []
    assert result.outputs == []
    assert ORRERY_SQL_PARSING_RESULT_KEY in result.run_facets


class TestSqlOperatorCompleteGuard:
    """``get_openlineage_facets_on_complete`` must not run side effects for Orrery.

    For common-sql operators the complete pass is ``get_openlineage_facets_on_start()``
    merged with ``hook.get_openlineage_database_specific_lineage()``. Orrery reads
    none of the latter, but on Snowflake producing it emits OpenLineage query events
    straight to the provider's transport with a fresh runId.
    """

    @pytest.fixture
    def guarded_operator_class(self) -> Any:
        return _installed_guard_operator_class()

    @staticmethod
    def _fake_operator() -> Any:
        hook = mock.MagicMock()
        hook.get_openlineage_database_specific_lineage.return_value = None
        start_lineage: Any = _sql_parser_patch.OperatorLineage(
            inputs=[], outputs=[], job_facets={}, run_facets={}
        )
        return SimpleNamespace(
            get_openlineage_facets_on_start=mock.MagicMock(return_value=start_lineage),
            get_db_hook=mock.MagicMock(return_value=hook),
            _hook=hook,
            _start_lineage=start_lineage,
        )

    def test_database_specific_lineage_skipped_inside_scope(
        self, guarded_operator_class
    ):
        op = self._fake_operator()

        with orrery_extraction_scope():
            result = guarded_operator_class.get_openlineage_facets_on_complete(
                op, mock.MagicMock()
            )

        op._hook.get_openlineage_database_specific_lineage.assert_not_called()
        assert result is op._start_lineage

    def test_database_specific_lineage_runs_outside_scope(self, guarded_operator_class):
        op = self._fake_operator()

        guarded_operator_class.get_openlineage_facets_on_complete(op, mock.MagicMock())

        op._hook.get_openlineage_database_specific_lineage.assert_called_once()

    def test_subclass_override_still_wins(self, guarded_operator_class):
        """Orrery's own operator patches install on subclasses and must not be bypassed."""
        sentinel = object()
        subclass: Any = type(
            "FakePatchedOperator",
            (guarded_operator_class,),
            {
                "get_openlineage_facets_on_complete": lambda self, ti: sentinel,
            },
        )
        op = self._fake_operator()

        with orrery_extraction_scope():
            result = subclass.get_openlineage_facets_on_complete(op, mock.MagicMock())

        assert result is sentinel

    def test_operator_patch_registered_later_captures_the_guard(
        self, guarded_operator_class
    ):
        """Ordering constraint: operator patches capture their original by MRO lookup.

        ``patch_teradata_operator`` does ``original = TeradataOperator.get_openlineage_
        facets_on_complete`` and installs its wrapper on the subclass. If the guard is
        installed after that, the subclass entry shadows it and the guard never runs.
        Installing the guard first means any later operator patch routes through it.
        """
        subclass: Any = type("FakeTeradataOperator", (guarded_operator_class,), {})
        captured_original = subclass.get_openlineage_facets_on_complete

        calls = []

        def teradata_style_wrapper(self, ti):
            calls.append("teradata")
            return captured_original(self, ti)

        subclass.get_openlineage_facets_on_complete = teradata_style_wrapper
        op = self._fake_operator()

        with orrery_extraction_scope():
            subclass.get_openlineage_facets_on_complete(op, mock.MagicMock())

        assert calls == ["teradata"]
        op._hook.get_openlineage_database_specific_lineage.assert_not_called()

    def test_patch_is_idempotent(self, guarded_operator_class):
        from orrery_airflow_plugin.airflow3._sql_operator_complete_patch import (
            SqlOperatorCompletePatch,
        )

        before = guarded_operator_class.get_openlineage_facets_on_complete
        SqlOperatorCompletePatch().patch()

        assert guarded_operator_class.get_openlineage_facets_on_complete is before


class TestListenerEntersScope:
    def test_facet_method_sees_active_scope(self):
        """The listener must mark its own facet calls, or neither guard engages."""
        from orrery_airflow_plugin.airflow3.orrery_listener import OrreryListener

        observed = []

        def get_openlineage_facets_on_start():
            observed.append(in_orrery_extraction())
            return None

        task = SimpleNamespace(
            task_id="t1",
            get_openlineage_facets_on_start=get_openlineage_facets_on_start,
        )
        listener = SimpleNamespace(config=SimpleNamespace(cluster="PROD"))

        OrreryListener._extract_lineage_from_airflow3(
            cast(Any, listener), cast(Any, task), mock.MagicMock(), complete=False
        )

        assert observed == [True]
        assert in_orrery_extraction() is False


class TestPatchInstallOrder:
    def test_guard_is_installed_before_operator_patches(self):
        """Operator patches capture their original by MRO lookup off the subclass.

        A guard installed after them is shadowed by the subclass entry they write, so
        the install order in _airflow_compat is load-bearing, not cosmetic.
        """
        import inspect

        from orrery_airflow_plugin.airflow3 import _airflow_compat

        source = inspect.getsource(_airflow_compat)

        def position(call: str) -> int:
            assert call in source, f"{call} not found in _airflow_compat"
            return source.index(call)

        guard_at = position("patch_sql_execute_query_operator()")
        operator_patches = [
            position(call)
            for call in (
                "patch_athena_operator()",
                "patch_bigquery_insert_job_operator()",
                "patch_teradata_operator()",
            )
        ]

        assert guard_at < min(operator_patches)


class TestPassCountBaseline:
    """The reported metric: enabling Orrery must not multiply warehouse passes.

    Simulates one task run with both listeners active. Airflow's provider calls the
    facet methods from its fork (outside the scope); the Orrery listener calls them
    in-process (inside it). ``_original_sql_parser_method`` is the only thing that
    issues ``information_schema`` queries, and
    ``get_openlineage_database_specific_lineage`` is the only thing that emits
    provider-side query events.
    """

    def test_orrery_adds_no_warehouse_passes(self, sql_parser_env):
        on_complete = (
            _installed_guard_operator_class().get_openlineage_facets_on_complete
        )

        hook = mock.MagicMock()
        hook.get_openlineage_database_specific_lineage.return_value = None
        operator: Any = SimpleNamespace(
            get_openlineage_facets_on_start=_call_patched_parser,
            get_db_hook=mock.MagicMock(return_value=hook),
        )

        # Provider's forked passes, then Orrery's in-process passes.
        operator.get_openlineage_facets_on_start()
        with orrery_extraction_scope():
            operator.get_openlineage_facets_on_start()
        on_complete(operator, mock.MagicMock())
        with orrery_extraction_scope():
            on_complete(operator, mock.MagicMock())

        # Two passes, not four: exactly what the provider alone would have done.
        assert sql_parser_env.call_count == 2
        assert all(c.kwargs["use_connection"] for c in sql_parser_env.call_args_list)
        hook.get_openlineage_database_specific_lineage.assert_called_once()


class TestTableErrorIsVisible:
    def test_table_error_is_recorded_on_datajob(self, fake_parse_result):
        """A table error drops all table lineage, so it must not be log-only.

        With the provider enabled, Orrery's parser is the only table source during
        Orrery extraction, making this the most likely way to lose lineage.
        """
        from orrery_airflow_plugin.airflow3.orrery_listener import OrreryListener

        fake_parse_result.debug_info.table_error = ValueError("cannot resolve table")
        datajob = SimpleNamespace(urn="urn:li:dataJob:test", properties={})

        inputs, outputs, _ = OrreryListener._process_sql_parsing_result(
            cast(Any, SimpleNamespace()), cast(Any, datajob), fake_parse_result
        )

        assert inputs == [] and outputs == []
        assert (
            "cannot resolve table"
            in datajob.properties["orrery_sql_parser_table_error"]
        )
