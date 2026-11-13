import types

# Imported as a module, not `from typing import ...`, so the _vars sweep below
# (which collects every non-underscore, non-module name) leaves it alone.
import typing

import orrery.metadata.schema_classes as models
from orrery.errors import SdkUsageError
from orrery.ingestion.graph.config import OrreryClientConfig
from orrery.ingestion.graph.filters import FilterOperator
from orrery.metadata.urns import (
    ChartUrn,
    ContainerUrn,
    CorpGroupUrn,
    CorpUserUrn,
    DashboardUrn,
    DataFlowUrn,
    DataJobUrn,
    DataPlatformInstanceUrn,
    DataPlatformUrn,
    DatasetUrn,
    DocumentUrn,
    DomainUrn,
    GlossaryNodeUrn,
    GlossaryTermUrn,
    MetricUrn,
    MlModelGroupUrn,
    MlModelUrn,
    SchemaFieldUrn,
    SemanticModelUrn,
    TagUrn,
)
from orrery.sdk._semantic_shared import (
    AiContextInput,
    DialectExpressionInput,
    MetricExpressionInputType,
    require_metrics_support,
)
from orrery.sdk.chart import Chart
from orrery.sdk.container import Container
from orrery.sdk.dashboard import Dashboard
from orrery.sdk.dataflow import DataFlow
from orrery.sdk.datajob import DataJob
from orrery.sdk.dataset import Dataset
from orrery.sdk.document import Document
from orrery.sdk.glossary_node import GlossaryNode
from orrery.sdk.glossary_term import GlossaryTerm
from orrery.sdk.main_client import OrreryClient
from orrery.sdk.metric import Metric
from orrery.sdk.mlmodel import MLModel
from orrery.sdk.mlmodelgroup import MLModelGroup
from orrery.sdk.search_filters import Filter, FilterDsl
from orrery.sdk.semantic_model import (
    SemanticFieldInput,
    SemanticModel,
    SemanticModelDataset,
    SemanticModelRelationshipInput,
)
from orrery.sdk.tag import Tag

# We want to print out the warning if people do `from orrery.sdk import X`.
# But we don't want to print out warnings if they're doing a more direct
# import like `from orrery.sdk.container import Container`, since that's
# what our internal code does.
_vars = {}
for _name, _value in list(locals().items()):
    if not _name.startswith("_") and (
        _name == "models" or not isinstance(_value, types.ModuleType)
    ):
        _vars[_name] = _value
        del locals()[_name]


# Hidden from type checkers on purpose. mypy honors a module-level __getattr__ in .py
# files, so leaving it visible makes every unknown name resolve to Any -- which is how
# `from orrery.sdk import DataFlowUrn` shipped broken. With it hidden, mypy checks
# against the static imports above, which are exactly what _vars holds at runtime.
if not typing.TYPE_CHECKING:

    def __getattr__(name: str) -> typing.Any:
        import warnings

        from orrery.errors import ExperimentalWarning

        warnings.warn(
            "The new orrery SDK (e.g. orrery.sdk.*) is experimental. "
            "Our typical backwards-compatibility and stability guarantees do not apply to this code. "
            "When it's promoted to stable, the import path will change "
            "from `from orrery.sdk import ...` to `from orrery import ...`.",
            ExperimentalWarning,
            stacklevel=2,
        )
        return _vars[name]
