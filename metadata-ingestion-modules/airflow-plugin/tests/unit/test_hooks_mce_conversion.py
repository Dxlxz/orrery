import warnings
from typing import Iterable, List, Union
from unittest import mock

import pytest

from orrery.emitter.mcp import MetadataChangeProposalWrapper
from orrery.errors import OrreryDeprecationWarning
from orrery.metadata.schema_classes import (
    DatasetPropertiesClass,
    DatasetSnapshotClass,
    MetadataChangeEventClass,
    StatusClass,
    UpstreamClass,
    UpstreamLineageClass,
)
from orrery_airflow_plugin.hooks.orrery import (
    OrreryCompositeHook,
    OrreryKafkaHook,
    OrreryRestHook,
    SynchronizedFileHook,
)

URN = "urn:li:dataset:(urn:li:dataPlatform:hive,db.table,PROD)"
MCE = MetadataChangeEventClass(
    proposedSnapshot=DatasetSnapshotClass(
        urn=URN,
        aspects=[
            StatusClass(removed=False),
            DatasetPropertiesClass(description="d"),
            UpstreamLineageClass(
                upstreams=[
                    UpstreamClass(
                        dataset="urn:li:dataset:(urn:li:dataPlatform:hive,db.up,PROD)",
                        type="TRANSFORMED",
                    )
                ]
            ),
        ],
    )
)
MCPW = MetadataChangeProposalWrapper(entityUrn=URN, aspect=StatusClass(removed=True))

Hook = Union[OrreryRestHook, OrreryKafkaHook, SynchronizedFileHook, OrreryCompositeHook]

HOOKS: List[Hook] = [
    OrreryRestHook(),
    OrreryKafkaHook(),
    SynchronizedFileHook(),
    OrreryCompositeHook(["orrery_rest_default"]),
]


def _emitted(
    hook: Hook,
    items: Iterable[Union[MetadataChangeEventClass, MetadataChangeProposalWrapper]],
) -> List[MetadataChangeProposalWrapper]:
    with mock.patch.object(type(hook), "make_emitter") as make_emitter:
        hook.emit(items)
    return [c.args[0] for c in make_emitter.return_value.emit.call_args_list]


@pytest.mark.parametrize("hook", HOOKS, ids=lambda h: type(h).__name__)
def test_hook_sends_mce_as_one_mcp_per_aspect(hook: Hook) -> None:
    with pytest.warns(OrreryDeprecationWarning, match="MetadataChangeEvent") as record:
        emitted = _emitted(hook, [MCE, MCPW, MCE])

    assert sum(issubclass(w.category, OrreryDeprecationWarning) for w in record) == 1
    assert all(isinstance(e, MetadataChangeProposalWrapper) for e in emitted)
    assert [(e.entityUrn, e.aspect) for e in emitted] == [
        *((URN, a) for a in MCE.proposedSnapshot.aspects),
        (URN, MCPW.aspect),
        *((URN, a) for a in MCE.proposedSnapshot.aspects),
    ]


@pytest.mark.parametrize("hook", HOOKS, ids=lambda h: type(h).__name__)
def test_hook_does_not_warn_on_mcps(hook: Hook) -> None:
    with warnings.catch_warnings():
        warnings.simplefilter("error", DeprecationWarning)
        assert _emitted(hook, [MCPW]) == [MCPW]


def test_hook_accepts_a_generator() -> None:
    items: List[Union[MetadataChangeEventClass, MetadataChangeProposalWrapper]] = [
        MCPW,
        MCE,
        MCPW,
    ]
    with pytest.warns(OrreryDeprecationWarning):
        emitted = _emitted(OrreryRestHook(), (item for item in items))

    assert len(emitted) == 2 + len(MCE.proposedSnapshot.aspects)


def test_hook_warns_on_mce_without_aspects(caplog: pytest.LogCaptureFixture) -> None:
    empty = MetadataChangeEventClass(
        proposedSnapshot=DatasetSnapshotClass(urn=URN, aspects=[])
    )
    with pytest.warns(OrreryDeprecationWarning):
        assert _emitted(OrreryRestHook(), [empty]) == []

    assert "produced zero MCPs" in caplog.text
